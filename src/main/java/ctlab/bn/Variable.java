package ctlab.bn;

import org.apache.commons.math3.distribution.BinomialDistribution;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Variable {
    private String name;

    private List<Double> data;
    private double[] u_x;
    private int[] uniq;
    private int[] ordered_obs;
    private double[] log_precomputed;
    private int[] discrete;
    private List<Double> edges;
    private LogFactorial lf;
    private int default_disc_classes;
    private double[][] priors;
    private Random random;

    private int lb;
    private int ub;

    public void set_disc_limits(int lb, int ub) {
        this.lb = lb;
        this.ub = ub;
    }

    public Variable(String name, List<Double> data, int disc_classes, DiscretizationPrior prior) {
        random = ThreadLocalRandom.current();
        this.name = name;
        this.data = new ArrayList<>(data);
        lf = new LogFactorial();

        ordered_obs = IntStream.range(0, data.size()).toArray();

        Comparator<Integer> cmp = Comparator.comparingDouble(data::get);

        ordered_obs = Arrays.stream(ordered_obs)
                .boxed()
                .sorted(cmp)
                .mapToInt(Integer::intValue)
                .toArray();

        List<Integer> uniq = new ArrayList<>();
        if (data.size() > 0) {
            double last = data.get(ordered_obs[0]);
            for (int i = 1; i < ordered_obs.length; i++) {
                double curr = data.get(ordered_obs[i]);
                if (curr != last) {
                    uniq.add(i - 1);
                    last = curr;
                }
            }
        }
        uniq.add(data.size() - 1);
        this.uniq = uniq.stream().mapToInt(Integer::intValue).toArray();
        u_x = new double[uniq.size()];
        for (int i = 0; i < uniq.size(); i++) {
            u_x[i] = data.get(get_uniq(i));
        }

        log_precomputed = IntStream.range(0, data.size() * 2 + 1)
                .mapToDouble(Math::log)
                .toArray();

        this.default_disc_classes = disc_classes;
        initPriors(prior);
        initial(disc_classes);
        lb = 1;
        ub = obsNum();
    }

    private void initPriors(DiscretizationPrior prior) {
        int n = uniq.length;
        priors = new double[n][n];
        BiFunction<Integer, Integer, Double> func = (u, v) -> 0.0;
        switch (prior) {
            case MULTINOMIAL:
                double p = 1.0 / default_disc_classes;
                double[] values = new double[n];
                for (int k = 1; k < n + 1; k++) {
                    values[k - 1] = lf.value(k) + lf.value(n - k) -lf.value(n);
                    values[k - 1] -= k * Math.log(p) + (n - k) * Math.log(1 - p);
                }
                func = (u, v) -> values[v - u + 1];
                break;
            case EXP:
                List<Double> W = new ArrayList<>();
                double rng = get_u(uniq.length - 1) - get_u(0);
                for (int i = 0; i < uniq.length - 1; i++) {
                    double val = 1 - Math.exp(-default_disc_classes * ((get_u(i + 1) - get_u(i)) / rng));
                    W.add(-Math.log(val));
                }
                W.add(0.0);
                func = (u, v) -> W.get(v) + default_disc_classes * ((get_u(v) - get_u(u)) / rng);
                break;
            case UNIFORM:
                func = (u, v) -> 0.0;
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                priors[i][j] = func.apply(i, j);
            }
        }
    }

    public Variable(Variable v) {
        data = new ArrayList<>(v.data);
        u_x = Arrays.copyOf(v.u_x, v.u_x.length);
        uniq = Arrays.copyOf(v.uniq, v.uniq.length);
        ordered_obs = Arrays.copyOf(v.ordered_obs, v.ordered_obs.length);
        log_precomputed = v.log_precomputed;
        discrete = Arrays.copyOf(v.discrete, v.discrete.length);
        edges = new ArrayList<>(v.edges);
        lf = new LogFactorial();
        this.name = v.name;
        default_disc_classes = v.default_disc_classes;
        priors = v.priors.clone();
        this.lb = v.lb;
        this.ub = v.ub;
        this.priors = v.priors;
        random = ThreadLocalRandom.current();
    }

    void setLF(LogFactorial lf) {
        this.lf = lf;
    }

    private double get_disc_edge(int u) {
        double disc_edge;
        if (u + 1 < uniq.length) {
            disc_edge = (get_u(u) + get_u(u + 1)) / 2.0;
        } else {
            disc_edge = Double.POSITIVE_INFINITY;
        }
        return disc_edge;
    }

    void discretize(List<Variable> parents, List<Variable> children, List<List<Variable>> spouse_sets) {
        for (int i = 0; i < data.size(); i++) {
            discrete[i] = 0;
        }

        double[][] h = compute_hs(parents, children, spouse_sets);

        double[] S = new double[uniq.length];
        List<List<Double>> lambda = new ArrayList<>();

        for (int i = 0; i < uniq.length; i++) {
            lambda.add(new ArrayList<>());
        }

        for (int v = lb - 1; v < uniq.length; v++) {
            List<Double> lambda_v = lambda.get(v);
            double s_hat = Double.POSITIVE_INFINITY;
            int u_hat = 0;
            double disc_edge = Double.POSITIVE_INFINITY;
            if (v < ub) {
                s_hat = priors[0][v];
                s_hat += h[0][uniq[v]];
                u_hat = v;
                disc_edge = get_disc_edge(v);
            }
            for (int u = Math.max(v - ub, lb - 1); u <= v - lb; u++) {
                double s_tilde = S[u] + priors[u + 1][v];
                s_tilde += h[uniq[u] + 1][uniq[v] - uniq[u] - 1];
                if (s_tilde < s_hat) {
                    s_hat = s_tilde;
                    u_hat = u;
                    disc_edge = get_disc_edge(u);
                }
            }
            S[v] = s_hat;
            lambda_v.addAll(lambda.get(u_hat));
            if (disc_edge < Double.POSITIVE_INFINITY) {
                if (lambda_v.isEmpty() || lambda_v.get(lambda_v.size() - 1) != disc_edge) {
                    lambda_v.add(disc_edge);
                }
            }
        }

        edges = new ArrayList<>(lambda.get(uniq.length - 1));

        write_discretization();
    }

    private void write_discretization() {
        discrete = data.stream().mapToInt(x -> -Collections.binarySearch(edges, x)).toArray();
    }

    public Collection<Integer> cardinalities() {
        Map<Integer, Integer> cs = new TreeMap<>();
        for (int d: discrete) {
            cs.putIfAbsent(d, 0);
            cs.put(d, cs.get(d) + 1);
        }
        return cs.values();
    }

    private int get_uniq(int i) {
        return ordered_obs[uniq[i]];
    }

    private double get_u(int i) {
        return u_x[i];
    }

    private double[][] compute_hs(List<Variable> ps, List<Variable> cs, List<List<Variable>> ss) {
        double[][] result = parents_term(ps);
        child_spouse_term(cs, ss, result);
        return result;
    }

    private void child_spouse_term(List<Variable> cs, List<List<Variable>> ss, double[][] result) {
        for (int i = 0; i < cs.size(); i++) {
            one_child_spouse_term(cs.get(i), ss.get(i), result);
        }
    }

    private int number_of_classes(int[] obs) {
        return Arrays.stream(obs)
                .max().getAsInt() + 1;
    }

    private void one_child_spouse_term(Variable child, List<Variable> ss, double[][] result) {
        int n = result.length;
        int card = child.cardinality() - 1;
        List<Variable> spouse_and_child = new ArrayList<>(ss);
        spouse_and_child.add(child);

        int[] spouse_child_mapping = map_obs(spouse_and_child);
        int[] spouse_mapping = map_obs(ss);

        int num_sc_classes = number_of_classes(spouse_child_mapping);
        int num_spouse_classes = number_of_classes(spouse_mapping);

        int[] sc_count = new int[num_sc_classes];
        int[] s_count = new int[num_spouse_classes];

        for (int i = 0; i < n; i++) {
            Arrays.fill(sc_count, 0);
            Arrays.fill(s_count, 0);

            double value = 0.0;
            for (int j = 0; j < n - i; j++) {
                int v = i + j;
                int sc_cl = spouse_child_mapping[v];
                int s_cl = spouse_mapping[v];

                int curr_sc_count = ++sc_count[sc_cl];
                int curr_s_count = ++s_count[s_cl];

                value += log_precomputed[curr_s_count + card] - log_precomputed[curr_sc_count];

                result[i][j] += value;
            }
        }
    }

    public int[] map_obs(List<Variable> ps) {
        int m = obsNum();
        int[] result = new int[m];
        Variable[] vs = ps.toArray(new Variable[0]);
        int ps_size = ps.size();
        int[] cds = new int[ps_size + 1];
        for (int i = 0; i < ps_size; i++) {
            cds[i] = ps.get(i).cardinality();
        }
        cds[ps_size] = 1;

        Trie t = new Trie(cds);

        Trie.Selector selector = t.selector();
        for (int i = 0; i < m; i++) {
            selector.reuse();
            for (Variable p: vs) {
                selector.choose(p.discrete_value(ordered_obs[i]) - 1);
            }

            result[i] = selector.get();
        }
        return result;
    }

    private double[][] parents_term(List<Variable> ps) {
        int n = obsNum();
        double[][] result = new double[n][n];
        double[] combinations = new double[n];

        int[] mapped_obs = map_obs(ps);
        int num_classes = Arrays.stream(mapped_obs)
                .max()
                .getAsInt() + 1;

        int k = num_classes - 1;
        combinations[0] = Math.log(k + 1);
        for (int i = 1; i < n; i++) {
            combinations[i] = combinations[i - 1] + Math.log(k + i + 1) - log_precomputed[i + 1];
        }

        for (int u = 0; u < n; u++) {
            System.arraycopy(combinations, 0, result[u], 0, n - u);
        }

        int[] hits = new int[num_classes];
        double[] local_lf = new double[n];
        for (int i = 0; i < n; i++) {
            local_lf[i] = lf.value(i + 1);
        }
        for (int i = 0; i < n; i++) {
            Arrays.fill(hits, 0);
            double value = 0.0;
            for (int j = 0; j < n - i; j++) {
                int v = i + j;
                int cl = mapped_obs[v];
                value -= log_precomputed[++hits[cl]];
                result[i][j] += value + local_lf[j];
            }
        }

        return result;
    }

    public String getName() {
        return name;
    }

    public int obsNum() {
        return data.size();
    }

    void initial(int num_classes) {
        int num_edges = num_classes - 1;
        if (num_classes > uniq.length) {
            throw new IllegalArgumentException("Too many classes");
        }
        edges = new ArrayList<>();
        for (int i = 0; i < num_edges; i++) {
            int pos = (int)Math.round(Math.floor(uniq.length * ((double)i + 1) / num_classes));
            edges.add((data.get(get_uniq(pos)) + data.get(get_uniq(pos + 1))) / 2);
        }
        write_discretization();
    }

    void initial() {
        initial(default_disc_classes);
    }

    public int cardinality() {
        return edges.size() + 1;
    }

    public int discrete_value(int obs) {
        return discrete[obs];
    }

    boolean equals(Variable v) {
        return v != null && name.equals(v.name);
    }

    public List<Double> discretization_edges() {
        return Collections.unmodifiableList(edges);
    }

    public void random_policy() {
        double[] edges = IntStream.range(0, uniq.length - 1).mapToDouble(this::get_disc_edge).toArray();
        int k = random.nextInt(ub - lb + 1) + lb - 1;
        int[] buckets = new int[k];
        for (int i = 0; i < uniq.length; i++) {
            buckets[random.nextInt(k)]++;
        }
        this.edges.clear();
        int curr = 0;
        for (int i = 0; i < k - 1; i++) {
            curr += buckets[i];
            if (curr == uniq.length) {
                break;
            }
            if (buckets[i] == 0) {
                continue;
            }
            this.edges.add(edges[curr]);
        }
        write_discretization();
    }

    public enum DiscretizationPrior {
        UNIFORM, EXP, MULTINOMIAL
    }
}

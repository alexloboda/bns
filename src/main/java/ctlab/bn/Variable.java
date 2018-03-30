package ctlab.bn;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Variable {
    private String name;

    private List<Double> data;
    private double[] u_x;
    private int[] uniq;
    private int[] ordered_obs;
    private double[] log_precomputed;
    private List<Integer> discrete;
    private List<Double> edges;
    private LogFactorial lf;

    public Variable(String name, List<Double> data, int disc_classes) {
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

        initial(disc_classes);
    }

    void setLF(LogFactorial lf) {
        this.lf = lf;
    }

    void discretize(List<Variable> parents, List<Variable> children, List<List<Variable>> spouse_sets,
                    boolean at_least_one_edge) {
        for (int i = 0; i < data.size(); i++) {
            discrete.set(i, 0);
        }

        double[][] h = compute_hs(parents, children, spouse_sets);

        int l_card = max_card(parents, children, spouse_sets);
        double[] S = new double[uniq.length];
        double[] W = initW(l_card).stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        List<Set<Double>> lambda = new ArrayList<>();
        double whl_rng = get_u(uniq.length - 1) - get_u(0);

        for (int v = 0; v < uniq.length; v++) {
            lambda.add(new TreeSet<>());
            if (v == 0) {
                S[0] =  h[v][uniq[v] - v] + W[v];
                lambda.get(v).add((get_u(v) + get_u(v + 1)) / 2.0);
            } else {
                double s_hat = Double.POSITIVE_INFINITY;
                int u_hat = 0;
                double disc_edge = Double.POSITIVE_INFINITY;
                for (int u = 0; u <= v; u++) {
                    double s_tilde = W[v];
                    if (u == v) {
                        s_tilde += h[0][uniq[v]];
                        s_tilde += l_card * ((get_u(v) - get_u(0)) / whl_rng);
                    } else {
                        s_tilde += h[uniq[u] + 1][uniq[v] - uniq[u] - 1];
                        s_tilde += l_card * ((get_u(v) - get_u(u + 1)) / whl_rng);
                        s_tilde += S[u];
                    }
                    if (s_tilde < s_hat && !(at_least_one_edge && v == u && v == uniq.length - 1)) {
                        s_hat = s_tilde;
                        u_hat = u;
                        if (u + 1 < uniq.length) {
                            disc_edge = (get_u(u) + get_u(u + 1)) / 2.0;
                        } else {
                            disc_edge = Double.POSITIVE_INFINITY;
                        }
                    }
                }
                S[v] = s_hat;
                lambda.get(v).addAll(lambda.get(u_hat));
                if (disc_edge < Double.POSITIVE_INFINITY) {
                    lambda.get(v).add(disc_edge);
                }
            }
        }

        edges = new ArrayList<>(lambda.get(uniq.length - 1));
        write_discretization();
    }

    private void write_discretization() {
        discrete = data.stream().map(x -> -Collections.binarySearch(edges, x)).collect(Collectors.toList());
    }

    private List<Double> initW(int max_card) {
        List<Double> W = new ArrayList<>();
        double rng = get_u(uniq.length - 1) - get_u(0);
        for (int i = 0; i < uniq.length - 1; i++) {
            double val = 1 - Math.exp(-max_card * ((get_u(i + 1) - get_u(i)) / rng));
            W.add(-Math.log(val));
        }
        W.add(0.0);
        return W;
    }

    private int get_uniq(int i) {
        return ordered_obs[uniq[i]];
    }

    private double get_u(int i) {
        return u_x[i];
    }

    private int max_card(List<Variable> parents, List<Variable> children, List<List<Variable>> spouse_sets) {
        int max_card = 0;
        for (Variable v: parents) {
            max_card = Math.max(max_card, v.cardinality());
        }

        for (Variable v: children) {
            max_card = Math.max(max_card, v.cardinality());
        }

        for (List<Variable> set: spouse_sets) {
            for (Variable v: set) {
                max_card = Math.max(max_card, v.cardinality());
            }
        }
        return max_card;
    }

    private double[][] compute_hs(List<Variable> ps, List<Variable> cs, List<List<Variable>> ss) {
        double[][] result = get_first_term(ps);
        parents_term(ps, result);
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

    int[] map_obs(List<Variable> ps) {
        Map<List<Integer>, Integer> map = new HashMap<>();
        int m = obsNum();
        int[] result = new int[m];
        Variable[] vs = ps.toArray(new Variable[0]);

        for (int i = 0; i < m; i++) {
            List<Integer> disc_ps = new ArrayList<>();
            for (Variable p: vs) {
                disc_ps.add(p.discrete_value(ordered_obs[i]));
            }

            if (!map.containsKey(disc_ps)) {
                map.put(disc_ps, map.size());
            }

            result[i] = map.get(disc_ps);
        }
        return result;
    }

    private void parents_term(List<Variable> ps, double[][] result) {
        int[] mapped_obs = map_obs(ps);
        int num_classes = Arrays.stream(mapped_obs)
                .max()
                .getAsInt() + 1;

        int n = result.length;
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
                value -= Math.log(++hits[cl]);
                result[i][j] += value + local_lf[j];
            }
        }
    }

    private double[][] get_first_term(List<Variable> ps) {
        int n = obsNum();
        double[][] result = new double[n][n];
        int parent_classes = 1;
        for (Variable p : ps) {
            parent_classes *= p.cardinality();
        }

        double[] combinations = new double[n];
        for (int i = 0; i < n; i++) {
            combinations[i] = log_combinations(i + 1 + parent_classes - 1, parent_classes - 1);
        }

        for (int u = 0; u < n; u++) {
            System.arraycopy(combinations, 0, result[u], 0, n - u);
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

    int cardinality() {
        return edges.size() + 1;
    }

    public int discrete_value(int obs) {
        return discrete.get(obs);
    }

    private double log_combinations(int n, int k) {
        return lf.value(n) - lf.value(k) - lf.value(n - k);
    }

    boolean equals(Variable v) {
        return v != null && name.equals(v.name);
    }

    List<Double> discretization_edges() {
        return Collections.unmodifiableList(edges);
    }

}
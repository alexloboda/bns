import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Variable {
    private String name;
    private int default_num_classes;

    private List<Double> data;
    private List<Integer> uniq;
    private List<Integer> ordered_obs;
    private List<Integer> discrete;
    private List<Double> edges;
    private LogFactorial lf;

    Variable(String name, List<Double> data, int disc_classes) {
        this.name = name;
        this.data = new ArrayList<>(data);
        lf = new LogFactorial();
        default_num_classes = disc_classes;

        ordered_obs = IntStream.range(0, data.size()).boxed().collect(Collectors.toList());

        Comparator<Integer> cmp = Comparator.comparingDouble(data::get);

        Collections.sort(ordered_obs, cmp);

        uniq = new ArrayList<>();
        if (data.size() > 0) {
            double last = data.get(ordered_obs.get(0));
            for (int i = 1; i < ordered_obs.size(); i++) {
                double curr = data.get(ordered_obs.get(i));
                if (curr != last) {
                    uniq.add(i - 1);
                    last = curr;
                }
            }
        }
        uniq.add(data.size() - 1);

        initial(disc_classes);
    }

    public void discretize(List<Variable> parents, List<Variable> children, List<List<Variable>> spouse_sets) {
        for (int i = 0; i < data.size(); i++) {
            discrete.set(i, 0);
        }

        List<List<Double>> h = compute_hs(parents, children, spouse_sets);

        int l_card = max_card(parents, children, spouse_sets);
        List<Double> S = new ArrayList<>();
        List<Double> W = initW(l_card);
        List<Set<Double>> lambda = new ArrayList<>();
        double whl_rng = get_u(uniq.size() - 1) - get_u(0);

        for (int v = 0; v < uniq.size(); v++) {
            lambda.add(new TreeSet<>());
            if (v == 0) {
                S.add(v, h.get(v).get(uniq.get(v) - v) + W.get(v));
                lambda.get(v).add((get_u(v) + get_u(v + 1)) / 2.0);
            } else {
                double s_hat = Double.POSITIVE_INFINITY;
                int u_hat = 0;
                double disc_edge = Double.POSITIVE_INFINITY;
                for (int u = 0; u <= v; u++) {
                    double s_tilde = W.get(v);
                    if (u == v) {
                        s_tilde += h.get(0).get(uniq.get(v));
                        s_tilde += l_card * ((get_u(v) - get_u(0)) / whl_rng);
                    } else {
                        s_tilde += h.get(uniq.get(u) + 1).get(uniq.get(v) - uniq.get(u) - 1);
                        s_tilde += l_card * ((get_u(v) - get_u(u + 1)) / whl_rng);
                        s_tilde += S.get(u);
                    }
                    if (s_tilde < s_hat) {
                        s_hat = s_tilde;
                        u_hat = u;
                        if (u + 1 < uniq.size()) {
                            disc_edge = (get_u(u) + get_u(u + 1)) / 2.0;
                        }
                    }
                }
                S.add(s_hat);
                lambda.get(v).addAll(lambda.get(u_hat));
                if (disc_edge < Double.POSITIVE_INFINITY) {
                    lambda.get(v).add(disc_edge);
                }
            }
        }

        edges = new ArrayList<>(lambda.get(uniq.size() - 1));
        if (edges.isEmpty()) {
            initial(default_num_classes);
        } else {
            write_discretization();
        }
    }

    private void write_discretization() {
        discrete = data.stream().map(x -> -Collections.binarySearch(edges, x)).collect(Collectors.toList());
    }

    private List<Double> initW(int max_card) {
        List<Double> W = new ArrayList<>();
        double rng = get_u(uniq.size() - 1) - get_u(0);
        for (int i = 0; i < uniq.size() - 1; i++) {
            double val = 1 - Math.exp(-max_card * ((get_u(i + 1) - get_u(i)) / rng));
            W.add(-Math.log(val));
        }
        W.add(0.0);
        return W;
    }

    private int get_uniq(int i) {
        return ordered_obs.get(uniq.get(i));
    }

    private double get_u(int i) {
        return data.get(get_uniq(i));
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

    private List<List<Double>> compute_hs(List<Variable> ps, List<Variable> cs, List<List<Variable>> ss) {
        List<List<Double>> result = get_first_term(ps);
        parents_term(ps, result);
        child_spouse_term(cs, ss, result);
        return result;
    }

    private void child_spouse_term(List<Variable> cs, List<List<Variable>> ss, List<List<Double>> result) {
        for (int i = 0; i < cs.size(); i++) {
            one_child_spouse_term(cs.get(i), ss.get(i), result);
        }
    }

    private void one_child_spouse_term(Variable child, List<Variable> ss, List<List<Double>> result) {
        int n = result.size();
        List<Variable> spouse_and_child = new ArrayList<>(ss);
        spouse_and_child.add(child);
        List<Integer> spouse_child_mapping = map_obs(spouse_and_child);
        List<Integer> spouse_mapping = map_obs(ss);
        int num_sc_classes = Collections.max(spouse_child_mapping) + 1;
        int num_spouse_classes = Collections.max(spouse_mapping) + 1;
        List<Integer> sc_count = new ArrayList<>(Collections.nCopies(num_sc_classes, 0));
        List<Integer> s_count = new ArrayList<>(Collections.nCopies(num_spouse_classes, 0));

        for (int i = 0; i < n; i++) {
            double value = 0.0;
            for (int j = 0; j < n - i; j++) {
                int v = i + j;
                int sc_cl = spouse_child_mapping.get(ordered_obs.get(v));
                int s_cl = spouse_mapping.get(ordered_obs.get(j));

                int curr_sc_count = sc_count.get(sc_cl);
                int curr_s_count = s_count.get(s_cl);

                sc_count.set(sc_cl, curr_sc_count + 1);
                s_count.set(s_cl, curr_s_count + 1);

                // last term of h(v, u)
                value += lf.value(curr_s_count + 1) - lf.value(curr_s_count);
                value -= lf.value(curr_sc_count + 1) - lf.value(curr_sc_count);

                value -= log_combinations(curr_s_count + child.cardinality() - 1, child.cardinality() - 1);
                value += log_combinations(curr_s_count + 1 + child.cardinality() - 1, child.cardinality() - 1);

                result.get(i).set(j, result.get(i).get(j) + value);
            }
        }
    }

    private List<Integer> map_obs(List<Variable> ps) {
        Map<List<Integer>, Integer> map = new HashMap<>();
        List<Integer> result = new ArrayList<>();
        int m = obsNum();

        for (int i = 0; i < m; i++) {
            List<Integer> disc_ps = new ArrayList<>();
            for (Variable p: ps) {
                disc_ps.add(p.discrete_value(i));
            }

            if (!map.containsKey(disc_ps)) {
                map.put(disc_ps, map.size());
            }

            result.add(map.get(disc_ps));
        }
        return result;
    }

    private void parents_term(List<Variable> ps, List<List<Double>> result) {
        List<Integer> mapped_obs = map_obs(ps);
        int num_classes = Collections.max(mapped_obs) + 1;
        int n = result.size();
        for (int i = 0; i < n; i++) {
            List<Integer> hits = new ArrayList<>(Collections.nCopies(num_classes, 0));
            double value = 0.0;
            for (int j = 0; j < n - i; j++) {
                int v = i + j;
                int cl = mapped_obs.get(ordered_obs.get(v));
                int curr = hits.get(cl);
                value += lf.value(curr);
                value -= lf.value(curr + 1);
                hits.set(cl, curr + 1);
                result.get(i).set(j, result.get(i).get(j) + value + lf.value(j + 1));
            }
        }
    }

    private List<List<Double>> get_first_term(List<Variable> ps) {
        List<List<Double>> result = new ArrayList<>();
        int n = obsNum();
        int parent_classes = 1;
        for (Variable p : ps) {
            parent_classes *= p.cardinality();
        }
        for (int u = 0; u < n; u++) {
            result.add(new ArrayList<>());
            for(int j = 0; j < n - u; j++) {
                double value = log_combinations(j + 1 + parent_classes - 1, parent_classes - 1);
                result.get(u).add(value);
            }
        }
        return result;
    }

    String getName() {
        return name;
    }

    int obsNum() {
        return data.size();
    }

    private void initial(int num_classes) {
        int num_edges = num_classes - 1;
        if (num_classes > uniq.size()) {
            throw new IllegalArgumentException("Too many classes");
        }
        edges = new ArrayList<>();
        for (int i = 0; i < num_edges; i++) {
            int pos = (int)Math.round(Math.floor(uniq.size() * ((double)i + 1) / num_classes));
            edges.add((data.get(get_uniq(pos)) + data.get(get_uniq(pos + 1))) / 2);
        }
        write_discretization();
    }

    private int cardinality() {
        return edges.size() + 1;
    }

    int discrete_value(int obs) {
        return discrete.get(obs);
    }

    private double log_combinations(int n, int k) {
        return lf.value(n) - lf.value(k) - lf.value(n - k);
    }

    public boolean equals(Variable v) {
        return v != null && name.equals(v.name);
    }

    List<Double> discretization_edges() {
        return Collections.unmodifiableList(edges);
    }
}

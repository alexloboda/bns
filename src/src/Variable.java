import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Variable {
    private String name;

    private List<Double> data;
    private List<Integer> uniq;
    private List<Integer> ordered_obs;
    private List<Integer> discrete;
    private List<Double> edges;

    public Variable(String name, List<Double> data, int disc_points) {
        this.name = name;
        this.data = new ArrayList<>(data);
        ordered_obs = IntStream.range(0, data.size()).boxed().collect(Collectors.toList());

        Comparator<Integer> cmp = Comparator.comparingDouble(data::get);

        Collections.sort(ordered_obs, cmp);

        uniq = new ArrayList<>();
        if (data.size() > 0) {
            double last = uniq.get(ordered_obs.get(0));
            for (int i = 1; i < ordered_obs.size(); i++) {
                double curr = data.get(ordered_obs.get(i));
                if (curr != last) {
                    uniq.add(i - 1);
                    last = curr;
                }
            }
        }
    }

    public void discretize(List<Variable> parents, List<Variable> children, List<List<Variable>> spouse_sets) {
        for (int i = 0; i < data.size(); i++) {
            discrete.set(i, 0);
        }
        List<List<Double>> h = compute_hs(parents, children, spouse_sets);
        int l_card = max_card(parents, children, spouse_sets);
        List<Double> S = new ArrayList<>();
        List<Double> W = initW(l_card);
        List<List<Double>> lambda = new ArrayList<>();
        double whl_rng = data.get(ordered_obs.get(data.size() - 1)) - data.get(ordered_obs.get(0));

        for (int v = 0; v < uniq.size(); v++) {
            S.add(0.0);
            if (v == 0) {
                S.set(v, h.get(v).get(uniq.get(v)) + W.get(v));
                lambda.add(Collections.singletonList((uniq.get(v) + uniq.get(v + 1)) / 2.0));
            } else {
                double s_hat = Double.POSITIVE_INFINITY;
                int u_hat = 0;
                double disc_edge = Double.POSITIVE_INFINITY;
                for (int u = 0; u <= v; u++) {
                    double s_tilde = W.get(v);
                    if (u == v) {
                        s_tilde += h.get(0).get(get_uniq(v));
                        s_tilde += l_card * ((data.get(get_uniq(v)) - data.get(get_uniq(0))) / whl_rng);
                    } else {
                        s_tilde += h.get(get_uniq(u) + 1).get(get_uniq(v));
                        s_tilde += l_card * ((data.get(get_uniq(v)) - data.get(get_uniq(u + 1))) / whl_rng);
                        s_tilde += S.get(u);
                    }
                    if (s_tilde < s_hat) {
                        s_hat = s_tilde;
                        u_hat = u;
                        disc_edge = (data.get(get_uniq(u)) + data.get(get_uniq(u + 1))) / 2.0;
                    }
                }
                S.add(s_hat);
                List<Double> lam = new ArrayList<>(lambda.get(u_hat));
                lam.add(disc_edge);
            }
        }

        this.edges = lambda.get(uniq.size() - 1);
        write_discretization();
    }

    private void write_discretization() {
        discrete = data.stream().map(x -> Collections.binarySearch(edges, x)).collect(Collectors.toList());
    }

    private List<Double> initW(int max_card) {
        List<Double> W = new ArrayList<>();
        double max = get_uniq(uniq.size() - 1);
        double min = get_uniq(0);
        for (int i = 0; i < uniq.size() - 1; i++) {
            double val = -max_card * ((get_uniq(i + 1) - get_uniq(i)) / (max - min));
            W.add(-Math.log(1 - val));
        }
        W.add(0.0);
        return W;
    }

    private int get_uniq(int i) {
        return ordered_obs.get(uniq.get(i));
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
        List<List<Double>> result = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            result.add(new ArrayList<>());
            for (int j = i; j < data.size(); j++) {
                double value = 0.0;
                List<Integer> samples = new ArrayList<>();
                for (int k = i; k <= j; k++) {
                    samples.add(ordered_obs.get(k));
                }
                value += compute_hs_parent(samples, this, ps);
                for (int k = 0; k < cs.size(); k++) {
                    value += compute_hs_child(samples, cs.get(k), ss.get(k));
                }
                result.get(i).add(value);
            }
        }
        return result;
    }

    private double compute_hs_parent(List<Integer> samples, Variable variable, List<Variable> ps) {
        double value = 0.0;
        Map<List<Integer>, Integer> insts = new HashMap<>();
        for (int s: samples) {
            List<Integer> inst = new ArrayList<>();
            for (Variable v: ps) {
                inst.add(v.discrete_value(s));
            }
            if (!insts.containsKey(inst)) {
                insts.put(inst, 0);
            }
            insts.put(inst, insts.get(inst) + 1);
        }

        value += log_combinations(samples.size() + insts.size() - 1, insts.size() - 1);
        value += log_factorial(samples.size());
        for (int inst_size: insts.values()) {
            value -= log_factorial(inst_size);
        }
        return value;
    }

    private double compute_hs_child(List<Integer> samples, Variable child, List<Variable> ps) {
        double value = 0.0;
        Map<List<Integer>, List<Integer>> insts = new HashMap<>();
        for (int s: samples) {
            List<Integer> inst = new ArrayList<>();
            for (Variable v: ps) {
                inst.add(v.discrete_value(s));
            }
            if (!insts.containsKey(inst)) {
                insts.put(inst, new ArrayList<>());
            }
            insts.get(inst).add(s);
        }

        for (List<Integer> inst: insts.keySet()) {
            Map<Integer, Integer> ch_insts = new HashMap<>();
            List<Integer> obs = insts.get(inst);
            for (int ob: obs) {
                int disc = child.discrete_value(ob);
                if (!ch_insts.containsKey(disc)) {
                    ch_insts.put(disc, 0);
                }
                ch_insts.put(disc, ch_insts.get(disc) + 1);
            }

            value += log_combinations(inst.size() + ch_insts.size() - 1, ch_insts.size() - 1);

            value += log_factorial(inst.size());
            for (int num: ch_insts.values()) {
                value -= log_factorial(num);
            }
        }
        return value;
    }

    private void initial(int num_classes) {
        int num_edges = num_classes - 1;
        if (num_classes > uniq.size()) {
            throw new IllegalArgumentException("Too many classes");
        }
        for (int i = 0; i < num_edges; i++) {
            int pos = uniq.size() / num_edges;
            edges.add((data.get(get_uniq(pos)) + data.get(get_uniq(pos + 1))) / 2);
        }
        write_discretization();
    }

    public int cardinality() {
        return edges.size() + 1;
    }

    public int discrete_value(int obs) {
        return discrete.get(obs);
    }

    private static int log_factorial(int n) {
        return ((1 + n) * n) / 2;
    }

    public static int log_combinations(int n, int k) {
        return log_factorial(n) - log_factorial(k) - log_factorial(n - k);
    }

    public boolean equals(Variable v) {
        return v != null && name.equals(v.name);
    }
}

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

        System.err.println("H?");
        List<List<Double>> h = compute_hs(parents, children, spouse_sets);
        System.err.println("H!");

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
        List<List<Double>> result = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            result.add(new ArrayList<>());
            for (int j = i; j < data.size(); j++) {
                double value = 0.0;
                List<Integer> samples = new ArrayList<>();
                for (int k = i; k <= j; k++) {
                    samples.add(ordered_obs.get(k));
                }
                value += compute_hs_parent(samples, ps);
                for (int k = 0; k < cs.size(); k++) {
                    value += compute_hs_child(samples, cs.get(k), ss.get(k));
                }
                result.get(i).add(value);
            }
        }
        return result;
    }

    private double compute_hs_parent(List<Integer> samples, List<Variable> ps) {
        double value = 0.0;
        Map<List<Integer>, Integer> insts = new HashMap<>();
        long class_number = 1;
        for (Variable v: ps) {
            class_number *= v.cardinality();
        }
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

        value += log_combinations(samples.size() + class_number - 1, class_number - 1);
        value += lf.value(samples.size());
        for (int inst_size: insts.values()) {
            value -= lf.value(inst_size);
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

            value += log_combinations(obs.size() + child.cardinality() - 1, child.cardinality() - 1);

            value += lf.value(obs.size());
            for (int num: ch_insts.values()) {
                value -= lf.value(num);
            }
        }
        return value;
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

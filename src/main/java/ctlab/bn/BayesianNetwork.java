package ctlab.bn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BayesianNetwork {
    private List<Variable> variables;
    private Graph g;
    private boolean keep_one;
    private boolean repair_initial;

    public BayesianNetwork(List<Variable> variables, boolean keep_one, boolean repair_initial) {
        g = new Graph(variables.size());
        this.variables = variables;
        LogFactorial lf = new LogFactorial();
        variables.forEach(v -> v.setLF(lf));
        this.keep_one = keep_one;
        this.repair_initial = repair_initial;
    }

    public BayesianNetwork(BayesianNetwork bn) {
        variables = new ArrayList<>();
        for (Variable v : bn.variables) {
            variables.add(new Variable(v));
        }
        g = new Graph(bn.g);
        keep_one = bn.keep_one;
        repair_initial = bn.repair_initial;
    }

    public void backup() {
        variables.forEach(Variable::backup);
    }

    public void restore() {
        variables.forEach(Variable::restore);
    }

    public void add_edge(int v, int u) {
        g.add_edge(v, u);
    }

    public void remove_edge(int v, int u) {
        g.remove_edge(v, u);
    }

    private List<Variable> parent_set(int v) {
        return g.ingoing_edges(v).stream()
                .map(x -> variables.get(x))
                .collect(Collectors.toList());
    }

    private void discretization_step() {
        List<Integer> order = g.top_sort();
        int max_card = 0;
        for (Variable v : variables) {
            max_card = Math.max(max_card, v.cardinality());
        }
        for (int v : order) {
            Variable var = variables.get(v);
            List<Variable> ps = parent_set(v);
            List<Variable> cs = g.outgoing_edges(v).stream()
                    .map(x -> variables.get(x))
                    .collect(Collectors.toList());
            List<List<Variable>> ss = g.outgoing_edges(v).stream()
                    .map(x -> g.ingoing_edges(x))
                    .map(x -> x.stream()
                            .map(y -> variables.get(y))
                            .filter(y -> !y.equals(var))
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
            var.discretize(ps, cs, ss, keep_one, max_card, repair_initial);
        }
    }

    private List<List<Double>> discretization_policy() {
        return variables.stream()
                .map(Variable::discretization_edges)
                .collect(Collectors.toList());
    }

    public int cardinality(int v) {
        return variables.get(v).cardinality();
    }

    public int observations() {
        return variables.stream().findAny().get().obsNum();
    }

    private double score(K2ScoringFunction sf, Variable v, List<Variable> ps) {

        int[] parent_cls = v.map_obs(ps);

        List<Variable> vs = new ArrayList<>(ps);
        vs.add(v);

        int[] all_cls = v.map_obs(vs);

        return sf.score(parent_cls, all_cls, v.cardinality());
    }

    public double score(K2ScoringFunction sf, PriorDistribution pd) {
        double log_score = pd.value(g);
        for (int i = 0; i < variables.size(); i++) {
            log_score += score(sf, variables.get(i), parent_set(i));
        }
        return log_score;
    }

    public int size() {
        return variables.size();
    }

    public boolean edge_exists(int v, int u) {
        return g.edge_exists(v, u);
    }

    public boolean path_exists(int v, int u) {
        return g.path_exists(v, u);
    }

    public List<Integer> ingoing_edges(int v) {
        return g.ingoing_edges(v);
    }

    public int discretize(int steps_ub) {
        List<List<Double>> disc_policy = discretization_policy();
        for (int i = 0; i < steps_ub; i++) {
            discretization_step();
            List<List<Double>> new_policy = discretization_policy();
            if (disc_policy.equals(new_policy)) {
                return i + 1;
            } else {
                disc_policy = new_policy;
            }
        }
        return steps_ub;
    }
}

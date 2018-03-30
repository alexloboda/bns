package ctlab.bn;

import ctlab.graph.Graph;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BayesianNetwork {
    private List<Variable> variables;
    private Graph g;

    public BayesianNetwork(List<Variable> variables) {
        g = new Graph(variables.size());
        this.variables = variables;
        LogFactorial lf = new LogFactorial();
        variables.forEach(v -> v.setLF(lf));
    }

    public void add_edge(int v, int u) {
        g.add_edge(v, u);
    }

    void remove_edge(int v, int u) {
        g.remove_edge(v, u);
    }

    private List<Variable> parent_set(int v) {
        return g.ingoing_edges(v).stream()
                .map(x -> variables.get(x))
                .collect(Collectors.toList());
    }

    private void discretization_step(boolean at_least_one_edge) {
        List<Integer> order = g.top_sort();
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
            var.discretize(ps, cs, ss, at_least_one_edge);
        }
    }

    private List<List<Double>> discretization_policy() {
        return variables.stream()
                .map(Variable::discretization_edges)
                .collect(Collectors.toList());
    }

    private double score(K2ScoringFunction sf, Variable v, List<Variable> ps) {

        int[] parent_cls = v.map_obs(ps);

        List<Variable> vs = new ArrayList<>(ps);
        vs.add(v);

        int[] all_cls = v.map_obs(vs);

        return sf.score(parent_cls, all_cls, v.cardinality());
    }

    public double score(K2ScoringFunction sf) {
        double log_score = 0.0;
        for (int i = 0; i < variables.size(); i++) {
            log_score += score(sf, variables.get(i), parent_set(i));
        }
        return log_score;
    }

    private int discretize_internal(int steps_ub, boolean at_least_one_edge) {
        List<List<Double>> disc_policy = discretization_policy();
        for (int i = 0; i < steps_ub; i++) {
            discretization_step(at_least_one_edge);
            List<List<Double>> new_policy = discretization_policy();
            if (disc_policy.equals(new_policy)) {
                return i + 1;
            } else {
                disc_policy = new_policy;
            }
        }
        return steps_ub;
    }

    public int discretize(int steps_ub, boolean repair_initial, boolean at_least_one_edge) {
        List<Integer> before = variables.stream()
                .map(Variable::cardinality)
                .collect(Collectors.toList());

        int res = discretize_internal(steps_ub, at_least_one_edge);

        if (repair_initial) {
            for (int i = 0; i < variables.size(); i++) {
                if (variables.get(i).discretization_edges().isEmpty()) {
                    variables.get(i).initial(before.get(i));
                }
            }
        }
        return res;
    }
}
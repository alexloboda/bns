import java.util.List;
import java.util.stream.Collectors;

public class BayesianNetwork {
    private List<Variable> variables;
    private Graph g;

    BayesianNetwork(List<Variable> variables) {
        g = new Graph(variables.size());
        this.variables = variables;
        LogFactorial lf = new LogFactorial();
        variables.forEach(v -> v.setLF(lf));
    }

    void add_edge(int v, int u) {
        g.add_edge(v, u);
    }

    private List<Variable> parent_set(int v) {
        return g.ingoing_edges(v).stream()
                .map(x -> variables.get(x))
                .collect(Collectors.toList());
    }

    private void discretization_step() {
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
            var.discretize(ps, cs, ss);
        }
    }

    private List<List<Double>> discretization_policy() {
        return variables.stream()
                .map(Variable::discretization_edges)
                .collect(Collectors.toList());
    }

    double bde_score(BDEScoringFunction bde) {
        double log_score = 0.0;
        for (int i = 0; i < variables.size(); i++) {
            log_score += variables.get(i).bde(bde, parent_set(i));
        }
        return log_score;
    }

    private int discretize_internal(int steps_ub) {
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

    int discretize(int steps_ub, boolean repair_initial) {
        List<Integer> before = variables.stream()
                .map(Variable::cardinality)
                .collect(Collectors.toList());

        int res = discretize_internal(steps_ub);

        if (repair_initial) {
            for (int i = 0; i < variables.size(); i++) {
                if (variables.get(i).discretization_edges().isEmpty()) {
                    variables.get(i).initial(before.get(i));
                }
            }
        }
        return res;
    }

    void print_discretization() {
        for (Variable v : variables) {
            for (double edge : v.discretization_edges()) {
                System.out.print(edge + "\t");
            }
            System.out.println();
        }
    }
}

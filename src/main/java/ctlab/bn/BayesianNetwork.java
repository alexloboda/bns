package ctlab.bn;

import ctlab.bn.sf.ScoringFunction;
import ctlab.graph.Graph;

import java.util.*;
import java.util.stream.Collectors;

public class BayesianNetwork {
    private List<Variable> variables;
    private Graph g;
    private Map<String, Integer> names;

    public BayesianNetwork(List<Variable> variables) {
        g = new Graph(variables.size());
        this.variables = variables;
        Collections.shuffle(this.variables);
        LogFactorial lf = new LogFactorial();
        variables.forEach(v -> v.setLF(lf));
        names = new HashMap<>();
        for (int i = 0; i < variables.size(); i++) {
            names.put(this.variables.get(i).getName(), i);
        }
    }

    public int getID(String name) {
        return names.get(name);
    }

    public BayesianNetwork(BayesianNetwork bn) {
        variables = new ArrayList<>();
        for (Variable v : bn.variables) {
            variables.add(new Variable(v));
        }
        g = new Graph(bn.g);
        names = new HashMap<>(bn.names);
    }

    public void addEdge(int v, int u) {
        g.add_edge(v, u);
    }

    public void removeEdge(int v, int u) {
        g.remove_edge(v, u);
    }

    private List<Variable> parentSet(int v) {
        return g.ingoing_edges(v).stream()
                .map(x -> variables.get(x))
                .collect(Collectors.toList());
    }

    private void discretizationStep() {
        List<Integer> order = g.top_sort();
        for (int v : order) {
            Variable var = variables.get(v);
            List<Variable> ps = parentSet(v);
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

    private List<List<Double>> discretizationPolicy() {
        return variables.stream()
                .map(Variable::discretizationEdges)
                .collect(Collectors.toList());
    }

    public void randomPolicy() {
        variables.forEach(Variable::randomPolicy);
    }

    public int observations() {
        return variables.stream().findAny().get().obsNum();
    }

    public Variable var(int v) {
        return variables.get(v);
    }

    public double score(int v, ScoringFunction sf) {
        return sf.score(variables.get(v), parentSet(v));
    }

    public double scoreIncluding(int v, ScoringFunction sf, int u) {
        List<Integer> ps = g.ingoing_edges(v);
        assert !ps.contains(u);
        ps.add(v);
        return sf.score(variables.get(v), ps.stream().map(x -> variables.get(x)).collect(Collectors.toList()));
    }

    public double scoreExcluding(int v, ScoringFunction sf, int u) {
        List<Integer> ps = g.ingoing_edges(v);
        assert ps.contains(u);
        ps.remove(v);
        return sf.score(variables.get(v), ps.stream().map(x -> variables.get(x)).collect(Collectors.toList()));
    }

    public void clearEdges() {
        for (int u = 0; u < size(); u++) {
            for (int v: new ArrayList<>(ingoingEdges(u))) {
                removeEdge(v, u);
            }
        }
    }

    public int size() {
        return variables.size();
    }

    public boolean edgeExists(int v, int u) {
        return g.edge_exists(v, u);
    }

    public boolean pathExists(int v, int u) {
        return g.path_exists(v, u);
    }

    public List<Integer> ingoingEdges(int v) {
        return g.ingoing_edges(v);
    }

    int discretize(int steps_ub) {
        List<List<Double>> disc_policy = discretizationPolicy();
        int ret = -1;
        for (int i = 0; i < steps_ub; i++) {
            discretizationStep();
            List<List<Double>> new_policy = discretizationPolicy();
            if (disc_policy.equals(new_policy)) {
                ret = i + 1;
                break;
            } else {
                disc_policy = new_policy;
            }
        }
        int[] cs = new int[observations() + 1];
        for (Variable v: variables) {
            cs[v.cardinality()]++;
        }
        return ret != -1 ? ret : steps_ub;
    }
}

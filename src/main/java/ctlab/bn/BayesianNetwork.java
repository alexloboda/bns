package ctlab.bn;

import ctlab.bn.sf.ScoringFunction;
import ctlab.graph.Graph;
import org.apache.commons.math3.util.MathArrays;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BayesianNetwork {
    private List<Variable> variables;
    private Graph g;
    private Map<String, Integer> names;

    public BayesianNetwork(List<Variable> variables) {
        g = new Graph(variables.size());
        this.variables = variables;
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

    public void setCallback(BiConsumer<Integer, Integer> callback) {
        g.setCallback(callback);
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
        g.addEdge(v, u);
    }

    public void removeEdge(int v, int u) {
        g.removeEdge(v, u);
    }

    private List<Variable> parentSet(int v) {
        return g.ingoingEdges(v).stream()
                .map(x -> variables.get(x))
                .collect(Collectors.toList());
    }

    private void discretizationStep() {
        List<Integer> order = g.topSort();
        for (int v : order) {
            Variable var = variables.get(v);
            List<Variable> ps = parentSet(v);
            List<Variable> cs = g.outgoingEdges(v).stream()
                    .map(x -> variables.get(x))
                    .collect(Collectors.toList());
            List<List<Variable>> ss = g.outgoingEdges(v).stream()
                    .map(x -> g.ingoingEdges(x))
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
        List<Integer> ps = g.ingoingEdges(v);
        assert !ps.contains(u);
        ps.add(u);
        return sf.score(variables.get(v), ps.stream().map(x -> variables.get(x)).collect(Collectors.toList()));
    }

    public double scoreExcluding(int v, ScoringFunction sf, int u) {
        List<Integer> ps = g.ingoingEdges(v);
        assert ps.contains(u);
        ps.remove((Integer)u);
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
        return g.edgeExists(v, u);
    }

    public boolean pathExists(int v, int u) {
        return g.pathExists(v, u);
    }

    public List<Integer> ingoingEdges(int v) {
        return g.ingoingEdges(v);
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

    public List<Integer> shuffleVariables(Random random) {
        List<Integer> perm = IntStream.range(0, variables.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(perm, random);
        Map<Integer, Integer> inverseMap = IntStream.range(0, variables.size()).boxed()
                .collect(Collectors.toMap(perm::get, x -> x));
        names = names.keySet().stream()
                .collect(Collectors.toMap(x -> x, x -> inverseMap.get(names.get(x))));
        List<Variable> newList = new ArrayList<>();
        for (int k: perm) {
            newList.add(variables.get(k));
        }
        variables = newList;
        return perm;
    }
}

package ctlab.mc5.bn;

import ctlab.mc5.bn.sf.ScoringFunction;
import ctlab.mc5.graph.Graph;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BayesianNetwork {
    private List<Variable> variables;
    private Graph g;
    private ScoringFunction sf;
    private Map<String, Integer> names;

    public BayesianNetwork(List<Variable> variables) {
        this(variables, null);
    }

    public BayesianNetwork(List<Variable> variables, ScoringFunction sf) {
        g = new Graph(variables.size());
        this.variables = variables;
        LogFactorial lf = new LogFactorial();
        variables.forEach(v -> v.setLF(lf));
        names = new HashMap<>();
        for (int i = 0; i < variables.size(); i++) {
            names.put(this.variables.get(i).getName(), i);
        }
        this.sf = sf;
    }

    public void setScoringFunction(ScoringFunction sf) {
        this.sf = sf;
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
        sf = bn.sf;
    }

    public void addEdge(int from, int to) {
        g.addEdge(from, to);
    }

    public void removeEdge(int from, int to) {
        g.removeEdge(from, to);
    }

    public int getEdgeCount() {
        return g.getEdgeCount();
    }

    private List<Variable> parentSet(int to) {
        return g.ingoingEdges(to).stream()
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

    public double score(int to) {
        return sf.score(variables.get(to), parentSet(to));
    }

    public double scoreIncluding(int from, int to) {
        List<Integer> ps = g.ingoingEdges(to);
        assert !ps.contains(from);
        ps.add(from);
        return sf.score(variables.get(to), ps.stream().map(x -> variables.get(x)).collect(Collectors.toList()));
    }

    public double scoreExcluding(int from, int to) {
        List<Integer> ps = g.ingoingEdges(to);
        assert ps.contains(from);
        ps.remove((Integer) from); // removing the element with this value ps.remove(from) is illegal
        return sf.score(variables.get(to), ps.stream().map(x -> variables.get(x)).collect(Collectors.toList()));
    }

    public void clearEdges() {
        for (int to = 0; to < size(); to++) {
            for (int from : ingoingEdges(to)) {
                removeEdge(from, to);
            }
        }
    }

    public int size() {
        return variables.size();
    }

    public boolean edgeExists(int from, int to) {
        return g.edgeExists(from, to);
    }

    public boolean pathExists(int from, int to) {
        return g.pathExists(from, to);
    }

    public List<Integer> ingoingEdges(int to) {
        return g.ingoingEdges(to);
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
        for (Variable v : variables) {
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
        for (int k : perm) {
            newList.add(variables.get(k));
        }
        variables = newList;
        return perm;
    }

    public boolean canBeVShape(int v, int u) {
        if (g.edgeExists(v, u)) {
            /*
             * if we have another edge (x,u), x != v
             * then (v,u) and (x,u) make up a V-shape,
             * breaking equivalence
             */
            if (g.inDegree(u) > 1) {
                return true;
            }
            /*
             * if we have another an edge (x,v),
             * then if (v,u) reversed (u,v) and (x,v) make up a V-shape,
             * breaking equivalence
             */
            if (g.inDegree(v) > 0) {
                return true;
            }
        }
        return false;
    }
}

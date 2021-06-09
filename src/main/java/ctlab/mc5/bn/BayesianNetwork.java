package ctlab.mc5.bn;

import ctlab.mc5.bn.sf.ScoringFunction;
import ctlab.mc5.graph.Graph;
import org.apache.commons.math3.util.Pair;

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
        sf.init(g.size());
    }

    public void setScoringFunction(ScoringFunction sf) {
        this.sf = sf;
    }

    public ScoringFunction getScoringFunction() {
        return sf;
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
        sf = bn.sf.cp();
    }

    public Pair<Integer, Integer> randomEdge(SplittableRandom random) {
        return g.randomEdge(random);
    }

    public boolean isSubscribed(int from, int to) {
        return g.isSubscribed(from, to);
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

    public List<Variable> parentSet(int to) {
        List<Integer> numbers = ingoingEdges(to);
        List<Variable> set = new ArrayList<>(numbers.size());
        for (Integer number : numbers) {
            set.add(variables.get(number));
        }
        return set;
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
        return sf.score(var(to), parentSet(to), variables.size());
    }

    public double scoreIncluding(int from, int to) {
        List<Variable> parents = parentSet(to);
        assert !parents.contains(var(from));

        parents.add(var(from));
        return sf.score(var(to), parents, variables.size());
    }

    public double scoreExcluding(int from, int to) {
        List<Variable> parents = parentSet(to);
        assert parents.contains(var(from));
        parents.remove(var(from));
        return sf.score(var(to), parents, variables.size());
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
        if (g.edgeExists(from, to)) {
            return true;
        }
        return g.pathExists(from, to);
    }

    public List<Integer> ingoingEdges(int to) {
        return g.ingoingEdges(to);
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
}

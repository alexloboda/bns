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
    private IngoingCache cache;


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
        this.cache = new IngoingCache(g.size());
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
        sf = bn.sf;
        cache = new IngoingCache(bn.cache);
    }

    public Pair<Integer, Integer> randomEdge(SplittableRandom random) {
        return g.randomEdge(random);
    }

    static class IngoingCache {
        ArrayList<LinkedHashSet<Variable>> map;

        IngoingCache(int n) {
            map = new ArrayList<>();
            for (int i = 0; i < n; ++i) {
                map.add(new LinkedHashSet<>());
            }
        }

        IngoingCache(IngoingCache other) {
            map = new ArrayList<>();
            for (int i = 0; i < other.map.size(); ++i) {
                map.add(new LinkedHashSet<>(other.map.get(i)));
            }
        }

        void add(int to, Variable v) {
            assert !map.get(to).contains(v);
            map.get(to).add(v);
        }

        void rem(int to, Variable v) {
            assert map.get(to).contains(v);
            map.get(to).remove(v);
        }

        Set<Variable> get(int to) {
            return map.get(to);
        }
    }

    public boolean isSubscribed(int from, int to) {
        return g.isSubscribed(from, to);
    }

    public void addEdge(int from, int to) {
        g.addEdge(from, to);
        cache.add(to, var(from));
    }

    public void removeEdge(int from, int to) {
        g.removeEdge(from, to);
        cache.rem(to, var(from));
    }

    public int getEdgeCount() {
        return g.getEdgeCount();
    }

    public Set<Variable> parentSet(int to) {
        return cache.get(to);
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
        Set<Variable> parents = cache.get(to);
        assert !parents.contains(var(from));

        parents.add(var(from));
        double val = sf.score(var(to), parents, variables.size());
        parents.remove(var(from));
        return val;
    }

    public double scoreExcluding(int from, int to) {
        Set<Variable> parents = cache.get(to);
        assert parents.contains(var(from));
        parents.remove(var(from));
        double val = sf.score(var(to), parents, variables.size());
        parents.add(var(from));
        return val;
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

    public int getDegree(int to) {
        return cache.get(to).size();
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

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
//    private IngoingCache cache;


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
//        this.cache = new IngoingCache(g.size());
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
//        cache = new IngoingCache(bn.cache);
    }

    public Pair<Integer, Integer> randomEdge(SplittableRandom random) {
        return g.randomEdge(random);
    }

//    public static class SuperSet extends TreeSet<Variable> {
//        int val1 = 0;
//        int val2 = 0;
//
//        public SuperSet() {
//        }
//
//        public SuperSet(Set<Variable> variables) {
//            super(variables);
//            if (variables instanceof SuperSet) {
//                val1 = ((SuperSet) variables).val1;
//                val2 = ((SuperSet) variables).val2;
//            } else {
//                variables.forEach(v -> {
//                    val1 += v.getNumber();
//                    val2 ^= v.getNumber();
//                });
//            }
//        }
//
//        @Override
//        public boolean add(Variable t) {
//            val1 += t.getNumber();
//            val2 ^= t.getNumber();
//            return super.add(t);
//        }
//
//        @Override
//        public boolean remove(Object o) {
//            if (o instanceof Variable) {
//                val1 -= ((Variable) o).getNumber();
//                val2 ^= ((Variable) o).getNumber();
//            }
//            return super.remove(o);
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (o instanceof SuperSet) {
//                return val1 == ((SuperSet) o).val1 && val2 == ((SuperSet) o).val2 && super.equals(o);
//            }
//            return super.equals(o);
//        }
//
//        @Override
//        public int hashCode() {
//            return val1 ^ ~val2;
//        }
//    }

//    static class IngoingCache {
//        ArrayList<Set<Variable>> map;
//
//        IngoingCache(int n) {
//            map = new ArrayList<>();
//            for (int i = 0; i < n; ++i) {
//                map.add(new TreeSet<>());
//            }
//        }
//
//        IngoingCache(IngoingCache other) {
//            map = new ArrayList<>();
//            for (int i = 0; i < other.map.size(); ++i) {
//                map.add(new TreeSet<>(other.map.get(i)));
//            }
//        }
//
//        void add(int to, Variable v) {
//            assert !map.get(to).contains(v);
//            map.get(to).add(v);
//        }
//
//        void rem(int to, Variable v) {
//            assert map.get(to).contains(v);
//            map.get(to).remove(v);
//        }
//
//        Set<Variable> get(int to) {
//            return map.get(to);
//        }
//    }

    public boolean isSubscribed(int from, int to) {
        return g.isSubscribed(from, to);
    }

    public void addEdge(int from, int to) {
        g.addEdge(from, to);
//        cache.add(to, var(from));
    }

    public void removeEdge(int from, int to) {
        g.removeEdge(from, to);
//        cache.rem(to, var(from));
    }

    public int getEdgeCount() {
        return g.getEdgeCount();
    }

    public List<Variable> parentSet(int to) {

//        assert (cache.get(to)).equals(ingoingEdges(to).stream().map(x -> variables.get(x)).collect(Collectors.toSet()));
//        return cache.get(to);
        return ingoingEdges(to).stream().map(x -> variables.get(x)).collect(Collectors.toList());
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
        double val = sf.score(var(to), parents, variables.size());
        parents.remove(var(from));
        return val;
    }

    public double scoreExcluding(int from, int to) {
        List<Variable> parents = parentSet(to);
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

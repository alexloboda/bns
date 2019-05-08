package ctlab.bn;

import ctlab.bn.prior.PriorDistribution;
import ctlab.bn.prior.ScaleFreePrior;
import ctlab.bn.sf.ScoringFunction;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BayesianNetwork {
    private List<Variable> variables;
    private Graph g;
    private PriorDistribution pd;
    private List<Cache> caches;
    private Map<String, Integer> names;

    public BayesianNetwork(List<Variable> variables) {
        g = new Graph(variables.size());
        this.variables = variables;
        LogFactorial lf = new LogFactorial();
        variables.forEach(v -> v.setLF(lf));
        pd = new ScaleFreePrior(variables.size(), 2);
        caches = IntStream.range(0, variables.size()).mapToObj(x -> new Cache()).collect(Collectors.toList());
        names = new HashMap<>();
        for (int i = 0; i < variables.size(); i++) {
            names.put(variables.get(i).getName(), i);
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
        pd = bn.pd.clone();
        caches = IntStream.range(0, variables.size()).mapToObj(x -> new Cache()).collect(Collectors.toList());
        names = new HashMap<>(bn.names);
    }

    public void set_prior_distribution(PriorDistribution pd) {
        this.pd = pd;
    }

    public void add_edge(int v, int u) {
        pd.insert(v, u);
        g.add_edge(v, u);
    }

    public void remove_edge(int v, int u) {
        pd.remove(v, u);
        g.remove_edge(v, u);
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

    public List<List<Double>> discretization_policy() {
        return variables.stream()
                .map(Variable::discretization_edges)
                .collect(Collectors.toList());
    }

    public int cardinality(int v) {
        return variables.get(v).cardinality();
    }

    public void random_policy() {
        caches.forEach(Cache::clear);
        variables.forEach(Variable::random_policy);
    }

    public int observations() {
        return variables.stream().findAny().get().obsNum();
    }

    public Variable var(int v) {
        return variables.get(v);
    }

    public double logPrior() {
        return pd.value();
    }

    private static double likelihoodsSum(double ll1, double ll2) {
        if (ll1 == Double.NEGATIVE_INFINITY) {
            return ll2;
        }
        if (ll2 == Double.NEGATIVE_INFINITY) {
            return ll1;
        }
        double maxLL = Math.max(ll1, ll2);
        ll1 -= maxLL;
        ll2 -= maxLL;
        return Math.log(Math.exp(ll1) + Math.exp(ll2)) + maxLL;
    }

    public double score(int v, ScoringFunction sf) {
        Cache cache = caches.get(v);
        List<Integer> ps = g.ingoing_edges(v);
        Double value = cache.get(ps);
        if (value == null) {
            value = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < 100; i++) {
                variables.get(v).random_policy();
                for (Variable var: parent_set(v)) {
                    var.random_policy();
                }
                value = likelihoodsSum(value, sf.score(variables.get(v), parent_set(v)));
            }
            value -= Math.log(100);
            cache.add(ps, value);
        }
        return value;
    }

    public void clear_edges() {
        for (int u = 0; u < size(); u++) {
            for (int v: new ArrayList<>(ingoing_edges(u))) {
                remove_edge(v, u);
            }
        }
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
        int ret = -1;
        for (int i = 0; i < steps_ub; i++) {
            discretization_step();
            List<List<Double>> new_policy = discretization_policy();
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

    private static class Cache {
        private static final int CACHE_SIZE = 20_000;

        private Queue<List<Integer>> queue;
        private Map<List<Integer>, Double> map;

        Cache() {
            queue = new ArrayDeque<>();
            map = new HashMap<>();
        }

        Double get(List<Integer> ps) {
            return map.getOrDefault(ps, null);
        }

        void add(List<Integer> ps, double score) {
            if (queue.size() == CACHE_SIZE) {
                List<Integer> key = queue.poll();
                map.remove(key);
            }
            map.put(ps, score);
            queue.add(ps);
        }

        void clear() {
            map.clear();
            queue.clear();
        }
    }
}

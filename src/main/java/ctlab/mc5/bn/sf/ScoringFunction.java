package ctlab.mc5.bn.sf;

import ctlab.mc5.bn.Variable;
import org.apache.commons.math3.util.Pair;

import java.io.Serializable;
import java.util.*;

public abstract class ScoringFunction implements Serializable {

    private static class Cache implements Serializable {
        private static final int CACHE_SIZE = 20_000;

        private Queue<List<Variable>> queue;
        private Map<List<Variable>, Double> map;

        Cache() {
            queue = new ArrayDeque<>();
            map = new HashMap<>();
        }

        Double get(List<Variable> ps) {
            return map.getOrDefault(ps, null);
        }

        void add(List<Variable> ps, double score) {
            if (queue.size() == CACHE_SIZE) {
                List<Variable> key = queue.poll();
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

    private List<Cache> ht;
    ScoringFunction() {
        ht = new ArrayList<>();
    }

    public void init(int n) {
        for (int i = 0 ; i < n ; i++) {
            ht.add(i, new Cache());
        }
    }

    public ScoringFunction cp() {
        ScoringFunction sf =  cp_internal();
        sf.init(ht.size());
        return sf;
    }

    abstract public ScoringFunction cp_internal();

    public double score(Variable v, List<Variable> ps, int n) {
        if (ps.size() > 10) {
            return Double.NEGATIVE_INFINITY;
        }
        Collections.sort(ps);
        Double resCache = ht.get(v.getNumber()).get(ps);
        if (resCache != null) {
            assert score(v.mapObs(ps).getFirst(), v.mapObs(ps).getSecond(), v.cardinality()) == resCache;
            return resCache;
        }
        Pair<int[], int[]> cls = v.mapObs(ps);

        double res = score(cls.getFirst(), cls.getSecond(), v.cardinality());
        ht.get(v.getNumber()).add(ps, res);
        return res;
    }

    abstract double score(int[] parent_cls, int[] all_cls, int cardinality);

    public static ScoringFunction parse(String s) {
        String[] parts = s.split("\\s");

        switch (parts[0]) {
            case "BDE":
                return new BDE(Double.parseDouble(parts[1]));
            case "IC":
                return new InformationSF(Double.parseDouble(parts[1]));
            case "K2":
                return new K2();
            default:
                throw new IllegalArgumentException("Unknown scoring function: " + parts[0]);
        }
    }
}

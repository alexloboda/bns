package ctlab.mc5.bn.sf;

import ctlab.mc5.bn.Variable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ScoringFunction {

    static class LRUCache {
        final Map<Variable, Map<Set<Variable>, Double>> map = new HashMap<>();

        Double get(Variable v, Set<Variable> parents) {
            synchronized (map) {
                if (map.containsKey(v)) {
                    return map.get(v).get(parents);
                }
                return null;
            }
        }

        void add(Variable v, Set<Variable> parents, double res) {
            synchronized (map) {
                if (map.containsKey(v)) {
                    map.get(v).put(new HashSet<>(parents), res);
                } else {
                    Map<Set<Variable>, Double> mapik = new HashMap<>();
                    mapik.put(new HashSet<>(parents), res);
                    map.put(v, mapik);
                }
            }
        }
    }

    LRUCache ht;

    ScoringFunction() {
        ht = new LRUCache();
    }

    public double score(Variable v, Set<Variable> ps, int n) {
        Double resCache = ht.get(v, ps);
        if (resCache != null) {
            return resCache;
        }

        int[] parent_cls = v.mapObs(ps);

        int[] all_cls = v.mapObsAnd(ps);

        double res = score(parent_cls, all_cls, v.cardinality());
        ht.add(v, ps, res);
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

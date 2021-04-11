package ctlab.mc5.bn.sf;

import ctlab.mc5.bn.Variable;
import ctlab.mc5.bn.action.Cache;
import ctlab.mc5.bn.action.HashTable;
import ctlab.mc5.bn.action.HashTableCache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ScoringFunction {

    static class LRUCache {
        Map<Variable, Map<List<Variable>, Double>> map = new ConcurrentHashMap<>();

        boolean contains(Variable v, List<Variable> parents) {
            if (map.containsKey(v)) {
                return map.get(v).containsKey(parents);
            }
            return false;
        }

        void add(Variable v, List<Variable> parents, double res) {
            if (map.containsKey(v)) {
                map.get(v).put(parents, res);
            } else {
                map.put(v, new ConcurrentHashMap<>(Map.of(parents, res)));
            }
        }

        Double get(Variable v, List<Variable> parents) {
            return map.get(v).get(parents);
        }
    }

    LRUCache ht;

    ScoringFunction() {
        ht = new LRUCache();
    }

    public double score(Variable v, List<Variable> ps, int n) {
        if (ht.contains(v, ps)) {
            return ht.get(v, ps);
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

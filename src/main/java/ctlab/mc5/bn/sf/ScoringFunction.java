package ctlab.mc5.bn.sf;

import ctlab.mc5.bn.Variable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ScoringFunction {

    static class LRUCache {
        final ArrayList<Map<Set<Variable>, Double>> map = new ArrayList<>();
        boolean inited = false;

        void init(int varSize) {
            if (inited) return;
            inited = true;
            for (int i = 0; i < varSize; ++i) {
                map.add(new ConcurrentHashMap<>());
            }
        }

        Double get(int num, Set<Variable> parents) {

            final Map<Set<Variable>, Double> curParents = map.get(num);
//            synchronized (curParents) {
            return curParents.get(parents);
//            }
        }

        void add(int num, Set<Variable> parents, double res) {
            final Map<Set<Variable>, Double> curParents = map.get(num);
            Set<Variable> copySet = new LinkedHashSet<>(parents);
//            synchronized (curParents) {
            curParents.put(copySet, res);
//            }
        }
    }

    LRUCache ht;

    ScoringFunction() {
        ht = new LRUCache();
    }

    public void init(int n) {
        ht.init(n);
    }

    public double score(Variable v, Set<Variable> ps, int n) {
//        Double resCache = ht.get(v.getNumber(), ps);
//        if (resCache != null) {
//            return resCache;
//        }

        Set<Variable> varik = new HashSet<>(ps);

        double parent_cls = v.mapObs(varik);

        varik.add(v);
        double all_cls = v.mapObs(varik);

//        double res = score(parent_cls, all_cls, v.cardinality());
//        ht.add(v.getNumber(), ps, res);
        return all_cls - parent_cls;
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

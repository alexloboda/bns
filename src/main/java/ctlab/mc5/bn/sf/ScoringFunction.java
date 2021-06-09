package ctlab.mc5.bn.sf;

import ctlab.mc5.bn.Variable;
import org.apache.commons.math3.util.Pair;

import java.io.Serializable;
import java.util.*;

public abstract class ScoringFunction implements Serializable {

    static class LRUCache implements Serializable {

        final ArrayList<Map<List<Variable>, Double>> map = new ArrayList<>();
        boolean inited = false;

        void init(int varSize) {
            if (inited) return;
            inited = true;
            for (int i = 0; i < varSize; ++i) {
                map.add(new HashMap<>());
            }
        }

        Double get(int num, List<Variable> parents) {
            final Map<List<Variable>, Double> curParents = map.get(num);
            return curParents.get(parents);
        }

        void add(int num, List<Variable> parents, double res) {
            final Map<List<Variable>, Double> curParents = map.get(num);
            List<Variable> copySet;
            copySet = new ArrayList<>(parents);
            curParents.put(copySet, res);
        }
    }

    LRUCache ht;
    ScoringFunction() {
        ht = new LRUCache();
    }

    public void init(int n) {
        ht.init(n);
    }

    public ScoringFunction cp() {
        ScoringFunction sf =  cp_internal();
        sf.init(ht.map.size());
        return sf;
    }

    abstract public ScoringFunction cp_internal();

    public double score(Variable v, List<Variable> ps, int n) {
        if (ps.size() > 10) {
            return Double.NEGATIVE_INFINITY;
        }
        Collections.sort(ps);
        Double resCache = ht.get(v.getNumber(), ps);
        if (resCache != null) {
            assert score(v.mapObs(ps).getFirst(), v.mapObs(ps).getSecond(), v.cardinality()) == resCache;
            return resCache;
        }
        Pair<int[], int[]> cls = v.mapObs(ps);

        double res = score(cls.getFirst(), cls.getSecond(), v.cardinality());
        ht.add(v.getNumber(), ps, res);
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

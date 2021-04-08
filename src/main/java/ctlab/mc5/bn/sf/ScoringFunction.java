package ctlab.mc5.bn.sf;

import ctlab.mc5.bn.Variable;

import java.util.*;

public abstract class ScoringFunction {

    int[] array1;
    int[] array2;
    int[] array3;
    boolean init = false;

    void init(int n, int len) {
        if (init) return;
        init = true;
        array1 = new int[len + 1];
        array2 = new int[len + 1];
        array3 = new int[n + 1];
    }

    public double score(Variable v, List<Variable> ps, int n) {
        init(n, v.obsNum());
        int[] parent_cls = v.mapObsNoMem(ps, array1, array3);

        int[] all_cls = v.mapObsNoMemAnd(ps, v, array2, array3);

        return score(parent_cls, all_cls, v.cardinality());
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

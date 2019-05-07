package ctlab.bn.sf;

import ctlab.bn.Variable;

import java.util.*;

public abstract class ScoringFunction {
    public double score(Variable v, List<Variable> ps) {
        int[] parent_cls = v.map_obs(ps);

        List<Variable> vs = new ArrayList<>(ps);
        vs.add(v);

        int[] all_cls = v.map_obs(vs);

        return score(parent_cls, all_cls, v.cardinality());
    }

    abstract double score(int[] parent_cls, int[] all_cls, int cardinality);

    public static ScoringFunction parse(String s) {
        String[] parts = s.split("\\s");

        switch (parts[0]){
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

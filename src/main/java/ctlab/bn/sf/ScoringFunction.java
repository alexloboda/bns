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
}

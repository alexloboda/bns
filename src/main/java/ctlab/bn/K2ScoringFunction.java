package ctlab.bn;

import java.util.*;

import static org.apache.commons.math3.special.Gamma.logGamma;

public class K2ScoringFunction {
    private double iss;

    public K2ScoringFunction(int iss){
        this.iss = iss;
    }

    public K2ScoringFunction() {
        iss = 1;
    }

    double score(int[] parent_cls, int[] all_cls, int cardinality) {
        int num_cls = Arrays.stream(parent_cls).max().getAsInt() + 1;
        int num_all_cls = Arrays.stream(all_cls).max().getAsInt() + 1;

        int n = parent_cls.length;
        List<Set<Integer>> corr = new ArrayList<>();
        for (int i = 0; i < num_cls; i++) {
            corr.add(new HashSet<>());
        }

        int[] occ_parent_cls = new int[num_cls];
        int[] occ_all_cls = new int[num_all_cls];

        for (int i = 0; i < n; i++) {
            corr.get(parent_cls[i]).add(all_cls[i]);
            occ_parent_cls[parent_cls[i]]++;
            occ_all_cls[all_cls[i]]++;
        }

        double value = 0.0;

        double iss1 = iss / num_cls;
        double iss2 = iss / (num_cls * cardinality);

        for (int i = 0; i < num_cls; i++) {
            for (int cl : corr.get(i)) {
                value += logGamma(occ_all_cls[cl] + iss2);
                value -= logGamma(iss2);
            }

            value += logGamma(iss1);
            value -= logGamma(occ_parent_cls[i] + iss1);
        }

        return value;
    }
}

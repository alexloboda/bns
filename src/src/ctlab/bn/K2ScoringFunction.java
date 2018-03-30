package ctlab.bn;

import java.util.*;

public class K2ScoringFunction {
    private int iss;
    private LogFactorial lf;

    public K2ScoringFunction(int iss){
        this.iss = iss;
        lf = new LogFactorial();
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

        for (int i = 0; i < num_cls; i++) {
            for (int cl : corr.get(i)) {
                value += lf.value(occ_all_cls[cl]);
            }

            value += lf.value(cardinality - 1);
            value -= lf.value(occ_parent_cls[i] + cardinality - 1);
        }

        return value;
    }
}

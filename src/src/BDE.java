import static org.apache.commons.math3.special.Gamma.logGamma;

import java.util.*;

public class BDE {
    private int iss;

    BDE(int iss){
        this.iss = iss;
    }

    double score(int[] parent_cls, int[] all_cls, int cardinality) {
        int num_cls = Arrays.stream(parent_cls).max().getAsInt() - 1;
        int num_all_cls = Arrays.stream(all_cls).max().getAsInt() - 1;
        int n = parent_cls.length;
        List<Set<Integer>> corr = new ArrayList<>(Collections.nCopies(num_cls, new HashSet<>()));
        int[] occ_parent_cls = new int[num_cls];
        int[] occ_all_cls = new int[num_all_cls];

        for (int i = 0; i < n; i++) {
            corr.get(parent_cls[i]).add(all_cls[i]);
            occ_parent_cls[parent_cls[i]]++;
            occ_all_cls[all_cls[i]]++;
        }

        double value = 0.0;
        double iss_1 = (double)iss / num_cls;
        double iss_2 = (double)iss / (num_cls * cardinality);
        for (int i = 0; i < num_cls; i++) {
            value += logGamma(iss_1);
            value -= logGamma(occ_parent_cls[i] + iss_1);
            for (int cls : corr.get(i)) {
                value += logGamma(occ_all_cls[cls] + iss_2);
                value -= logGamma(iss_2);
            }
        }

        return value;
    }
}

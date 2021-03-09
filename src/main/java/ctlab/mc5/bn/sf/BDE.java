package ctlab.mc5.bn.sf;



import static org.apache.commons.math3.special.Gamma.logGamma;


public class BDE extends ScoringFunction {
    private double iss;

    public BDE(double iss) {
        this.iss = iss;
    }

    public BDE() {
        this.iss = 1;
    }

    double score(int[] parent_cls, int[] all_cls, int cardinality) {
        int num_cls = 0;
        int num_all_cls = 0;
        for (int v: parent_cls) {
            num_cls = Math.max(num_cls, v);
        }
        for (int v: all_cls) {
            num_all_cls = Math.max(num_all_cls, v);
        }
        num_cls += 1;
        num_all_cls += 1;

        int n = parent_cls.length;

        int[] occ_parent_cls = new int[num_cls];
        int[] occ_all_cls = new int[num_all_cls];

        for (int i = 0; i < n; i++) {
            occ_parent_cls[parent_cls[i]]++;
            occ_all_cls[all_cls[i]]++;
        }

        double value = 0.0;

        double iss1 = iss / num_cls;
        double iss2 = iss / (num_cls * cardinality);

        double log_gamma_iss1 = logGamma(iss1);
        double log_gamma_iss2 = logGamma(iss2);

        for (int i = 0; i < num_all_cls; i++) {
            value += logGamma(occ_all_cls[i] + iss2);
            value -= log_gamma_iss2;
        }

        for (int i = 0; i < num_cls; i++) {
            value += log_gamma_iss1;
            value -= logGamma(occ_parent_cls[i] + iss1);
        }

        return value;
    }
}

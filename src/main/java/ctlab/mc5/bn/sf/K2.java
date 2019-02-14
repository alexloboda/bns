package ctlab.mc5.bn.sf;

import ctlab.mc5.bn.LogFactorial;

public class K2 extends ScoringFunction {
    private LogFactorial lf;

    public K2() {
        lf = new LogFactorial();
    }

    @Override
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

        for (int i = 0; i < num_all_cls; i++) {
            value += lf.value(occ_all_cls[i]);
        }

        double nom = lf.value(cardinality - 1);
        for (int i = 0; i < num_cls; i++) {
            value += nom;
            value -= lf.value(occ_parent_cls[i] + cardinality - 1);
        }

        return value;
    }
}

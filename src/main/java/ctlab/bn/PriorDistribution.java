package ctlab.bn;

import java.util.Arrays;

public class PriorDistribution {
    private double[] ps;
    private LogFactorial lf;

    public PriorDistribution(int n, double gamma) {
        ps = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            ps[i] = Math.pow(i + 1, -gamma);
            sum += ps[i];
        }
        double finalSum = sum;
        ps = Arrays.stream(ps).map(x -> Math.log(x / finalSum)).toArray();
        lf = new LogFactorial();
    }

    public double value(Graph g) {
        double value = 0.0;
        value += lf.value(g.size());
        int[] occ = new int[g.size()];

        for (int i = 0; i < g.size(); i++) {
            occ[g.out_degree(i)]++;
        }

        for (int i = 0; i < g.size(); i++) {
            value -= lf.value(occ[i]);
            value += occ[i] * ps[i];
        }

        return value;
    }
}

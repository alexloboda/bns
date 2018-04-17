package ctlab.bn;

import java.util.Arrays;

public class PriorDistribution {
    private double[] ps;
    private LogFactorial lf;
    private int[] occ;
    private double loglik;

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
        occ = new int[n];
        occ[0] = n;
        loglik = n * ps[0];
    }

    public PriorDistribution(PriorDistribution pd) {
        lf = new LogFactorial();
        loglik = pd.loglik;
        ps = pd.ps.clone();
        occ = pd.occ.clone();
    }

    private void change(int k, int change) {
         if (change == 1) {
             loglik -= Math.log(occ[k] + 1);
         } else {
             loglik += Math.log(occ[k]);
         }
         occ[k] += change;
         loglik += change * ps[k];
    }

    public void remove(int k) {
        change(k, -1);
        change(k - 1, 1);
    }

    public void add(int k) {
        change(k, -1);
        change(k + 1, 1);
    }

    public double value() {
        return loglik;
    }
}

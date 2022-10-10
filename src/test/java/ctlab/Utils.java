package ctlab;

import org.apache.commons.math3.special.Beta;

public class Utils {
    public static double binomialCDF(double k, double n, double p) {
        if (k >= n) {
            return 1.0;
        }
        if (k <= 0.0) {
            return 0.0;
        }
        return Beta.regularizedBeta(1 - p, n - k, k + 1);
    }

    public static double binomialTest(int k, int n, double p) {
        double mean = p * n;
        double pos = k;
        if (k > mean) {
            pos = mean - (k - mean);
        }
        double lower = binomialCDF(pos, n, p);
        double term = k < mean ? -1.0 : 0.0;
        double higher = binomialCDF(mean + (mean - pos) + term, n, p);
        return Math.min(1.0, 1.0 - (higher - lower));
    }

}

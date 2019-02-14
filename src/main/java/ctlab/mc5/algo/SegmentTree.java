package ctlab.mc5;

import java.util.Arrays;
import java.util.SplittableRandom;

public class SegmentTree {
    private double[] lls;
    private double[] sum;
    private int n;
    private double beta;

    public SegmentTree(int size) {
        this(size, 1.0);
    }

    public SegmentTree(int size, double beta) {
        this.beta = beta;
        n = size;
        int sumSize = (size + 1) / 2;
        sum = new double[sumSize];
        lls = new double[n];
        Arrays.fill(lls, Float.NEGATIVE_INFINITY);
        Arrays.fill(sum, Float.NEGATIVE_INFINITY);
    }

    private int child(int k) {
        return 2 * k + 1;
    }

    private int parent(int k) {
        return (k - 1) / 2;
    }

    private double get_sum(int k) {
        if (k >= n) {
            return Double.NEGATIVE_INFINITY;
        }
        return k >= sum.length ? Math.min(beta * lls[k], 0.0) : sum[k];
    }

    public void set(int k, double ll) {
        if (Double.isNaN(ll)) {
            throw new IllegalArgumentException();
        }
        lls[k] = ll;
        if (k >= sum.length) {
            k = parent(k);
        }
        while(true) {
            Distribution d = new Distribution(k);
            sum[k] = d.maxLL + Math.log(d.sum);
            if (k == 0) {
                break;
            }
            k = parent(k);
        }
    }

    public double get(int k) {
        return lls[k];
    }

    public double likelihood() {
        return get_sum(0);
    }

    public int randomChoice(SplittableRandom re) {
        int k = 0;
        while(true) {
            if (k > n) {
                return parent(k);
            }
            Distribution d = new Distribution(k);
            double rv = re.nextDouble();
            double leftThreshold = d.left;
            double rightThreshold = d.left + d.right;
            if (rv < leftThreshold) {
                k = child(k);
            } else if (rv < rightThreshold) {
                k = child(k) + 1;
            } else {
                return k;
            }
        }
    }

    private class Distribution {
        public double left, right, current, maxLL, sum;

        private double NANSafe(float x) {
            return Float.isNaN(x) ? 0 : x;
        }

        public Distribution(int k) {
            double left_sum = get_sum(child(k));
            double right_sum = get_sum(child(k) + 1);
            double ll = Math.min(beta * lls[k], 0.0);
            maxLL = Math.max(ll, Math.max(left_sum, right_sum));
            left = NANSafe((float)Math.exp(left_sum - maxLL));
            right = NANSafe((float)Math.exp(right_sum - maxLL));
            current = NANSafe((float)Math.exp(ll - maxLL));
            sum = left + right + current;
            left /= sum;
            right /= sum;
            current /= sum;
        }
    }
}

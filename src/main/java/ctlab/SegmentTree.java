package ctlab;

import java.util.Collections;
import java.util.SplittableRandom;

public class SegmentTree {
    private double[] lls;
    private double[] sum;
    private int n;

    public SegmentTree(int size) {
        n = size;
        int sumSize = (size + 1) / 2;
        sum = Collections.nCopies(sumSize, Double.NEGATIVE_INFINITY)
                .stream()
                .mapToDouble(x -> x)
                .toArray();
        lls = Collections.nCopies(n, Double.NEGATIVE_INFINITY)
                .stream()
                .mapToDouble(x -> x)
                .toArray();
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
        return k >= sum.length ? lls[k] : sum[k];
    }

    public void set(int k, double ll) {
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

    public double likelihood() {
        return sum[0];
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

        public Distribution(int k) {
            double left_sum = get_sum(child(k));
            double right_sum = get_sum(child(k) + 1);
            maxLL = Math.max(lls[k], Math.max(left_sum, right_sum));
            left = Math.exp(left_sum - maxLL);
            right = Math.exp(right_sum - maxLL);
            current = Math.exp(lls[k] - maxLL);
            sum = left + right + current;
            left /= sum;
            right /= sum;
            current /= sum;
        }
    }
}

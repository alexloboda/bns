package ctlab;

import java.util.Arrays;
import java.util.SplittableRandom;

public class SegmentTree {
    private float[] lls;
    private float[] sum;
    private int n;

    public SegmentTree(int size) {
        n = size;
        int sumSize = (size + 1) / 2;
        sum = new float[sumSize];
        lls = new float[n];
        Arrays.fill(lls, Float.NEGATIVE_INFINITY);
        Arrays.fill(sum, Float.NEGATIVE_INFINITY);
    }

    private int child(int k) {
        return 2 * k + 1;
    }

    private int parent(int k) {
        return (k - 1) / 2;
    }

    private float get_sum(int k) {
        if (k >= n) {
            return Float.NEGATIVE_INFINITY;
        }
        return k >= sum.length ? lls[k] : sum[k];
    }

    public void set(int k, float ll) {
        if (Float.isNaN(ll)) {
            throw new IllegalArgumentException();
        }
        lls[k] = ll;
        if (k >= sum.length) {
            k = parent(k);
        }
        while(true) {
            Distribution d = new Distribution(k);
            sum[k] = d.maxLL + (float)Math.log(d.sum);
            if (k == 0) {
                break;
            }
            k = parent(k);
        }
    }

    public float get(int k) {
        return lls[k];
    }

    public float likelihood() {
        return sum[0];
    }

    public int randomChoice(SplittableRandom re) {
        int k = 0;
        while(true) {
            if (k > n) {
                return parent(k);
            }
            Distribution d = new Distribution(k);
            float rv = (float)re.nextDouble();
            float leftThreshold = d.left;
            float rightThreshold = d.left + d.right;
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
        public float left, right, current, maxLL, sum;

        private float NANSafe(float x) {
            return Float.isNaN(x) ? 0 : x;
        }

        public Distribution(int k) {
            float left_sum = get_sum(child(k));
            float right_sum = get_sum(child(k) + 1);
            maxLL = Math.max(lls[k], Math.max(left_sum, right_sum));
            left = NANSafe((float)Math.exp(left_sum - maxLL));
            right = NANSafe((float)Math.exp(right_sum - maxLL));
            current = NANSafe((float)Math.exp(lls[k] - maxLL));
            sum = left + right + current;
            left /= sum;
            right /= sum;
            current /= sum;
        }
    }
}

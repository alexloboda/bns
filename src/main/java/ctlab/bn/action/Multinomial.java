package ctlab.bn.action;

import ctlab.SegmentTree;

import java.util.*;
import java.util.function.Function;

public class Multinomial {
    public static final float EPS = 1e-6f;

    private int n;
    private int batchSize;
    private int batchesNum;
    private short mainCacheSize;
    private int hits;
    private double initialLL;
    private boolean initialized;

    private Function<Integer, Double> computeLL;
    private SplittableRandom re;

    private SegmentTree actions;
    private Cache cache;

    private short[] batchHits;
    private float[] batchMaxLL;
    private BitSet batchResolved;

    private HashMap<Short, Float> disabledActions;

    private int batchSize(int batch) {
        if (batch < batchesNum - 1) {
            return batchSize;
        } else {
            return n - ((batchesNum - 1) * batchSize);
        }
    }

    public void disableAction(short action, Float ll) {
        disabledActions.put(action, ll);
        if (initialized) {
            if (cache.contains(action)) {
                cache.disable(action);
                refreshCacheNode();
                return;
            }
            int b = batch(action);
            if (batchResolved.get(b)) {
                actions.set(b, (float)likelihoodSubtract(actions.get(b), ll));
            } else {
                actions.set(b, (float)likelihoodSubtract(actions.get(b), initialLL));
            }
        }
    }

    public void reEnableAction(short action) {
        if (!disabledActions.containsKey(action)) {
            return;
        }

        float ll = disabledActions.get(action);
        disabledActions.remove(action);
        if (cache.contains(action)) {
            cache.reEnable(action, ll);
            refreshCacheNode();
        } else {
            int b = batch(action);
            if (batchResolved.get(b)) {
                float batchLL = (float)likelihoodsSum(actions.get(b), resolveAction(action, ll - initialLL));
                actions.set(b, batchLL);
                refreshCacheNode();
            } else {
                actions.set(b, (float)likelihoodsSum(actions.get(b), initialLL));
            }
        }
    }

    public void deactivate() {
        for (Short action: disabledActions.keySet()) {
            reEnableAction(action);
        }
        disabledActions = new LinkedHashMap<>();
    }

    private double likelihoodsSum(double ll1, double ll2) {
        if (ll1 == Double.NEGATIVE_INFINITY) {
            return ll2;
        }
        if (ll2 == Double.NEGATIVE_INFINITY) {
            return ll1;
        }
        double maxLL = Math.max(ll1, ll2);
        ll1 -= maxLL;
        ll2 -= maxLL;
        return Math.log(Math.exp(ll1) + Math.exp(ll2)) + maxLL;
    }

    private double likelihoodSubtract(double ll1, double ll2) {
        double maxLL = Math.max(ll1, ll2);
        ll1 -= maxLL;
        ll2 -= maxLL;
        return Math.log(Math.exp(ll1) - Math.exp(ll2)) + maxLL;
    }

    public Multinomial(int maxSize, int batchesNum, short mainCacheSize, Function<Integer, Double> computeLL,
                       double initialLL, SplittableRandom re) {
        n = maxSize;
        this.mainCacheSize = mainCacheSize;
        this.batchesNum = batchesNum;
        this.batchSize = (int)Math.round(Math.ceil((double)n / batchesNum));
        this.computeLL = computeLL;
        this.initialLL = initialLL;
        this.re = re;
        this.disabledActions = new LinkedHashMap<>();
    }

    public double logLikelihood() {
        if (!initialized) {
            return Math.log(n) + initialLL;
        } else {
            return actions.likelihood();
        }
    }

    private void refreshCacheNode() {
        actions.set(batchesNum, cache.loglikelihood());
    }

    private void init() {
        actions = new SegmentTree(batchesNum + 1);
        batchResolved = new BitSet(batchesNum);
        cache = new HashTableCache(mainCacheSize, re);
        batchHits = new short[batchesNum];
        for (int i = 0; i < batchesNum; i++) {
            actions.set(i, (float)(initialLL + Math.log(batchSize(i))));
        }
        initialized = true;
        batchMaxLL = new float[batchesNum];
        Arrays.fill(batchMaxLL, Float.NEGATIVE_INFINITY);
    }

    private Short tryAction(int pos) {
        double ll = computeLL.apply(pos);
        if (Math.log(re.nextDouble()) < ll) {
            return (short)pos;
        } else {
            return null;
        }
    }

    public Short randomAction() {
        hits++;
        Short result;
        if (!initialized) {
            int pos = re.nextInt(n);
            result = tryAction(pos);
            if (hits > (batchSize + mainCacheSize) / 2) {
                init();
            }
            return result;
        }
        int node = actions.randomChoice(re);
        if (node == batchesNum) {
            return cache.randomAction();
        }
        if (batchResolved.get(node)) {
            int bs = batchSize(node);
            while (true) {
                int curr = re.nextInt(bs) + node * batchSize;
                if (cache.contains((short)curr)) {
                    continue;
                }
                double ll = computeLL.apply(curr);
                if (Math.log(re.nextDouble()) < ll - batchMaxLL[node]) {
                    return (short)curr;
                }
            }
        } else {
            batchHits[node]++;
            int pos = re.nextInt(batchSize(node));
            result = tryAction(batchSize * node + pos);
            if (batchHits[node] > batchSize(node) / 2) {
                resolveBatch(node);
            }
        }
        return result;
    }

    private int batch(int action) {
        return action / batchSize;
    }

    private void insertBack(Short action, Double loglik) {
        if (action == null) {
            return;
        }
        double ll = loglik == null ? computeLL.apply((int)action) : loglik;
        int b = batch(action);
        batchMaxLL[b] = Math.max(batchMaxLL[b], (float)ll);
        ll += initialLL;
        double newLL = likelihoodsSum(actions.get(b), ll);
        actions.set(b, (float)newLL);
    }

    private double resolveAction(short action, Double loglik) {
        if (disabledActions.containsKey(action)) {
            return Double.NEGATIVE_INFINITY;
        }
        int b = batch(action);
        double batchLL = Double.NEGATIVE_INFINITY;
        double ll = loglik == null ? computeLL.apply((int)action) + initialLL : loglik;
        if (!cache.isFull() || ll > cache.min() + EPS) {
            Short other = cache.add(action, (float)ll);
            if (other != null) {
                if (batch(other) == b) {
                    batchLL = likelihoodsSum(resolveAction(other, null), batchLL);
                } else {
                    insertBack(other, null);
                }
            }
        } else {
            batchMaxLL[b] = (float)Math.max(ll - initialLL, batchMaxLL[b]);
            batchLL = likelihoodsSum(batchLL, ll);
        }
        return batchLL;
    }

    private void resolveBatch(int b) {
        double batchLL = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < batchSize(b); i++) {
            short action = (short)(i + batchSize * b);
            batchLL = likelihoodsSum(batchLL, resolveAction(action, null));
        }
        actions.set(b, (float)batchLL);
        batchResolved.set(b, true);
        refreshCacheNode();
    }
}

package ctlab.bn.action;

import ctlab.SegmentTree;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Multinomial {
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
    private float[] batchMCFactor;
    private BitSet batchResolved;

    private int batchSize(int batch) {
        if (batch < batchesNum - 1) {
            return batchSize;
        } else {
            return n - ((batchesNum - 1) * batchSize);
        }
    }

    private double likelihoodsSum(double ll1, double ll2) {
        double maxLL = Math.max(ll1, ll2);
        ll1 -= maxLL;
        ll2 -= maxLL;
        return Math.log(Math.exp(ll1) + Math.exp(ll2)) + maxLL;
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
        batchMCFactor = new float[batchesNum];
    }

    public Short tryAction(int pos) {
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
            double c = -Math.log(batchMCFactor[node]) - Math.log(bs);
            while (true) {
                int curr = re.nextInt(bs) + node * batchSize;
                double ll = computeLL.apply(curr);
                if (Math.log(re.nextDouble()) < ll + c) {
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

    private void insertBack(Short action) {
        if (action == null) {
            return;
        }
        double ll = computeLL.apply((int)action);
        int b = batch(action);
        batchMCFactor[b] = Math.max(batchMCFactor[b], (float)Math.exp(ll + Math.log(batchSize(b))));
        double newLL = likelihoodsSum(actions.get(b), ll);
        actions.set(b, (float)newLL);
    }

    private void resolveBatch(int b) {
        double overallLL = Double.NEGATIVE_INFINITY;
        Queue<Integer> processingQ = IntStream.range(0, batchSize(b))
                .mapToObj(x -> x + batchSize * b)
                .collect(Collectors.toCollection(ArrayDeque::new));
        double maxLL = Double.NEGATIVE_INFINITY;
        while(!processingQ.isEmpty()) {
            int action = processingQ.poll();
            double ll = computeLL.apply(action);
            if (!cache.isFull() || ll > cache.min()) {
                Short other = cache.add((short)action, (float)(ll + initialLL));
                if (other != null) {
                    if (batch(other) == b) {
                        processingQ.add((int)other);
                    } else {
                        insertBack(other);
                    }
                }
            }
            overallLL = likelihoodsSum(overallLL, ll);
            maxLL = Math.max(ll, maxLL);
        }
        actions.set(b, (float)(overallLL + initialLL));
        batchMCFactor[b] = (float)Math.exp(maxLL + Math.log(batchSize(b)));
        batchResolved.set(b, true);
        refreshCacheNode();
    }
}

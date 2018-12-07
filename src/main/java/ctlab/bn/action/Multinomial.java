package ctlab.bn.action;

import ctlab.SegmentTree;

import java.util.*;
import java.util.function.Function;

public class Multinomial {
    private int n;
    private int batchSize;
    private int batchesNum;
    private int mainCacheSize;
    private int hits;
    private double initialLL;
    private boolean initialized;

    private Function<Integer, Double> computeLL;

    private SegmentTree actions;
    private HashTable mostLikely;
    private short[] batchHits;
    private float[] batchMCFactor;
    private BitSet batchResolved;
    private Heap topActions;

    private int batch(int k) {
        return k / batchSize;
    }

    private int batchSize(int batch) {
        if (batch < batchesNum - 1) {
            return batchSize;
        } else {
            return n - ((batchesNum - 1) * batchSize);
        }
    }

    private int batchNode(int k) {
        return mainCacheSize + k;
    }

    private double likelihoodsSum(double ll1, double ll2) {
        double maxLL = Math.max(ll1, ll2);
        ll1 -= maxLL;
        ll2 -= maxLL;
        return Math.log(Math.exp(ll1) + Math.exp(ll2)) + maxLL;
    }

    public Multinomial(int maxSize, int batchesNum, int mainCacheSize, Function<Integer, Double> computeLL,
                       double initialLL) {
        n = maxSize;
        this.mainCacheSize = mainCacheSize;
        this.batchesNum = batchesNum;
        this.batchSize = (int)Math.round(Math.ceil((double)n / batchesNum));
        this.computeLL = computeLL;
        this.initialLL = initialLL;
    }

    public double logLikelihood() {
        if (!initialized) {
            return Math.log(n) + initialLL;
        } else {
            return actions.likelihood();
        }
    }

    private void init() {
        actions = new SegmentTree(batchesNum + mainCacheSize);
        batchResolved = new BitSet(batchesNum);
        mostLikely = new HashTable(mainCacheSize);
        batchHits = new short[batchesNum];
        for (int i = 0; i < batchesNum; i++) {
            actions.set(batchNode(i), (float)(initialLL + Math.log(batchSize(i))));
        }
        initialized = true;
        batchMCFactor = new float[batchesNum];
    }

    /*
    private double calculateLLAndMoveToBin(int i) {
        double ll = computeLL.apply(i);
        if (ll > unlikelyThreshold && mainCacheSize > 0) {
            int pos = nodesQueue.peek();
            double necessaryLL = actions.get(likelyNodes[pos]);
            if (ll > necessaryLL) {
                likelyActions.set(likelyNodes[pos], false);
                nodesQueue.poll();
                likelyNodes[pos] = i;
                actions.set(pos, ll);
                nodesQueue.add(pos);
                likelyActions.set(i, true);
            }
        }
        if (ll < unlikelyThreshold) {
            unLikelyActions.set(i, true);
            int b = batch(i);
            int node = unlikelyBatchNode(b);
            double batchLL = actions.get(node);
            actions.set(node, likelihoodsSum(ll, batchLL));
        }
        return ll;
    }

    public boolean isLikely(int i) {
        return likelyActions.get(i);
    }

    public boolean isUnlikely(int i) {
        return unLikelyActions.get(i);
    }

    public double unlikelyThreshold() {
        return unlikelyThreshold;
    }
    */

    public int randomAction(SplittableRandom re) {
        hits++;
        if (!initialized && hits > (batchSize + mainCacheSize) / 2) {
            init();
        }
        /*
        int action = actions.randomChoice(re);
        if (action < mainCacheSize) {
            return likelyNodes[action];
        } else if (action < mainCacheSize + batchesNum) {
            int batch = action - mainCacheSize - batchesNum;
            List<Integer> variants = new ArrayList<>();
            for (int i = batch * batchSize; i < (batch + 1) * batchSize; i++) {
                if (i == n) {
                    break;
                }
                if (unLikelyActions.get(i)) {
                    variants.add(i);
                }
            }
            assert variants.size() != 0;
            SegmentTree batchTree = new SegmentTree(variants.size());
            for (int i = 0; i < variants.size(); i++) {
                batchTree.set(i, calculateLLAndMoveToBin(variants.get(i)));
            }
            return variants.get(batchTree.randomChoice(re));
        } else {

        }
        */
        return 0;
    }
}

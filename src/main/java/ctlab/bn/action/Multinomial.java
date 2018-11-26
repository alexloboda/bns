package ctlab.bn.action;

import ctlab.SegmentTree;

import java.util.*;
import java.util.function.Function;

public class Multinomial {
    private int n;
    private int batchSize;
    private int batchesNum;
    private int mainCacheSize;

    private BitSet unLikelyActions;
    private BitSet likelyActions;
    private double unlikelyThreshold;

    private Function<Integer, Double> computeLL;

    private SegmentTree actions;
    // TODO: get red of it(?)
    private int[] likelyNodes;
    private PriorityQueue<Integer> nodesQueue;

    private int batch(int k) {
        return k / batchSize;
    }

    private int uniformBatchNode(int batch) {
        return mainCacheSize + batch;
    }

    private int unlikelyBatchNode(int batch) {
        return mainCacheSize + batchesNum + batch;
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
        this.batchSize = (int)Math.round(Math.ceil(n / batchesNum));
        unLikelyActions = new BitSet(n);
        likelyActions = new BitSet(n);
        actions = new SegmentTree(mainCacheSize + 2 * batchesNum);
        this.computeLL = computeLL;
        unlikelyThreshold = initialLL - Math.log(batchSize);

        nodesQueue = new PriorityQueue<>(mainCacheSize, Comparator.comparingDouble(i -> actions.get(i)));
        for (int i = 0; i < mainCacheSize; i++) {
            nodesQueue.add(0);
        }

        likelyNodes = new int[mainCacheSize];
        for (int i = 0; i < batchesNum; i++) {
            int size = Math.max(n - i * batchSize + 1, batchSize);
            actions.set(uniformBatchNode(i), size * initialLL);
        }
    }

    public double calculateLLAndMoveToBin(int i) {
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

    public int randomAction(SplittableRandom re) {
        int action = actions.randomChoice(re);
        if (action < mainCacheSize) {
            return likelyNodes[action];
        } else {
            // uniform & rare
            boolean uniform = action < mainCacheSize + batchesNum;
            int batch = action - mainCacheSize;
            if (!uniform) {
                batch -= batchesNum;
            }
            List<Integer> variants = new ArrayList<>();
            for (int i = batch * batchSize; i < (batch + 1) * batchSize; i++) {
                if (i == n) {
                    break;
                }
                if ((uniform && !likelyActions.get(i) && !unLikelyActions.get(i)) ||
                        (!uniform && unLikelyActions.get(i))) {
                    variants.add(i);
                }
            }
            assert variants.size() != 0;
            SegmentTree batchTree = new SegmentTree(variants.size());
            for (int i = 0; i < variants.size(); i++) {
                batchTree.set(i, calculateLLAndMoveToBin(variants.get(i)));
            }

        }
    }
}

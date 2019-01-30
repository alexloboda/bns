package ctlab.bn.action;

import ctlab.SegmentTree;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Multinomial {
    public static final double EPS = 1e-8;

    private int n;
    private int batchSize;
    private int batchesNum;
    private short mainCacheSize;
    private int hits;
    private double initialLL;
    private boolean initialized;

    private double lastLL;

    private Function<Integer, Double> computeLL;
    private SplittableRandom re;

    private SegmentTree actions;
    private Cache cache;

    private short[] batchHits;
    private float[] batchMaxLL;
    private BitSet batchResolved;

    private HashMap<Short, Double> disabledActions;

    private int batchSize(int batch) {
        if (batch < batchesNum - 1) {
            return batchSize;
        } else {
            return n - ((batchesNum - 1) * batchSize);
        }
    }

    public void disableAction(short action, Double ll) {
        disabledActions.put(action, ll);
        if (initialized) {
            if (cache.contains(action)) {
                cache.disable(action);
                refreshCacheNode();
                return;
            }
            int b = batch(action);
            if (batchResolved.get(b)) {
                actions.set(b, likelihoodSubtract(actions.get(b), ll));
            } else {
                actions.set(b, likelihoodSubtract(actions.get(b), initialLL));
            }
        }
    }

    public double getLastLL() {
        return lastLL;
    }

    public void reEnableAction(short action) {
        if (!disabledActions.containsKey(action)) {
            return;
        }

        double ll = disabledActions.get(action);
        disabledActions.remove(action);

        if (!initialized) {
            return;
        }

        if (cache.contains(action)) {
            cache.reEnable(action, ll);
            refreshCacheNode();
        } else {
            int b = batch(action);
            if (batchResolved.get(b)) {
                double batchLL = likelihoodsSum(actions.get(b), resolveAction(action, ll - initialLL));
                actions.set(b, batchLL);
                refreshCacheNode();
            } else {
                actions.set(b, likelihoodsSum(actions.get(b), initialLL));
            }
        }
    }

    public void deactivate() {
        List<Short> toDisable = new ArrayList<>(disabledActions.keySet());
        for (Short action: toDisable) {
            reEnableAction(action);
        }
        disabledActions = new LinkedHashMap<>();
        assert logLikelihood() < 0.1;
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
        if (ll1 < 0 && ll1 > -EPS) {
            return Double.NEGATIVE_INFINITY;
        }
        return Math.log(Math.exp(ll1) - Math.exp(ll2)) + maxLL;
    }

    public Multinomial(int maxSize, int batchSize, int mainCacheSize, Function<Integer, Double> computeLL,
                       double initialLL, SplittableRandom re) {
        n = maxSize;
        this.mainCacheSize = (short)mainCacheSize;
        this.batchesNum = maxSize / batchSize;
        this.batchSize = batchSize;
        if (maxSize % batchSize > 0) {
            ++batchesNum;
        }
        this.computeLL = computeLL;
        this.initialLL = initialLL;
        this.re = re;
        this.disabledActions = new LinkedHashMap<>();
    }

    public double logLikelihood() {
        if (!initialized) {
            return Math.log(n - disabledActions.size()) + initialLL;
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
            actions.set(i, (initialLL + Math.log(batchSize(i))));
        }
        initialized = true;
        batchMaxLL = new float[batchesNum];
        Arrays.fill(batchMaxLL, Float.NEGATIVE_INFINITY);
        List<Pair<Short, Double>> disabled = disabledActions.entrySet().stream()
                .map(x -> new Pair<>(x.getKey(), x.getValue())).collect(Collectors.toList());
        for (Pair<Short, Double> action: disabled) {
            disableAction(action.getFirst(), action.getSecond());
        }
    }

    private Short tryAction(int pos) {
        double ll = computeLL.apply(pos);
        lastLL = ll + initialLL;
        if (Math.log(re.nextDouble()) < ll) {
            return (short)pos;
        } else {
            return null;
        }
    }

    public Short randomAction() {
        hits++;
        if (!initialized) {
            short pos;
            while (true) {
                pos = (short)re.nextInt(n);
                if (!disabledActions.containsKey(pos)) {
                    break;
                }
            }
            Short result = tryAction(pos);
            if (hits > (batchSize + mainCacheSize) / 2) {
                init();
            }
            return result;
        }
        int node = actions.randomChoice(re);
        if (node == batchesNum) {
            Short result = cache.randomAction();
            lastLL = cache.getLastLL();
            return result;
        }
        if (batchResolved.get(node)) {
            int bs = batchSize(node);
            while (true) {
                int curr;
                while (true) {
                    curr = re.nextInt(bs) + node * batchSize;
                    if (!disabledActions.containsKey((short)curr)) {
                        break;
                    }
                }
                if (cache.contains((short)curr)) {
                    continue;
                }
                double ll = computeLL.apply(curr);
                lastLL = ll + initialLL;
                if (Math.log(re.nextDouble()) < ll - batchMaxLL[node]) {
                    return (short)curr;
                }
            }
        } else {
            batchHits[node]++;
            int pos;
            while (true) {
                pos = batchSize * node + re.nextInt(batchSize(node));
                if (!disabledActions.containsKey((short)pos)) {
                    break;
                }
            }

            Short result = tryAction(pos);
            if (batchHits[node] > batchSize(node) / 2) {
                resolveBatch(node);
            }

            return result;
        }
    }

    private int batch(int action) {
        return action / batchSize;
    }

    private void insertBack(Short action) {
        if (action == null || disabledActions.containsKey(action)) {
            return;
        }
        double ll = computeLL.apply((int)action);
        int b = batch(action);
        batchMaxLL[b] = Math.max(batchMaxLL[b], (float)ll);
        ll += initialLL;
        double newLL = likelihoodsSum(actions.get(b), ll);
        actions.set(b, newLL);
    }

    private double resolveAction(short action, Double loglik) {
        if (disabledActions.containsKey(action)) {
            return Double.NEGATIVE_INFINITY;
        }
        int b = batch(action);
        double batchLL = Double.NEGATIVE_INFINITY;
        double ll = (loglik == null ? computeLL.apply((int)action) : loglik) + initialLL;
        if (!cache.isFull() || ll > cache.min() + EPS) {
            Short other = cache.add(action, ll);
            if (other != null) {
                if (batch(other) == b) {
                    batchLL = likelihoodsSum(resolveAction(other, null), batchLL);
                } else {
                    insertBack(other);
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
        actions.set(b, batchLL);
        batchResolved.set(b, true);
        refreshCacheNode();
    }

    public void printDebugInfo(int u) {
        if (!initialized) {
            System.out.println("Structure is not yet initialized");
            for (int i = 0; i < n; i++) {
                int v = i >= u ? i + 1 : i;
                double ll = computeLL.apply(i) + initialLL;
                System.out.println(v + "->" + u + ": " + ll + "(" + Math.exp(ll) + ")");
            }
            return;
        }
        System.out.println("Cache:");
        cache.printDebugInfo(u);
        System.out.println("Regular:");
        for (int i = 0; i < n; i++) {
            if (cache.contains((short)i)) {
                continue;
            }
            int v = i >= u ? i + 1 : i;
            System.out.print(v + "->" + u + ": ");
            if (disabledActions.containsKey(i)) {
                System.out.println("disabled");
                return;
            }
            int b = batch(i);
            System.out.print("batch " + b + (batchResolved.get(b) ? "(resolved)" : "(unresolved)") + " - ");
            double ll = computeLL.apply(i) + initialLL;
            System.out.println(ll + "(" + Math.exp(ll) + ")");
        }
    }
}

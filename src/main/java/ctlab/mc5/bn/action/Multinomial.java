package ctlab.mc5.bn.action;

import ctlab.mc5.algo.SegmentTree;
import ctlab.mc5.bn.BayesianNetwork;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Multinomial {
    public static final double EPS = 1e-8;

    private final int n;
    private final int batchSize;
    private int batchesNum;
    private final short mainCacheSize;
    private int hits;
    private final double initialLL;
    private final double initialLLDel;
    private boolean initialized;
    private final double beta;

    private double lastLL;

    private final Function<Integer, Double> computeLL;
    private final SplittableRandom re;

    private SegmentTree actions;
    private Cache cache;

    private short[] batchHits;
    private float[] batchMaxLL;
    private BitSet batchResolved;

    private HashMap<Short, Double> disabledActions;
    private final BayesianNetwork bn;
    private final int v;

    private double initLL(int action) {
        if (bn.edgeExists(v, action)) {
            return initialLLDel;
        } else {
            return initialLL;
        }
    }

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
                double finalLL = Math.min(beta * ll, 0.0) + initialLL;
                actions.set(b, likelihoodSubtract(actions.get(b), finalLL));
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
                double batchLL = likelihoodsSum(actions.get(b), resolveAction(action, ll));
                actions.set(b, batchLL);
                refreshCacheNode();
            } else {
                actions.set(b, likelihoodsSum(actions.get(b), initialLL));
            }
        }
    }

    public void deactivate() {
        List<Short> toDisable = new ArrayList<>(disabledActions.keySet());
        for (Short action : toDisable) {
            reEnableAction(action);
        }
        disabledActions = new LinkedHashMap<>();
        assert logLikelihood() < 0.1;
    }

    public static double likelihoodsSum(double ll1, double ll2) {
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

    public static double likelihoodSubtract(double ll1, double ll2) {
        double maxLL = Math.max(ll1, ll2);
        ll1 -= maxLL;
        ll2 -= maxLL;
        if (ll1 < 0 && ll1 > -EPS) {
            return Double.NEGATIVE_INFINITY;
        }
        return Math.log(Math.exp(ll1) - Math.exp(ll2)) + maxLL;
    }

    public Multinomial(int maxSize, int batchSize, double beta, int mainCacheSize, Function<Integer, Double> computeLL,
                       double initialLL, SplittableRandom re, BayesianNetwork bn, int v) {
        n = maxSize;
        this.beta = beta;
        this.mainCacheSize = (short) mainCacheSize;
        this.batchesNum = maxSize / batchSize;
        this.batchSize = batchSize;
        if (maxSize % batchSize > 0) {
            ++batchesNum;
        }
        this.computeLL = computeLL;
        this.initialLL = initialLL;
        this.initialLLDel = initialLL / 2;
        this.re = re;
        this.disabledActions = new LinkedHashMap<>();
        this.bn = bn;
        this.v = v;
    }

    public double logLikelihood() {
        if (!initialized) {
            return Math.log(n - disabledActions.size()) + initialLL;
        } else {
            return actions.likelihood();
        }
    }

    private void refreshCacheNode() {
        actions.set(batchesNum, cache.loglikelihood() + initialLL);
    }

    private void init() {
        actions = new SegmentTree(batchesNum + 1);
        batchResolved = new BitSet(batchesNum);
        cache = new HashTableCache(mainCacheSize, re, beta);
        batchHits = new short[batchesNum];
        for (int i = 0; i < batchesNum; i++) {
            actions.set(i, (initialLL + Math.log(batchSize(i))));
        }
        initialized = true;
        batchMaxLL = new float[batchesNum];
        Arrays.fill(batchMaxLL, Float.NEGATIVE_INFINITY);
        List<Pair<Short, Double>> disabled = disabledActions.entrySet().stream()
                .map(x -> new Pair<>(x.getKey(), x.getValue()))
                .collect(Collectors.toList());

        for (Pair<Short, Double> action : disabled) {
            disableAction(action.getFirst(), action.getSecond());
        }
    }

    private Short tryAction(int pos, boolean check) {
        lastLL = computeLL.apply(pos);
        if (!check) {
            return (short) pos;
        }
        double ll = beta * lastLL;
        if (Math.log(re.nextDouble()) < ll) {
            return (short) pos;
        } else {
            return null;
        }
    }

    private Short tryResolvedAction(int curr, boolean check) {
        lastLL = computeLL.apply(curr);
        if (!check) {
            return (short) curr;
        }
        double finalLL = Math.min(beta * lastLL, 0.0) + initLL(curr);
        if (Math.log(re.nextDouble()) < finalLL - batchMaxLL[curr / batchSize]) {
            return (short) curr;
        }
        return null;
    }

    public Short tryAnyAction(int curr) {
        if (!initialized) {
            return tryAction(curr, true);
        }
        if (batchResolved.get(curr / batchSize)) {
            return tryResolvedAction(curr, true);
        } else {
            return tryAction(curr, true);
        }
    }

    public Short randomAction() {
        hits++;
        if (!initialized) {
            short pos;
            do {
                pos = (short) re.nextInt(n);
            } while (disabledActions.containsKey(pos));
            Short result = tryAction(pos, false);
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
                do {
                    curr = re.nextInt(bs) + node * batchSize;
                } while (disabledActions.containsKey((short) curr));
                if (cache.contains((short) curr)) {
                    continue;
                }
                lastLL = computeLL.apply(curr);
                double finalLL = Math.min(beta * lastLL, 0.0) + initLL(curr);
                if (Math.log(re.nextDouble()) < finalLL - batchMaxLL[node]) {
                    return (short) curr;
                }
            }
        } else {
            batchHits[node]++;
            int pos;
            do {
                pos = batchSize * node + re.nextInt(batchSize(node));
            } while (disabledActions.containsKey((short) pos));

            Short result = tryAction(pos, false);
            if (batchHits[node] > batchSize(node) / 2) {
                resolveBatch(node);
            }

            return result;
        }
    }

//
//    public Pair<Short, Boolean> randomActionBias(double reverseBias, BayesianNetwork bn, int to) {
//        hits++;
//        if (!initialized) {
//            short pos;
//            do {
//                pos = (short) re.nextInt(n);
//            } while (disabledActions.containsKey(pos));
//            Short result = tryAction(pos, false);
//            if (hits > (batchSize + mainCacheSize) / 2) {
//                init();
//            }
//            double randVal = Math.log(re.nextDouble());
//            if (bn.edgeExists(pos, to)) {
//                if (randVal < beta * lastLL) {
//                    return new Pair<>((short) pos, false);
//                }
//                if (randVal < beta * lastLL * reverseBias) {
//                    return new Pair<>((short) pos, true);
//                }
//            }
//            if (randVal < beta * lastLL) {
//                return new Pair<>(result, false);
//            } else {
//                return new Pair<>(null, false);
//            }
//        }
//
//        int node = actions.randomChoice(re);
//        if (node == batchesNum) {
//            Short result = cache.randomAction();
//            lastLL = cache.getLastLL();
//            return new Pair<>(result, false);
//        }
//        if (batchResolved.get(node)) {
//            int bs = batchSize(node);
//            while (true) {
//                int curr;
//                do {
//                    curr = re.nextInt(bs) + node * batchSize;
//                } while (disabledActions.containsKey((short) curr));
//                if (cache.contains((short) curr)) {
//                    continue;
//                }
//                lastLL = computeLL.apply(curr);
//                double finalLL = initialLL;
//                double randVal = Math.log(re.nextDouble());
//                if (bn.edgeExists(curr, to)) {
//                    if (randVal < finalLL + Math.min(beta * lastLL, 0.0) - batchMaxLL[node]) {
//                        return new Pair<>((short) curr, false);
//                    }
//                    if (randVal < finalLL + Math.min(beta * lastLL, 0.0) * reverseBias - batchMaxLL[node]) {
//                        return new Pair<>((short) curr, true);
//                    }
//                }
//                if (randVal < finalLL - batchMaxLL[node]) {
//                    return new Pair<>((short) curr, false);
//                }
//            }
//        } else {
//            batchHits[node]++;
//            int pos;
//            do {
//                pos = batchSize * node + re.nextInt(batchSize(node));
//            } while (disabledActions.containsKey((short) pos));
//
//            Short result = tryAction(pos, false);
//            if (batchHits[node] > batchSize(node) / 2) {
//                resolveBatch(node);
//            }
//            double randVal = Math.log(re.nextDouble());
//            if (bn.edgeExists(pos, to)) {
//                if (randVal < beta * lastLL) {
//                    return new Pair<>((short) pos, false);
//                }
//                if (randVal < beta * lastLL * reverseBias) {
//                    return new Pair<>((short) pos, true);
//                }
//            }
//            if (randVal < beta * lastLL) {
//                return new Pair<>(result, false);
//            } else {
//                return new Pair<>(null, false);
//            }
//        }
//    }

    private int batch(int action) {
        return action / batchSize;
    }

    private void insertBack(Short action) {
        if (action == null || disabledActions.containsKey(action)) {
            return;
        }
        double finalLL = Math.min(beta * computeLL.apply((int) action), 0.0) + initLL(action);
        int b = batch(action);
        batchMaxLL[b] = Math.max(batchMaxLL[b], (float) finalLL);
        actions.set(b, likelihoodsSum(actions.get(b), finalLL));
    }

    private double resolveAction(short action, Double loglik) {
        if (disabledActions.containsKey(action)) {
            return Double.NEGATIVE_INFINITY;
        }
        int b = batch(action);
        double batchLL = Double.NEGATIVE_INFINITY;

        if (loglik == null) {
            loglik = computeLL.apply((int) action);
        }
        double finalLL = Math.min(beta * loglik, 0.0) + initLL(action);

        if (!cache.isFull() || loglik > cache.min() + EPS) {
            Short other = cache.add(action, loglik);
            if (other != null) {
                if (batch(other) == b) {
                    batchLL = likelihoodsSum(resolveAction(other, null), batchLL);
                } else {
                    insertBack(other);
                }
            }
        } else {
            batchMaxLL[b] = (float) Math.max(finalLL, batchMaxLL[b]);
            batchLL = likelihoodsSum(batchLL, finalLL);
        }
        return batchLL;
    }

    private void resolveBatch(int b) {
        double batchLL = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < batchSize(b); i++) {
            short action = (short) (i + batchSize * b);
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
            if (cache.contains((short) i)) {
                continue;
            }
            int v = i >= u ? i + 1 : i;
            System.out.print(v + "->" + u + ": ");
            if (disabledActions.containsKey((short) i)) {
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

package ctlab.mc5.bn.action;

import ctlab.mc5.algo.SegmentTree;

import java.util.BitSet;
import java.util.Random;
import java.util.SplittableRandom;

public class HashTableCache implements Cache {
    private SegmentTree addActions;
    private SegmentTree remActions;
    private HashTable topActionNodes;
    private Heap topActionsMin;
    private short[] topActions;
    private SplittableRandom re;
    private double ll;
    private BitSet addRemBitset;

    public HashTableCache(short cacheSize, SplittableRandom re, double beta) {
        if (cacheSize == 0) {
            return;
        }
        this.re = re;
        topActionNodes = new HashTable(cacheSize);
        topActions = new short[cacheSize];
        addActions = new SegmentTree(cacheSize, beta);
        remActions = new SegmentTree(cacheSize, beta);
        topActionsMin = new Heap(cacheSize);
        addRemBitset = new BitSet(cacheSize);
    }

    public double getLastLL() {
        return ll;
    }

    @Override
    public void disable(short action) {
        addActions.set(topActionNodes.get(action), Float.NEGATIVE_INFINITY);
    }

    @Override
    public void reEnable(short action, double ll) {
        addActions.set(topActionNodes.get(action), ll);
    }

    @Override
    public boolean contains(short action) {
        if (topActionNodes == null) {
            return false;
        }
        return topActionNodes.contains(action);
    }

    @Override
    public double loglikelihood() {
        if (topActions == null) {
            return Float.NEGATIVE_INFINITY;
        }
        return Multinomial.likelihoodsSum(addActions.likelihood(), remActions.likelihood());
    }

    @Override
    public double loglikelihoodAdd() {
        if (topActions == null) {
            return Float.NEGATIVE_INFINITY;
        }
        return addActions.likelihood();
    }

    @Override
    public double loglikelihoodRem() {
        if (topActions == null) {
            return Float.NEGATIVE_INFINITY;
        }
        return remActions.likelihood();
    }

    @Override
    public Short randomAction() {
        short node1 = (short) remActions.randomChoice(re);
        double ll1 = remActions.get(node1);
        short node2 = (short) addActions.randomChoice(re);
        double ll2 = addActions.get(node2);
        short node;
        if (Double.isInfinite(ll1)) {
            ll = ll2;
            node = node2;
        } else if (Double.isInfinite(ll2)) {
            ll = ll1;
            node = node1;
        } else {
            Random rand = new Random();
            if (rand.nextDouble() < 1. / 3) {
                ll = ll2;
                node = node2;
            } else {
                ll = ll1;
                node = node1;
            }
        }
        return topActions[node];
    }

    @Override
    public double min() {
        if (topActions == null) {
            return Float.POSITIVE_INFINITY;
        }
        int idx = topActionsMin.min();
        if (addRemBitset.get(idx)) {
            return addActions.get(idx);
        } else {
            return remActions.get(idx);
        }
    }

    @Override
    public Short add(short action, boolean type, double ll) {
        Short ret = null;
        short pos = (short) topActionNodes.size();
        if (topActionNodes.size() == topActions.length) {
            pos = topActionsMin.extractMin();
            ret = topActions[pos];
            topActionNodes.remove(topActions[pos]);
        }
        topActions[pos] = action;
//        type = true;
        if (type) {
            addActions.set(pos, ll);
        } else {
            remActions.set(pos, ll);
        }
        addRemBitset.set(pos, type);
        topActionNodes.put(action, pos);
        topActionsMin.add(pos, ll);
        return ret;
    }

    @Override
    public boolean isFull() {
        if (topActionNodes == null) {
            return true;
        }
        return topActionNodes.size() == topActions.length;
    }

    @Override
    public void printDebugInfo(int u) {
        for (int i = 0; i < topActionNodes.size(); i++) {
            int v = topActions[i];
            int edgeFrom = v >= u ? v + 1 : v;
            if (addRemBitset.get(i)) {
                System.out.println(edgeFrom + "->" + u + ": " + addActions.get(i));
            } else {
                System.out.println(edgeFrom + "->" + u + ": " + remActions.get(i));
            }
        }
    }
}

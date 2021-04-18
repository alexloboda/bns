package ctlab.mc5.bn.action;

import ctlab.mc5.algo.SegmentTree;

import java.util.SplittableRandom;

public class HashTableCache implements Cache {
    private SegmentTree actions;
    private HashTable topActionNodes;
    private Heap topActionsMin;
    private short[] topActions;
    private SplittableRandom re;
    private double ll;

    public HashTableCache(short cacheSize, SplittableRandom re, double beta) {
        if (cacheSize == 0) {
            return;
        }
        this.re = re;
        topActionNodes = new HashTable(cacheSize);
        topActions = new short[cacheSize];
        actions = new SegmentTree(cacheSize, beta);
        topActionsMin = new Heap(cacheSize);
    }

    public double getLastLL() {
        return ll;
    }

    @Override
    public void disable(short action) {
        actions.set(topActionNodes.get(action), Float.NEGATIVE_INFINITY);
    }

    @Override
    public void reEnable(short action, double ll) {
        actions.set(topActionNodes.get(action), ll);
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
        return actions.likelihood();
    }

    @Override
    public Short randomAction() {
        short node = (short)actions.randomChoice(re);
        ll = actions.get(node);
        return topActions[node];
    }

    @Override
    public double min() {
        if (topActions == null) {
            return Float.POSITIVE_INFINITY;
        }
        int idx = topActionsMin.min();
        return actions.get(idx);
    }

    @Override
    public Short add(short action, double ll) {
        Short ret = null;
        short pos = (short) topActionNodes.size();
        if (topActionNodes.size() == topActions.length) {
            pos = topActionsMin.extractMin();
            ret = topActions[pos];
            topActionNodes.remove(topActions[pos]);
        }
        topActions[pos] = action;
        actions.set(pos, ll);
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
            System.out.println(edgeFrom + "->" + u + ": " + actions.get(i));
        }
    }
}

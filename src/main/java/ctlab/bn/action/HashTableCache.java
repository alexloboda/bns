package ctlab.bn.action;

import ctlab.SegmentTree;
import org.apache.commons.math3.util.Pair;

import java.util.Comparator;
import java.util.SplittableRandom;

public class HashTableCache implements Cache {
    private SegmentTree actions;
    private HashTable topActionNodes;
    private Heap topActionsMin;
    private short[] topActions;
    private SplittableRandom re;

    public HashTableCache(short cacheSize, SplittableRandom re) {
        if (cacheSize == 0) {
            return;
        }
        this.re = re;
        topActionNodes = new HashTable(cacheSize);
        topActions = new short[cacheSize];
        actions = new SegmentTree(cacheSize);
        topActionsMin = new Heap(cacheSize);
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
        return topActions[(short)actions.randomChoice(re)];
    }

    @Override
    public double min() {
        if (topActions == null) {
            return Float.POSITIVE_INFINITY;
        }
        return actions.get(topActionsMin.min());
    }

    @Override
    public Short add(short action, double ll) {
        Short ret = null;
        short pos = (short)topActionNodes.size();
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
}

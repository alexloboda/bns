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
        this.re = re;
        topActionNodes = new HashTable(cacheSize);
        topActions = new short[cacheSize];
        actions = new SegmentTree(cacheSize);
        topActionsMin = new Heap(cacheSize, Comparator.comparingDouble(k -> actions.get(k)));
    }

    @Override
    public boolean contains(short action) {
        return topActionNodes.contains(action);
    }

    @Override
    public float loglikelihood() {
        if (topActions.length == 0) {
            return Float.NEGATIVE_INFINITY;
        }
        return actions.likelihood();
    }

    @Override
    public Pair<Short, Double> randomAction() {
        return topActions[(short)actions.randomChoice(re)];
    }

    @Override
    public float min() {
        if (topActions.length == 0) {
            return Float.POSITIVE_INFINITY;
        }
        return actions.get(topActionsMin.min());
    }

    @Override
    public Short add(short action, float ll) {
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
        topActionsMin.add(pos);
        return ret;
    }

    @Override
    public boolean isFull() {
        return topActionNodes.size() == topActions.length;
    }
}

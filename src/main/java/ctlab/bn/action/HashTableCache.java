package ctlab.bn.action;

import ctlab.SegmentTree;

import java.util.Comparator;

public class HashTableCache implements Cache {
    SegmentTree actions;
    private HashTable topActionNodes;
    private Heap topActionsMin;
    private short[] topActions;

    public HashTableCache(short cacheSize) {
        topActionNodes = new HashTable(cacheSize);
        topActionsMin = new Heap(cacheSize, Comparator.comparingDouble(x -> ll.apply(topActionNodes.get(x))));
        topActions = new short[cacheSize];
        actions = new SegmentTree(cacheSize);
    }
}

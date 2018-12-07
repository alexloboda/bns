package ctlab.bn.action;

import java.util.Comparator;
import java.util.function.Function;

public class HashTableCache extends Cache {
    private HashTable topActionNodes;
    private Heap topActionsMin;
    private short[] topActions;

    public HashTableCache(Function<Short, Float> ll, short cacheSize) {
        super(ll);
        topActionNodes = new HashTable(cacheSize);
        topActionsMin = new Heap(cacheSize, Comparator.comparingDouble(x -> ll.apply(topActionNodes.get(x))));
        topActions = new short[cacheSize];
    }

    @Override
    public short getActionByNode(short node) {
        return topActions[node];
    }
}

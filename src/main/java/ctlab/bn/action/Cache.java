package ctlab.bn.action;

import java.util.function.Function;

public abstract class Cache {
    private Function<Short, Float> ll;

    public Cache(Function<Short, Float> ll) {
        this.ll = ll;
    }

    private float ll(short k) {
        return ll.apply(k);
    }

    public abstract short getActionByNode(short node);
}

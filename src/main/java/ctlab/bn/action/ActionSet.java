package ctlab.bn.action;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ActionSet {
    public static double EPS = 1e-12;
    public static double logEPS = Math.log(EPS);

    private int n;
    private int batchSize;

    private BitSet unLikelyActions;
    private BitSet likelyActions;

    public ActionSet(int maxSize, int batchSize) {
        n = maxSize;
        this.batchSize = batchSize;
        unLikelyActions = new BitSet(n);
        likelyActions = new BitSet(n);
    }


}

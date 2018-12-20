package ctlab.bn.action;

import java.util.SplittableRandom;
import java.util.function.Function;

public class MultinomialFactory {
    private final int maxSize;
    private final int batchesNum;
    private final short mainCacheSize;
    private final SplittableRandom re;

    public MultinomialFactory(int maxSize, int batchesNum, int mainCacheSize, SplittableRandom re) {
        this.maxSize = maxSize;
        this.batchesNum = batchesNum;
        this.mainCacheSize = (short)mainCacheSize;
        this.re = re;
    }

    public Multinomial spark(Function<Integer, Double> computeLL, double initialLL) {
        return new Multinomial(maxSize, batchesNum, mainCacheSize, computeLL, initialLL, re);
    }
}

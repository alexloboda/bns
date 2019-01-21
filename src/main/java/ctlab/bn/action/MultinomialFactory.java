package ctlab.bn.action;

import java.util.SplittableRandom;
import java.util.function.Function;

public class MultinomialFactory {
    private final int maxSize;
    private final int batchSize;
    private final short mainCacheSize;
    private final SplittableRandom re;

    public MultinomialFactory(int maxSize, int batchSize, int mainCacheSize, SplittableRandom re) {
        this.maxSize = maxSize;
        this.batchSize = batchSize;
        this.mainCacheSize = (short)mainCacheSize;
        this.re = re;
    }

    public Multinomial spark(Function<Integer, Double> computeLL, double initialLL) {
        return new Multinomial(maxSize, batchSize, mainCacheSize, computeLL, initialLL, re);
    }
}

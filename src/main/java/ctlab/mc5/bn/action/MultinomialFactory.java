package ctlab.mc5.bn.action;

import ctlab.mc5.bn.BayesianNetwork;

import java.util.SplittableRandom;
import java.util.function.Function;

public class MultinomialFactory {
    private final int batchSize;
    private final short mainCacheSize;
    private SplittableRandom re;

    public MultinomialFactory(int batchSize, int mainCacheSize) {
        this.batchSize = batchSize;
        this.mainCacheSize = (short)mainCacheSize;
    }

    public void setRandomEngine(SplittableRandom re) {
        this.re = re;
    }

    public Multinomial spark(int n, Function<Integer, Double> computeLL, double initialLL, double beta) {
        return new Multinomial(n, batchSize, beta, mainCacheSize, computeLL, initialLL, re);
    }
}

package ctlab.mcmc;

public interface EstimatorParams {
    int nThreads();

    int chains();
    int numberOfCachedStates();
    int batchSize();
    int mainCacheSize();

    long coldChainSteps();
    int powerBase();
    double deltaT();
    long swapPeriod();
}

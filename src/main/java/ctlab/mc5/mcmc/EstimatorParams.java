package ctlab.mc5.mcmc;

public interface EstimatorParams {
    int nThreads();
    int nRuns();

    int chains();
    int numberOfCachedStates();
    int batchSize();
    int mainCacheSize();

    long coldChainSteps();
    int powerBase();
    double deltaT();
    long swapPeriod();
}

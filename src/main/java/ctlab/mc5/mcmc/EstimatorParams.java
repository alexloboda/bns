package ctlab.mc5.mcmc;

import picocli.CommandLine.Option;

public interface EstimatorParams {
    @Option(names = {"-m", "--threads"}, defaultValue = "1")
    int nThreads();
    @Option(names = {"-r", "--runs"}, defaultValue = "100")
    int nRuns();

    @Option(names = {"-c", "--chains"}, defaultValue = "5")
    int chains();
    @Option(names = "--cached-states", defaultValue = "200")
    int numberOfCachedStates();
    @Option(names = "--batch-size", defaultValue = "100")
    int batchSize();
    @Option(names = "--cache-size", defaultValue = "25")
    int mainCacheSize();

    @Option(names = {"-s", "--steps"})
    long coldChainSteps();
    @Option(names = "--steps-power-base", defaultValue = "2.0")
    double powerBase();
    @Option(names = {"-d", "--temperature-delta"})
    double deltaT();
    @Option(names = "--swap-period")
    long swapPeriod();
}

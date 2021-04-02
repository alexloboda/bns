package ctlab.mc5.mcmc;

import picocli.CommandLine.Option;

public interface EstimatorParams {
    @Option(names = {"-m", "--threads"}, defaultValue = "8")
    int nThreads();
    @Option(names = {"-r", "--runs"}, defaultValue = "10")
    int nRuns();

    @Option(names = {"-c", "--chains"}, defaultValue = "5")
    int chains();
    @Option(names = "--cached-states", defaultValue = "200")
    int numberOfCachedStates();
    @Option(names = "--batch-size", defaultValue = "100")
    int batchSize();
    @Option(names = "--cache-size", defaultValue = "25")
    int mainCacheSize();
    @Option(names = {"-s", "--steps"}, defaultValue = "2000")
    long coldChainSteps();
    @Option(names = "--steps-power-base", defaultValue = "1.0")
    double powerBase();
    @Option(names = {"-d", "--temperature-delta"})
    double deltaT();
    @Option(names = "--swap-period", defaultValue = "1300")
    long swapPeriod();
}

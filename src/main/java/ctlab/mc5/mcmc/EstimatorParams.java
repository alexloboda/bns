package ctlab.mc5.mcmc;

import picocli.CommandLine.Option;

public interface EstimatorParams {
    @Option(names = {"-m", "--threads"}, defaultValue = "8")
    int nThreads();
    @Option(names = {"-r", "--runs"}, defaultValue = "10")
    int nRuns();
    @Option(names = {"-c", "--chains"}, defaultValue = "1")
    int chains();
    @Option(names = "--cached-states", defaultValue = "200")
    int numberOfCachedStates();
    @Option(names = "--batch-size", defaultValue = "10")
    int batchSize();
    @Option(names = "--cache-size", defaultValue = "10")
    int mainCacheSize();
    @Option(names = {"-s", "--steps"}, defaultValue = "2000")
    long coldChainSteps();
    @Option(names = "--steps-power-base", defaultValue = "1.0")
    double powerBase();
    @Option(names = {"-d", "--temperature-delta"}, defaultValue = "0.0")
    double deltaT();
    @Option(names = "--swap-period", defaultValue = "10000")
    long swapPeriod();
    @Option(names = {"-warmup", "-w"}, defaultValue = "0")
    long warmup();
    @Option(names = {"-mc"}, defaultValue = "0")
    int multipleCollectors();
}

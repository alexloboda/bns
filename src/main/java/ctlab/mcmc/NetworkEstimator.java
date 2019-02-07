package ctlab.mcmc;

import ctlab.bn.BayesianNetwork;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkEstimator {
    private EstimatorParams params;

    public NetworkEstimator(EstimatorParams params) {
        this.params = params;
    }

    public void run(BayesianNetwork bn) {
        ExecutorService es = Executors.newFixedThreadPool(params.nThreads());
    }
}

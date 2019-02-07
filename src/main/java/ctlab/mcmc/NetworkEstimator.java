package ctlab.mcmc;

import ctlab.bn.BayesianNetwork;
import ctlab.bn.action.MultinomialFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkEstimator {
    private EstimatorParams params;
    private SplittableRandom re;
    private EdgeList result;

    public NetworkEstimator(EstimatorParams params, SplittableRandom re) {
        this.params = params;
        this.re = re;
        result = new EdgeList();
    }

    public void run(BayesianNetwork bn) {
        ExecutorService es = Executors.newFixedThreadPool(params.nThreads());

    }

    private class Task implements Runnable {
        private MetaModel model;
        private EdgeList result;

        public Task(BayesianNetwork bn) {
            SplittableRandom random = re.split();
            List<Model> models = new ArrayList<>();
            for (int i = 0; i < params.chains(); i++) {
                MultinomialFactory mults = new MultinomialFactory(bn.size() - 1, params.batchSize(),
                        params.mainCacheSize(), random);
                Model model = new Model(bn, mults, params.numberOfCachedStates(), 1.0 - params.deltaT() * i);
                models.add(model);
            }
            model = new MetaModel(models, random);
        }

        @Override
        public void run() {
            result = model.run(params.swapPeriod(), params.coldChainSteps(), params.powerBase());
        }

        public EdgeList getResult() {
            return result;
        }
    }
}

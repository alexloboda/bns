package ctlab.mc5.mcmc;

import ctlab.mc5.bn.BayesianNetwork;
import ctlab.mc5.bn.action.MultinomialFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkEstimator {
    private EstimatorParams params;
    private SplittableRandom re;

    private List<Task> tasks;

    public NetworkEstimator(EstimatorParams params, SplittableRandom re) {
        this.params = params;
        this.re = re;
    }

    public EdgeList resultsFromCompletedTasks() {
        EdgeList result = new EdgeList(0);
        for (final Task t : tasks) {
            synchronized (t) {
                EdgeList res = t.getResult();
                if (res != null) {
                    result.merge(res);
                }
            }
        }
        return result;
    }

    static class Int {
        int count = 0;

        public void inc() {
            count++;
        }
    }

    public void run(BayesianNetwork bn) {
        ExecutorService es = Executors.newFixedThreadPool(params.nThreads());
        tasks = new ArrayList<>();
        Int model_counter = new Int();
        for (int i = 0; i < params.nRuns(); i++) {
            Task task = new Task(bn, model_counter);
            tasks.add(task);
            es.submit(task);
        }
        es.shutdown();

        try {
            es.awaitTermination(1_000_000, TimeUnit.HOURS);
        } catch (InterruptedException ignored) {
        }
    }

    private class Task implements Runnable {
        private MetaModel model;
        private EdgeList result;
        private final BayesianNetwork bn;
        private final Int mc;

        public Task(BayesianNetwork bn, Int mc) {
            this.bn = bn;
            this.mc = mc;
        }

        private void init() {
            SplittableRandom random = re.split();
            int chains = params.chains();
            List<Model> models = new ArrayList<>(chains);
            for (int i = 0; i < chains; i++) {
                MultinomialFactory mults = new MultinomialFactory(params.batchSize(), params.mainCacheSize());
                Model model = new Model(bn, mults, params.numberOfCachedStates(), 1.0, params.multipleCollectors() == 0);
                model.init(false);
                models.add(model);
            }
            model = new MetaModel(models, random, mc);
            result = null;
        }

        @Override
        public void run() {
            init();
            EdgeList result;
            try {
                result = model.run(params.swapPeriod(), params.coldChainSteps(), params.warmup(), params.powerBase());
            } catch (InterruptedException e) {
                return;
            } catch (Error | Exception e) {
                System.err.println("Exception occurred: ");
                System.err.println(e);
                e.printStackTrace();
                return;
            }
            synchronized (this) {
                this.result = result;
            }
        }

        public EdgeList getResult() {
            return result;
        }
    }
}

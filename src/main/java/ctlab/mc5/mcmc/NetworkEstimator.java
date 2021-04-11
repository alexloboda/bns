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

    public EdgeList resultsFromCompletedTasks(){
        EdgeList result = new EdgeList();
        for (final Task t: tasks) {
            synchronized (t) {
                EdgeList res = t.getResult();
                if (res != null) {
                    result.merge(res);
                }
            }
        }
        return result;
    }

    public void run(BayesianNetwork bn) {
        ExecutorService es = Executors.newFixedThreadPool(params.nThreads());
        tasks = new ArrayList<>();
        for (int i = 0; i < params.nRuns(); i++) {
            Task task = new Task(bn);
            tasks.add(task);
            es.submit(task);
        }
        es.shutdown();

        try {
            es.awaitTermination(1_000_000, TimeUnit.HOURS);
        } catch (InterruptedException ignored) {}
    }

    private class Task implements Runnable {
        private MetaModel model;
        private EdgeList result;

        public Task(BayesianNetwork bn) {
            SplittableRandom random = re.split();
            List<Model> models = new ArrayList<>();
            for (int i = 0; i < params.chains(); i++) {
                MultinomialFactory mults = new MultinomialFactory(params.batchSize(), params.mainCacheSize());
                Model model = new Model(bn, mults, params.numberOfCachedStates(), 1.0 - params.deltaT() * i);
                model.init(false);
                models.add(model);
            }
            model = new MetaModel(models, random);
            result = null;
        }

        @Override
        public void run() {
            EdgeList result;
            try {
                result = model.run(params.swapPeriod(), params.coldChainSteps(), params.powerBase());
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

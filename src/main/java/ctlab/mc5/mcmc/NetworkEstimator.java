package ctlab.mc5.mcmc;

import ctlab.mc5.bn.BayesianNetwork;
import ctlab.mc5.bn.Variable;
import ctlab.mc5.bn.action.MultinomialFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NetworkEstimator {
    private final EstimatorParams params;
    private final SplittableRandom re;
    private final List<List<Variable>> tf;

    private List<Task> tasks;

    public NetworkEstimator(EstimatorParams params, SplittableRandom re, List<List<Variable>> tf) {
        this.params = params;
        this.re = re;
        this.tf = tf;
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

    public static class Int {
        public AtomicLong count = new AtomicLong(0);

        public long inc() {
            return count.incrementAndGet();
        }
    }

    public void run(BayesianNetwork bn) {
        ExecutorService es = Executors.newFixedThreadPool(params.nThreads());
        tasks = new ArrayList<>();
        Int model_counter = new Int();
        for (int i = 0; i < params.nRuns(); i++) {
            Task task = new Task(bn, model_counter, re, params, tf);
            tasks.add(task);
            es.submit(task);
        }
        es.shutdown();

        try {
            es.awaitTermination(1_000_000, TimeUnit.HOURS);
        } catch (InterruptedException ignored) {
        }
    }

    public static class Task implements Runnable {
        private MetaModel model;
        private EdgeList result;
        private BayesianNetwork bn;
        private SplittableRandom re;
        private Int mc;
        private final int chains;
        private final int batchSize;
        private final int mainCacheSize;
        private final int numberOfCachedStates;
        private final int multipleCollectors;
        private final long swapPeriod;
        private final long coldChainSteps;
        private final long warmup;
        private final double powerBase;
        private final List<List<Variable>> tf;

        public Task(BayesianNetwork bn, Int mc, SplittableRandom rn, EstimatorParams params, List<List<Variable>> tf) {
            this(bn, mc, rn, params.chains(), params.batchSize(),
                    params.mainCacheSize(), params.numberOfCachedStates(),
                    params.multipleCollectors(), params.swapPeriod(),
                    params.coldChainSteps(), params.warmup(),
                    params.powerBase(), tf);
        }

        public Task(BayesianNetwork bn, Int mc, SplittableRandom rn,
                    int chains, int batchSize, int mainCacheSize, int numberOfCachedStates,
                    int multipleCollectors, long swapPeriod, long coldChainSteps, long warmup, double powerBase, List<List<Variable>> tf) {
            this.bn = bn;
            this.mc = Objects.requireNonNullElseGet(mc, Int::new);
            this.re = rn;
            this.chains = chains;
            this.batchSize = batchSize;
            this.mainCacheSize = mainCacheSize;
            this.numberOfCachedStates = numberOfCachedStates;
            this.multipleCollectors = multipleCollectors;
            this.swapPeriod = swapPeriod;
            this.coldChainSteps = coldChainSteps;
            this.warmup = warmup;
            this.powerBase = powerBase;
            this.tf = tf;
        }

        private void init() {
            SplittableRandom random = re.split();
            List<Model> models = new ArrayList<>(chains);
            for (int i = 0; i < chains; i++) {
                MultinomialFactory mults = new MultinomialFactory(batchSize, mainCacheSize);
                Model model = new Model(bn, mults, numberOfCachedStates, 1.0, multipleCollectors == 0, tf);
                model.init(false, true);
                models.add(model);
            }
            model = new MetaModel(models, random, mc);
            result = null;
        }

        private void unInit() {
            model = null;
            bn = null;
            mc = null;
            re = null;
            System.gc();
        }


        @Override
        public void run() {
            EdgeList result;
            try {
                init();
                result = model.run(swapPeriod, coldChainSteps, warmup, powerBase);
            } catch (InterruptedException e) {
                return;
            } catch (Error | Exception e) {
                System.err.println("Exception occurred: ");
                System.err.println(e);
                e.printStackTrace();
                return;
            } finally {
                unInit();
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

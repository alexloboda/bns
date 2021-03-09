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
//    private EstimatorParams params;
//    private SplittableRandom re;
//
//    public NetworkEstimator(EstimatorParams params, SplittableRandom re) {
//        this.params = params;
//        this.re = re;
//    }
//
//    public EdgeList resultsFromCompletedTasks() {
//        EdgeList result = new EdgeList();
//        for (Task t : tasks) {
//            synchronized (t) {
//                EdgeList res = t.getResult();
//                if (res != null) {
//                    result.merge(res);
//                }
//            }
//        }
//        return result;
//    }
//
//    public EstimatorParams getParams() {
//        return params;
//    }
//
//    public List<Task> genList(BayesianNetwork bn) {
//        tasks = new ArrayList<>();
//        for (int i = 0; i < params.nRuns(); i++) {
//            Task task = new Task(bn);
//            tasks.add(task);
//        }
//        return tasks;
//    }
//
//    public void run(BayesianNetwork bn) {
//        ExecutorService es = Executors.newFixedThreadPool(params.nThreads());
//        tasks = new ArrayList<>();
//        for (int i = 0; i < params.nRuns(); i++) {
//            Task task = new Task(bn);
//            tasks.add(task);
//            es.submit(task);
//        }
//        es.shutdown();
//
//        try {
//            es.awaitTermination(1_000_000, TimeUnit.HOURS);
//        } catch (InterruptedException ignored) {
//        }
//    }


}

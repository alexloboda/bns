package ctlab.mc5.mcmc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

public class MetaModel {
    private List<Model> models;
    private SplittableRandom random;

    private EdgeList finalAnswer;

    public MetaModel(List<Model> models, SplittableRandom random) {
        this.models = new ArrayList<>(models);
        this.models.sort(Comparator.comparingDouble(Model::beta));
        this.random = random;
        this.finalAnswer = new EdgeList(0);
    }

    public EdgeList run(long swapPeriod, long coldChainSteps, double powerBase) throws InterruptedException {
        long targetSteps = 0;

        final long stepCollect = 1000;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            targetSteps += swapPeriod;
            targetSteps = Math.min(targetSteps, coldChainSteps);

            for (int i = 0; i < models.size(); i++) {
                long currentTarget = (long)(targetSteps / Math.pow(powerBase, i));
                long currentSteps = models.get(i).getSteps();
                while (!models.get(i).step(currentTarget)) {
                    for (long cur = currentSteps; cur < models.get(i).getSteps(); cur += stepCollect) {
                        finalAnswer.merge(models.get(i).edgeList());
                    }
                }
            }


            if (targetSteps == coldChainSteps) {
                if (Math.abs(models.get(0).computeLogLikelihood() - models.get(0).logLikelihood()) >= 0.1) {
                    System.err.println(models.get(0).computeLogLikelihood());
                    System.err.println(models.get(0).logLikelihood());
                    System.err.println("lls dont match");
                }
                System.out.println("Iteration finished");
                return finalAnswer;
            }
            if (models.size() > 1) {
                int i = random.nextInt(models.size());
                int j = random.nextInt(models.size() - 1);
                if (j >= i) {
                    ++j;
                }

                double iLL = models.get(i).logLikelihood();
                double jLL = models.get(j).logLikelihood();
                double iBeta = models.get(i).beta();
                double jBeta = models.get(j).beta();

                double acceptLL = iBeta * (jLL - iLL) + jBeta * (iLL - jLL);
                if (Math.log(random.nextDouble()) < acceptLL) {
                    Model.swapNetworks(models.get(i), models.get(j));
//                    Model.swapNetworks(models.get(j), models.get(i));
                }
            }
        }
    }
}

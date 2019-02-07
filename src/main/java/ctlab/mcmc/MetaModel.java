package ctlab.mcmc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

public class MetaModel {
    private List<Model> models;
    private SplittableRandom random;

    public MetaModel(List<Model> models, SplittableRandom random) {
        models = new ArrayList<>(models);
        models.sort(Comparator.comparingDouble(Model::beta));
        this.random = random;
    }

    public void run(long swapPeriod, long coldChainSteps, double powerBase) {
        long targetSteps = 0;
        while (true) {
            targetSteps += swapPeriod;
            targetSteps = Math.min(targetSteps, coldChainSteps);

            for (int i = 0; i < models.size(); i++) {
                long currentTarget = (long)(targetSteps / Math.pow(powerBase, i));
                while (!models.get(i).step(currentTarget)) {}
            }

            if (targetSteps == coldChainSteps) {
                return;
            }

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
            }
        }
    }
}
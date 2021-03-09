package ctlab.mc5.mcmc;

import ctlab.mc5.bn.BayesianNetwork;
import ctlab.mc5.bn.action.MultinomialFactory;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private final MetaModel model;
    long swapPeriod;
    long coldChainSteps;
    double powerBase;

    public Task(BayesianNetwork bn,
                int chains,
                int batchSize,
                int mainCacheSize,
                int numberOfCachedStates,
                double deltaT,
                long swapPeriod,
                long coldChainSteps,
                double powerBase) {
        this.swapPeriod = swapPeriod;
        this.coldChainSteps = coldChainSteps;
        this.powerBase = powerBase;
        List<Model> models = new ArrayList<>();
        for (int i = 0; i < chains; i++) {
            MultinomialFactory mults = new MultinomialFactory(batchSize, mainCacheSize);
            Model model = new Model(bn, mults, numberOfCachedStates, 1.0 - deltaT * i);
            model.init(false);
            models.add(model);
        }
        model = new MetaModel(models);
    }

    public EdgeList run() {
        return model.run(swapPeriod, coldChainSteps, powerBase);
    }

}
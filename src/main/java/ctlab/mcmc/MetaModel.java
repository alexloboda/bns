package ctlab.mcmc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MetaModel {
    private List<Model> models;

    public MetaModel(List<Model> models) {
        models = new ArrayList<>(models);
        models.sort(Comparator.comparingDouble(Model::beta));
    }

    public void run(double swapP) {

    }
}

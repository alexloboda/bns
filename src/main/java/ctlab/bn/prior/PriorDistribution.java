package ctlab.bn.prior;

public interface PriorDistribution {
    void insert(int v, int u);
    void remove(int v, int u);
    double value();
    PriorDistribution clone();
}

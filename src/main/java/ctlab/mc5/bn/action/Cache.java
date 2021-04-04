package ctlab.mc5.bn.action;

public interface Cache {
    void disable(short action);
    void reEnable(short action, double ll);
    boolean contains(short action);
    double loglikelihood();
    double loglikelihoodAdd();
    double loglikelihoodRem();
    Short randomAction();
    double min();
    double getLastLL();
    Short add(short action, boolean type, double ll);
    boolean isFull();

    void printDebugInfo(int u);
}

package ctlab.bn.action;

public interface Cache {
    void disable(short action);
    void reEnable(short action, double ll);
    boolean contains(short action);
    double loglikelihood();
    Short randomAction();
    double min();
    Short add(short action, double ll);
    boolean isFull();
}

package ctlab.bn.action;

public interface Cache {
    void disable(short action);
    void reEnable(short action, float ll);
    boolean contains(short action);
    float loglikelihood();
    Short randomAction();
    float min();
    Short add(short action, float ll);
    boolean isFull();
}

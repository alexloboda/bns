package ctlab.bn.action;

public interface Cache {
    boolean contains(short action);
    float loglikelihood();
    Short randomAction();
    boolean isFull();
    float min();
    Short add(short action);
}

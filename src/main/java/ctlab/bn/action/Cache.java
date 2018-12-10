package ctlab.bn.action;

public interface Cache {
    boolean contains(short action);
    float loglikelihood();
    Short randomAction();
    float min();
    Short add(short action, float ll);
}

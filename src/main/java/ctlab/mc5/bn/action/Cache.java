package ctlab.mc5.bn.action;

public interface Cache {
    void disable(short action);
    void reEnable(short action, double ll);
    boolean contains(short action);
    double loglikelihood();
    Short randomAction();
    double min();
    double getLastLL();

    class RetAdd {
        public short action;
        public double ll;

        public RetAdd(Short ret, double lls) {
            action = ret;
            ll = lls;
        }
    }

    RetAdd add(short action, double ll);
    boolean isFull();

    void printDebugInfo(int u);
}

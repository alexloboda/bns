package ctlab.bn.action;

public class Action implements Comparable<Action> {
    private ActionType actionType;
    private int v;
    private int u;
    private double loglik;

    public Action(ActionType actionType, int v, int u, double loglik) {
        this.actionType = actionType;
        this.v = v;
        this.u = u;
        this.loglik = loglik;
    }

    public int v() {
        return v;
    }

    public int u() {
        return u;
    }

    public ActionType action() {
        return actionType;
    }

    public double loglik() {
        return loglik;
    }

    @Override
    public int compareTo(Action o) {
        return Double.compare(loglik, o.loglik);
    }
}

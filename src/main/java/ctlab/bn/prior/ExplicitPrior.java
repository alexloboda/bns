package ctlab.bn.prior;

import ctlab.graph.Graph;

public class ExplicitPrior implements PriorDistribution {
    private int loglik;
    private double[][] priors;
    private Graph g;

    public ExplicitPrior(double[][] priors) {
        g = new Graph(priors.length);
        this.priors = priors;
    }

    public ExplicitPrior(ExplicitPrior p) {
        loglik = p.loglik;
        priors = p.priors;
        g = new Graph(p.g);
    }

    @Override
    public void insert(int v, int u) {
        g.add_edge(v, u);
        loglik += priors[v][u];
    }

    @Override
    public void remove(int v, int u) {
        g.remove_edge(v, u);
        loglik -= priors[v][u];
    }

    @Override
    public double value() {
        return loglik;
    }

    @Override
    public PriorDistribution clone() {
        return new ExplicitPrior(this);
    }
}

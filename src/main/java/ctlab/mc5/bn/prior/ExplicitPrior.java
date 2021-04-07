package ctlab.mc5.bn.prior;

import ctlab.mc5.graph.Graph;

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
    public void insert(int from, int to) {
        g.addEdge(from, to);
        loglik += priors[from][to];
    }

    @Override
    public void remove(int from, int to) {
        g.removeEdge(from, to);
        loglik -= priors[from][to];
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

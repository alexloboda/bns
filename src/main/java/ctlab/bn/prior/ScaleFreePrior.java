package ctlab.bn.prior;

import ctlab.bn.Graph;

import java.util.Arrays;

public class ScaleFreePrior implements PriorDistribution {
    private Graph graph;
    private double[] ps;
    private int[] occ;
    private double loglik;

    public ScaleFreePrior(int n, double gamma) {
        graph = new Graph(n);
        ps = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            ps[i] = Math.pow(i + 1, -gamma);
            sum += ps[i];
        }
        double finalSum = sum;
        ps = Arrays.stream(ps).map(x -> Math.log(x / finalSum)).toArray();
        occ = new int[n];
        occ[0] = n;
        loglik = n * ps[0];
    }

    public ScaleFreePrior(ScaleFreePrior pd) {
        loglik = pd.loglik;
        ps = pd.ps.clone();
        occ = pd.occ.clone();
        graph = new Graph(pd.graph);
    }

    private void change(int k, int change) {
         if (change == 1) {
             loglik -= Math.log(occ[k] + 1);
         } else {
             loglik += Math.log(occ[k]);
         }
         occ[k] += change;
         loglik += change * ps[k];
    }

    public void remove(int v, int u) {
        int k = graph.out_degree(v);
        graph.remove_edge(v, u);
        change(k, -1);
        change(k - 1, 1);
    }

    public void insert(int v, int u) {
        int k = graph.out_degree(v);
        graph.add_edge(v, u);
        change(k, -1);
        change(k + 1, 1);
    }

    public double value() {
        return 0.0;
    }

    @Override
    public PriorDistribution clone() {
        return new ScaleFreePrior(this);
    }
}

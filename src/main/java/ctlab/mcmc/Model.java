package ctlab.mcmc;

import ctlab.bn.BayesianNetwork;
import ctlab.bn.K2ScoringFunction;

import java.util.Random;

import static java.lang.Math.log;

public class Model {
    private static final double LADD = log(0.5);
    private static final double LOTH = log(2.0);

    private int n;
    private int[][] hits;
    private int[][] time;
    private double loglik;
    private BayesianNetwork bn;
    private K2ScoringFunction sf;
    private int disc_steps;
    private int steps;
    private Random random;

    public Model(BayesianNetwork bn, int disc_steps) {
        n = bn.size();
        hits = new int[n][n];
        this.bn = bn;
        random = new Random();
        time = new int[n][n];
        bn.discretize(disc_steps);
        sf = new K2ScoringFunction();
        loglik = bn.score(sf);
        this.disc_steps = disc_steps;
    }

    public void step(boolean warming_up) {
        if (!warming_up) {
            steps++;
        }
        int v = 0;
        int u = 0;
        while (v == u) {
            v = random.nextInt(n);
            u = random.nextInt(n);
        }
        if (bn.edge_exists(v, u)) {
            if (random.nextBoolean()) {
                try_remove(v, u);
            } else {
                try_inverse(v, u);
            }
        } else {
            try_add(v, u);
        }
    }

    public int[][] hits() {
        return hits;
    }

    private void try_inverse(int v, int u) {
        bn.remove_edge(v, u);
        if (bn.path_exists(v, u)) {
            bn.add_edge(v, u);
            return;
        }
        bn.add_edge(u, v);
        double score = score();
        double log_accept = score - loglik;
        if (log(random.nextDouble()) < log_accept) {
            time[u][v] = steps;
            fix_edge_deletion(v, u);
            loglik = score;
        } else {
            bn.remove_edge(u, v);
            bn.add_edge(v, u);
        }
    }

    private void fix_edge_deletion(int v, int u) {
        hits[v][u] += steps - time[v][u];
    }

    private double score() {
        bn.discretize(disc_steps);
        return bn.score(sf);
    }

    private void try_remove(int v, int u) {
        bn.remove_edge(v, u);
        double score = score();
        double log_accept = score - loglik + LOTH;
        if (log(random.nextDouble()) < log_accept) {
            fix_edge_deletion(v, u);
            loglik = score;
        } else {
            bn.add_edge(v, u);
        }
    }

    private void try_add(int v, int u){
       if (bn.path_exists(u, v)) {
           return;
       }
       bn.add_edge(v, u);
       double score = score();
       double log_accept = score - loglik + LADD;
       if (log(random.nextDouble()) < log_accept) {
           time[v][u] = steps;
           loglik = score;
       } else {
           bn.remove_edge(v, u);
       }
   }

    public void finish() {
        for (int u = 0; u < n; u++) {
            for (int v : bn.ingoing_edges(u)) {
                bn.remove_edge(v, u);
                fix_edge_deletion(v, u);
                loglik = score();
            }
        }
    }
}
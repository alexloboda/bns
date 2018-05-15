package ctlab.mcmc;

import ctlab.bn.BayesianNetwork;
import ctlab.bn.sf.ScoringFunction;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.log;

public class Model {
    private static double LADD = Math.log(0.5);
    private static double LDEL = Math.log(2.0);

    private int n;
    private long[][] hits;
    private long[][] time;
    private double[] ll;
    private double loglik;
    private boolean random_policy;

    private BayesianNetwork bn;
    private ScoringFunction sf;
    private long steps;
    private Random random;

    public Model(BayesianNetwork bn, ScoringFunction sf, boolean random_policy) {
        this.random_policy = random_policy;
        this.sf = sf;
        n = bn.size();
        hits = new long[n][n];
        this.bn = bn;
        random = ThreadLocalRandom.current();
        time = new long[n][n];
        ll = new double[n];
        calculateLikelihood();
    }

    private void calculateLikelihood() {
        loglik = bn.logPrior();
        for (int i = 0; i < n; i++) {
            ll[i] = bn.score(i, sf);
            loglik += ll[i];
        }
    }

    public void run() {
        this.bn = new BayesianNetwork(this.bn);
        if (random_policy) {
            bn.random_policy();
        }
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
                try_reverse(v, u);
            }
        } else {
            try_add(v, u);
        }
    }

    public long[][] hits() {
        return hits;
    }

    private void fix_edge_deletion(int v, int u) {
        hits[v][u] += steps - time[v][u];
    }

    private void add_edge(int v, int u) {
        loglik -= bn.logPrior();
        bn.add_edge(v, u);
        loglik += bn.logPrior();
        loglik -= ll[u];
        ll[u] = bn.score(u, sf);
        loglik += ll[u];
    }

    private void remove_edge(int v, int u) {
        loglik -= bn.logPrior();
        bn.remove_edge(v, u);
        loglik += bn.logPrior();
        loglik -= ll[u];
        ll[u] = bn.score(u, sf);
        loglik += ll[u];
    }

    private void try_remove(int v, int u) {
        double prevll = loglik;
        remove_edge(v, u);

        double log_accept = loglik - prevll + LDEL;
        if (log(random.nextDouble()) < log_accept) {
            fix_edge_deletion(v, u);
        } else {
            add_edge(v, u);
        }
    }

    private void try_reverse(int v, int u) {
        double prevll = loglik;
        if (bn.path_exists(u, v)) {
            return;
        }

        add_edge(u, v);
        remove_edge(v, u);

        double log_accept = loglik - prevll;

        if (log(random.nextDouble()) < log_accept) {
            time[u][v] = steps;
            fix_edge_deletion(v, u);
        } else {
            remove_edge(u, v);
            add_edge(v, u);
        }
    }

    private void try_add(int v, int u){
       double prevll = loglik;
       if (bn.path_exists(u, v)) {
           return;
       }

       add_edge(v, u);

       double log_accept = loglik - prevll + LADD;
       if (log(random.nextDouble()) < log_accept) {
           time[v][u] = steps;
       } else {
           remove_edge(v, u);
       }
   }

    public void finish() {
        for (int u = 0; u < n; u++) {
            for (int v : bn.ingoing_edges(u)) {
                remove_edge(v, u);
                fix_edge_deletion(v, u);
            }
        }
        bn = null;
        time = null;
        ll = null;
    }
}
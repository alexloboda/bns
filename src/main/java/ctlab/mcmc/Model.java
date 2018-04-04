package ctlab.mcmc;

import ctlab.bn.BayesianNetwork;
import ctlab.bn.K2ScoringFunction;
import ctlab.bn.PriorDistribution;

import java.util.Random;

import static java.lang.Math.log;
import static ctlab.mcmc.Logger.*;

public class Model {
    private static final double LADD = log(0.5);
    private static final double LOTH = log(2.0);

    private int n;
    private int[][] hits;
    private int[][] time;
    private double loglik;

    private BayesianNetwork bn;
    private K2ScoringFunction sf;
    private PriorDistribution pd;
    private int disc_steps;
    private int steps;
    private Random random;

    private Logger logger;

    public Model(BayesianNetwork bn, int disc_steps) {
        n = bn.size();
        hits = new int[n][n];
        this.bn = bn;
        random = new Random();
        time = new int[n][n];
        bn.discretize(disc_steps);
        sf = new K2ScoringFunction();
        pd = new PriorDistribution(bn.size(), 2);
        loglik = bn.score(sf, pd);
        this.disc_steps = disc_steps;
        logger = new Logger();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
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
        logger.edge(v, u, loglik);
        if (bn.edge_exists(v, u)) {
            if (random.nextBoolean()) {
                logger.action(Action.REMOVE);
                try_remove(v, u);
            } else {
                logger.action(Action.REVERSE);
                try_inverse(v, u);
            }
        } else {
            logger.action(Action.INSERT);
            try_add(v, u);
        }
        logger.submit();
    }

    public void closeLogger() {
        logger.close();
    }

    public int[][] hits() {
        return hits;
    }

    private void try_inverse(int v, int u) {
        bn.remove_edge(v, u);
        if (bn.path_exists(v, u)) {
            logger.status(Status.CYCLE);
            bn.add_edge(v, u);
            return;
        }
        bn.add_edge(u, v);
        bn.backup();
        double score = score();
        double log_accept = score - loglik;
        logger.log_accept(log_accept);
        if (log(random.nextDouble()) < log_accept) {
            logger.status(Status.ACCEPTED);
            time[u][v] = steps;
            fix_edge_deletion(v, u);
            loglik = score;
        } else {
            logger.status(Status.REJECTED);
            bn.remove_edge(u, v);
            bn.add_edge(v, u);
            bn.restore();
        }
    }

    private void fix_edge_deletion(int v, int u) {
        hits[v][u] += steps - time[v][u];
    }

    private double score() {
        bn.discretize(disc_steps);
        double res = bn.score(sf, pd);
        logger.score(res);
        return res;
    }

    private void try_remove(int v, int u) {
        bn.backup();
        bn.remove_edge(v, u);
        double score = score();
        double log_accept = score - loglik + LOTH;
        logger.log_accept(log_accept);
        if (log(random.nextDouble()) < log_accept) {
            logger.status(Status.ACCEPTED);
            fix_edge_deletion(v, u);
            loglik = score;
        } else {
            logger.status(Status.REJECTED);
            bn.add_edge(v, u);
            bn.restore();
        }
    }

    private void try_add(int v, int u){
       if (bn.path_exists(u, v)) {
           logger.status(Status.CYCLE);
           return;
       }
       bn.add_edge(v, u);
       bn.backup();
       double score = score();
       double log_accept = score - loglik + LADD;
       logger.log_accept(log_accept);
       if (log(random.nextDouble()) < log_accept) {
           logger.status(Status.ACCEPTED);
           time[v][u] = steps;
           loglik = score;
       } else {
           logger.status(Status.REJECTED);
           bn.remove_edge(v, u);
           bn.restore();
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
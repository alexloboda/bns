package ctlab.mcmc;

import ctlab.bn.BayesianNetwork;
import ctlab.bn.sf.ScoringFunction;

import java.util.Random;

import static java.lang.Math.log;
import static ctlab.mcmc.Logger.*;

public class Model {
    private static double LADD = Math.log(0.5);
    private static double LDEL = Math.log(2.0);

    private int n;
    private long[][] hits;
    private long[][] time;
    private double[] ll;
    private double loglik;

    private BayesianNetwork bn;
    private ScoringFunction sf;
    private long steps;
    private Random random;

    private Logger logger;

    public Model(BayesianNetwork bn, ScoringFunction sf) {
        this.sf = sf;
        n = bn.size();
        hits = new long[n][n];
        this.bn = bn;
        random = new Random();
        time = new long[n][n];
        ll = new double[n];
        calculateLikelihood();
        logger = new Logger();
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
                try_reverse(v, u);
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

    public long[][] hits() {
        return hits;
    }

    private void fix_edge_deletion(int v, int u) {
        hits[v][u] += steps - time[v][u];
    }

    private int[] count_cardinals(BayesianNetwork bn) {
        int[] cs = new int[bn.observations()];
        for (int i = 0; i < bn.size(); i++) {
            cs[bn.cardinality(i)]++;
        }
        return cs;
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

    private boolean try_remove(int v, int u) {
        double prevll = loglik;
        remove_edge(v, u);

        logger.card(count_cardinals(bn));
        logger.score(loglik);

        double log_accept = loglik - prevll + LDEL;
        logger.log_accept(log_accept);
        logger.prior(bn.logPrior());
        if (log(random.nextDouble()) < log_accept) {
            logger.status(Status.ACCEPTED);
            fix_edge_deletion(v, u);
            return true;
        } else {
            logger.status(Status.REJECTED);
            add_edge(v, u);
            return false;
        }
    }

    private boolean try_reverse(int v, int u) {
        double prevll = loglik;
        if (bn.path_exists(u, v)) {
            logger.status(Status.CYCLE);
            return false;
        }

        add_edge(u, v);
        remove_edge(v, u);

        logger.score(loglik);
        double log_accept = loglik - prevll;
        logger.log_accept(log_accept);

        logger.prior(bn.logPrior());
        if (log(random.nextDouble()) < log_accept) {
            logger.status(Status.ACCEPTED);
            time[u][v] = steps;
            fix_edge_deletion(v, u);
            return true;
        } else {
            logger.status(Status.REJECTED);
            remove_edge(u, v);
            add_edge(v, u);
            return false;
        }
    }

    private boolean try_add(int v, int u){
       double prevll = loglik;
       if (bn.path_exists(u, v)) {
           logger.status(Status.CYCLE);
           return false;
       }

       add_edge(v, u);

       logger.score(loglik);
       double log_accept = loglik - prevll + LADD;
       logger.log_accept(log_accept);
       logger.prior(bn.logPrior());
       if (log(random.nextDouble()) < log_accept) {
           logger.status(Status.ACCEPTED);
           time[v][u] = steps;
           return true;
       } else {
           logger.status(Status.REJECTED);
           remove_edge(v, u);
           return false;
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
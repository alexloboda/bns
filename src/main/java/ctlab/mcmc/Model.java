package ctlab.mcmc;

import ctlab.SegmentTree;
import ctlab.bn.BayesianNetwork;
import ctlab.bn.sf.ScoringFunction;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Model {
    private int n;
    private long[][] hits;
    private long[][] time;
    private double[] ll;
    private double loglik;
    private boolean random_policy;
    private boolean random_dag;

    private Distribution cachedStates;

    private BayesianNetwork bn;
    private ScoringFunction sf;
    private long steps;
    private SplittableRandom random;
    private SegmentTree transitions;

    public Model(BayesianNetwork bn, ScoringFunction sf, SplittableRandom random,
                 boolean random_policy, boolean random_dag, int nCachedStates) {
        this.random_policy = random_policy;
        this.random_dag = random_dag;
        this.sf = sf;
        n = bn.size();
        hits = new long[n][n];
        this.bn = bn;
        this.random = random;
        time = new long[n][n];
        ll = new double[n];
        transitions = new SegmentTree(n);
        //cachedStates = new Distribution(nCachedStates);
    }

    private void calculateLikelihood() {
        loglik = -Double.NEGATIVE_INFINITY;
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
        if (random_dag) {
            sample_dag();
        }
        calculateLikelihood();
    }

    private void sample_dag() {
        int n = bn.size();
        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            order.add(i);
        }

        Random rd = ThreadLocalRandom.current();
        Collections.shuffle(order, rd);

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (rd.nextBoolean()) {
                    add_edge(order.get(i), order.get(j));
                }
            }
        }
    }

    public void step() {

    }

    public long[][] hits() {
        return hits;
    }

    private void fix_edge_deletion(int v, int u) {
        hits[v][u] += steps - time[v][u];
    }

    private void add_edge(int v, int u) {
        /*
        bn.add_edge(v, u);
        loglik -= ll[u];
        ll[u] = bn.score(u, sf);
        loglik += ll[u];
        */
    }

    private void remove_edge(int v, int u) {
        /*
        bn.remove_edge(v, u);
        loglik -= ll[u];
        ll[u] = bn.score(u, sf);
        loglik += ll[u];
        */
    }

    private void try_remove(int v, int u) {
        /*double prevll = loglik;
        remove_edge(v, u);

        double log_accept = loglik - prevll + LDEL;
        if (log(random.nextDouble()) < log_accept) {
            fix_edge_deletion(v, u);
        } else {
            add_edge(v, u);
        }*/
    }

    private void try_add(int v, int u){
        /*
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
       */
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
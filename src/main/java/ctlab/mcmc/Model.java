package ctlab.mcmc;

import ctlab.SegmentTree;
import ctlab.bn.BayesianNetwork;
import ctlab.bn.action.Multinomial;
import ctlab.bn.action.MultinomialFactory;
import ctlab.bn.sf.ScoringFunction;
import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class Model {
    private int n;
    private long[][] hits;
    private long[][] time;
    private double[] ll;
    private double loglik;
    private boolean random_policy;
    private boolean random_dag;

    private List<Cache> cache;
    private List<Multinomial> distributions;
    private MultinomialFactory multFactory;
    private int nCachedStates;

    private BayesianNetwork bn;
    private ScoringFunction sf;
    private long steps;
    private SplittableRandom random;
    private SegmentTree transitions;

    public Model(BayesianNetwork bn, ScoringFunction sf, SplittableRandom random,
                 boolean random_policy, boolean random_dag, MultinomialFactory multFactory,
                 int nCachedStates) {
        this.random_policy = random_policy;
        this.random_dag = random_dag;
        this.sf = sf;
        n = bn.size();
        hits = new long[n][n];
        this.bn = bn;
        this.random = random;
        time = new long[n][n];
        ll = new double[n];
        distributions = new ArrayList<>();
        this.multFactory = multFactory;
        this.nCachedStates = nCachedStates;
        cache = new ArrayList<>();
    }

    private void calculateLikelihood() {
        loglik = -Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            ll[i] = bn.score(i, sf);
            loglik += ll[i];
        }
    }

    public Function<List<Integer>, Multinomial> multinomials(int v) {
        return ps -> {
            double currLL = ll[v];
            Function<Integer, Double> computeLL = i -> {
                if (i >= v) {
                    ++i;
                }
                if (bn.edge_exists(i, v)) {
                    return bn.scoreExcluding(v, sf, i) - currLL;
                } else {
                    return bn.scoreIncluding(v, sf, i) - currLL;
                }
            };
            return multFactory.spark(computeLL, -2 * Math.log(n - 1));
        };
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

        transitions = new SegmentTree(n);
        for (int i = 0; i < n; i++) {
            cache.add(new Cache(nCachedStates, multinomials(i)));
            distributions.add(cache.get(i).request(Collections.emptyList()));
            transitions.set(i, (float)distributions.get(i).logLikelihood());
        }
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
        float ll = transitions.likelihood();
        GeometricDistribution gd = new GeometricDistribution(1.0 - Math.exp(ll));
        double jump = gd.getNumericalMean();
        steps += (int)jump + 1;
        if (random.nextDouble() < jump - (int)jump) {
            steps++;
        }

        int node = transitions.randomChoice(random);
        Multinomial mult = distributions.get(node);
        Short parent = mult.randomAction();
        if (parent == null) {
            return;
        }
        if (bn.edge_exists(parent, node)) {
            try_remove(parent, node);
        } else {
            try_add(parent, node);
        }
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
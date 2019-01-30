package ctlab.mcmc;

import ctlab.SegmentTree;
import ctlab.bn.BayesianNetwork;
import ctlab.bn.action.Multinomial;
import ctlab.bn.action.MultinomialFactory;
import ctlab.bn.sf.ScoringFunction;
import org.apache.commons.math3.distribution.GeometricDistribution;

import java.util.*;
import java.util.function.Function;

public class Model {
    public static final double EPS = 1e-5;

    private int n;
    private long[][] hits;
    private long[][] time;
    private double[] ll;
    private double loglik;
    private boolean randomPolicy;
    private boolean randomDAG;

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
        this.randomPolicy = random_policy;
        this.randomDAG = random_dag;
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
        loglik = 0.0;
        for (int i = 0; i < n; i++) {
            ll[i] = bn.score(i, sf);
            loglik += ll[i];
        }
    }

    public void processPathElimination(int v, int u) {
        u = u > v ? u - 1 : u;
        distributions.get(v).reEnableAction((short)u);
        transitions.set(v, distributions.get(v).logLikelihood());
    }

    private Function<List<Integer>, Multinomial> multinomials(int v) {
        return ps -> {
            double currLL = ll[v];
            Function<Integer, Double> computeLL = i -> {
                if (i >= v) {
                    ++i;
                }
                if (bn.edgeExists(i, v)) {
                    return Math.min(bn.scoreExcluding(v, sf, i) - currLL, 0.0);
                } else {
                    return Math.min(bn.scoreIncluding(v, sf, i) - currLL, 0.0);
                }
            };
            return multFactory.spark(computeLL, -Math.log(n * (n - 1)));
        };
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int u = 0; u < n; u++) {
            for (int v: bn.ingoingEdges(u)) {
                s.append(v).append("->").append(u).append(" ");
            }
        }
        return s.toString();
    }

    public void printDebugInfo() {
        System.out.println("Current log-likelihood " + loglik);
        for (int u = 0; u < n; u++) {
            for (int v: bn.ingoingEdges(u)) {
                System.out.print(v + "->" + u + " ");
            }
        }
        System.out.println();
        for (int i = 0; i < n; i++) {
            distributions.get(i).printDebugInfo(i);
        }
        System.out.println();
    }

    public void run() {
        if (randomPolicy) {
            bn.randomPolicy();
        }

        this.bn = new BayesianNetwork(this.bn);
        this.bn.setCallback(this::processPathElimination);
        if (randomDAG) {
            sampleDAG();
        }

        calculateLikelihood();

        transitions = new SegmentTree(n);
        for (int i = 0; i < n; i++) {
            cache.add(new Cache(nCachedStates, multinomials(i)));
            List<Integer> ps = this.bn.ingoingEdges(i);
            Collections.sort(ps);
            distributions.add(cache.get(i).request(ps));
            transitions.set(i, distributions.get(i).logLikelihood());
        }
    }

    private void sampleDAG() {
        int n = bn.size();
        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            order.add(i);
        }

        Random re = new Random(random.nextInt());
        Collections.shuffle(order, re);

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (random.nextBoolean()) {
                    bn.addEdge(order.get(i), order.get(j));
                }
            }
        }
    }

    public void step(long limit) {
        double ll = transitions.likelihood();
        assert ll < 0.1;
        double jump = 0.0;
        double likelihood = Math.exp(ll);
        if (likelihood < 1.0) {
            GeometricDistribution gd = new GeometricDistribution(likelihood);
            jump = gd.getNumericalMean();
        }
        steps += (int)jump + 1;
        if (random.nextDouble() < jump - (int)jump) {
            steps++;
        }
        if (steps > limit) {
            return;
        }

        int node = transitions.randomChoice(random);
        Multinomial mult = distributions.get(node);
        Short parent = mult.randomAction();
        transitions.set(node, mult.logLikelihood());
        if (parent == null) {
            return;
        }
        if (parent >= node) {
            ++parent;
        }
        if (bn.edgeExists(parent, node)) {
            removeEdge(parent, node);
        } else {
            if (bn.pathExists(node, parent)) {
                mult.disableAction((short)(parent > node ? parent - 1 : parent), mult.getLastLL());
                transitions.set(node, mult.logLikelihood());
                return;
            }

            addEdge(parent, node);
        }
    }

    public long steps() {
        return steps;
    }

    public boolean[][] adjMatrix() {
        boolean[][] m = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for(int j = 0; j < n; j++) {
                m[i][j] = bn.edgeExists(i, j);
            }
        }
        return m;
    }

    public double[][] frequencies() {
        double[][] fs = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                fs[i][j] = (double)hits[i][j] / steps;
            }
        }
        return fs;
    }

    private void updateDistribution(int u) {
        distributions.get(u).deactivate();
        List<Integer> parentSet = bn.ingoingEdges(u);
        Collections.sort(parentSet);
        Multinomial mult = cache.get(u).request(parentSet);
        distributions.set(u, mult);
        transitions.set(u, mult.logLikelihood());
    }

    private void addEdge(int v, int u) {
        bn.addEdge(v, u);
        loglik -= ll[u];
        ll[u] = bn.score(u, sf);
        loglik += ll[u];
        updateDistribution(u);
    }

    private void removeEdge(int v, int u) {
        bn.removeEdge(v, u);
        loglik -= ll[u];
        ll[u] = bn.score(u, sf);
        loglik += ll[u];
        hits[v][u] += steps - time[v][u];
        updateDistribution(u);
    }

    public void finish() {
        for (int u = 0; u < n; u++) {
            for (int v : bn.ingoingEdges(u)) {
                removeEdge(v, u);
            }
        }
        bn = null;
        time = null;
        ll = null;
    }
}
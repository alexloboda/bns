package ctlab.mcmc;

import ctlab.SegmentTree;
import ctlab.bn.BayesianNetwork;
import ctlab.bn.action.Multinomial;
import ctlab.bn.action.MultinomialFactory;
import org.apache.commons.math3.distribution.GeometricDistribution;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Model {
    public static final double EPS = 1e-5;

    private int n;
    private long[][] hits;
    private long[][] time;
    private double[] ll;
    private double loglik;
    private boolean randomDAG;
    private double beta;

    private List<Cache> cache;
    private List<Multinomial> distributions;
    private MultinomialFactory multFactory;
    private int nCachedStates;

    private BayesianNetwork bn;
    private long steps;
    private SplittableRandom random;
    private SegmentTree transitions;

    private List<Integer> permutation;

    public Model(BayesianNetwork bn, boolean randomDAG, MultinomialFactory multFactory,
                 int nCachedStates, double beta) {
        permutation = IntStream.range(0, bn.size()).boxed().collect(Collectors.toList());
        this.randomDAG = randomDAG;
        this.beta = beta;
        n = bn.size();
        hits = new long[n][n];
        this.bn = bn;
        this.random = new SplittableRandom();
        time = new long[n][n];
        ll = new double[n];
        distributions = new ArrayList<>();
        this.multFactory = multFactory;
        this.nCachedStates = nCachedStates;
        cache = new ArrayList<>();
    }

    public void setRandomGenerator(SplittableRandom re) {
        random = re;
    }

    private void calculateLikelihood() {
        loglik = 0.0;
        for (int i = 0; i < n; i++) {
            ll[i] = bn.score(i);
            loglik += ll[i];
        }
    }

    public double logLikelihood() {
        return loglik;
    }

    double computeLogLikelihood() {
        double ll = 0.0;
        for (int i = 0; i < n; i++) {
            ll += bn.score(i);
        }
        return ll;
    }

    private void processPathElimination(int v, int u) {
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
                    return bn.scoreExcluding(v, i) - currLL;
                } else {
                    return bn.scoreIncluding(v, i) - currLL;
                }
            };
            return multFactory.spark(computeLL, -Math.log(n * (n - 1)), beta);
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
        bn = new BayesianNetwork(bn);
        permutation = bn.shuffleVariables(new Random(random.nextInt()));

        bn.setCallback(this::processPathElimination);
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
            removeEdge(parent, node, mult.getLastLL());
        } else {
            if (bn.pathExists(node, parent)) {
                mult.disableAction((short)(parent > node ? parent - 1 : parent), mult.getLastLL());
                transitions.set(node, mult.logLikelihood());
                return;
            }

            addEdge(parent, node, mult.getLastLL());
        }
    }

    public long steps() {
        return steps;
    }

    public boolean[][] adjMatrix() {
        boolean[][] m = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for(int j = 0; j < n; j++) {
                m[permutation.get(i)][permutation.get(j)] = bn.edgeExists(i, j);
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

    private void addEdge(int v, int u, double actionLL) {
        bn.addEdge(v, u);
        ll[u] += actionLL;
        loglik += actionLL;
        updateDistribution(u);
    }

    private void removeEdge(int v, int u, double actionLL) {
        bn.removeEdge(v, u);
        ll[u] += actionLL;
        loglik += actionLL;
        hits[v][u] += steps - time[v][u];
        updateDistribution(u);
    }

    public static void swapNetworks(Model model, Model other) {
        for (int u = 0; u < model.bn.size(); u++) {
            Set<Integer> modelEdges = new LinkedHashSet<>(model.bn.ingoingEdges(u));
            Set<Integer> otherModelEdges = new LinkedHashSet<>(other.bn.ingoingEdges(u));
            int finalU = u;
            modelEdges.stream()
                    .filter(otherModelEdges::contains)
                    .forEach(v -> {
                        model.bn.removeEdge(v, finalU);
                        other.bn.addEdge(v, finalU);
                    });
            otherModelEdges.stream()
                    .filter(modelEdges::contains)
                    .forEach(v -> {
                        model.bn.addEdge(v, finalU);
                        other.bn.removeEdge(v, finalU);
                    });
            double ll = model.ll[u];
            model.ll[u] = other.ll[u];
            other.ll[u] = ll;
        }
        double ll = model.loglik;
        model.loglik = other.loglik;
        other.loglik = ll;
    }

    public void finish() {
        for (int u = 0; u < n; u++) {
            for (int v : bn.ingoingEdges(u)) {
                removeEdge(v, u, 0.0);
            }
        }
        bn = null;
        time = null;
        ll = null;
    }
}
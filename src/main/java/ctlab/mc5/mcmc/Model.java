package ctlab.mc5.mcmc;

import ctlab.mc5.algo.SegmentTree;
import ctlab.mc5.bn.BayesianNetwork;
import ctlab.mc5.bn.action.Multinomial;
import ctlab.mc5.bn.action.MultinomialFactory;
import ctlab.mc5.mcmc.EdgeList.Edge;
import org.apache.commons.math3.distribution.GeometricDistribution;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Model {
    private int n;
    private double[] ll;
    private double loglik;
    private double beta;
    private long steps;

    private List<Cache> caches;
    private int nCachedStates;

    private List<Multinomial> distributions;
    private MultinomialFactory multFactory;
    private SegmentTree transitions;

    private BayesianNetwork bn;
    private SplittableRandom random;

    private List<Integer> permutation;

    public Model(BayesianNetwork bn, MultinomialFactory multFactory,
                 int nCachedStates, double beta) {
        this.permutation = IntStream.range(0, bn.size()).boxed().collect(Collectors.toList());
        this.beta = beta;
        this.n = bn.size();
        this.bn = bn;
        this.ll = new double[n];
        this.distributions = new ArrayList<>();
        this.multFactory = multFactory;
        this.nCachedStates = nCachedStates;
        this.caches = new ArrayList<>();
        setRandomGenerator(new SplittableRandom());
    }

    public void setRandomGenerator(SplittableRandom re) {
        random = re;
        multFactory.setRandomEngine(re);
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
        distributions.get(v).reEnableAction((short) u);
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
            return multFactory.spark(bn.size() - 1, computeLL, -Math.log(n * (n - 1)), beta);
        };
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int u = 0; u < n; u++) {
            for (int v : bn.ingoingEdges(u)) {
                s.append(v).append("->").append(u).append(" ");
            }
        }
        return s.toString();
    }

    public void printDebugInfo() {
        System.out.println("Current log-likelihood " + loglik);
        for (int u = 0; u < n; u++) {
            for (int v : bn.ingoingEdges(u)) {
                System.out.print(v + "->" + u + " ");
            }
        }
        System.out.println();
        for (int i = 0; i < n; i++) {
            distributions.get(i).printDebugInfo(i);
        }
        System.out.println();
    }

    public void init(boolean randomDAG) {
        bn = new BayesianNetwork(bn);
        permutation = bn.shuffleVariables(new Random(random.nextInt()));

        bn.setCallback(this::processPathElimination);
        if (randomDAG) {
            sampleDAG();
        }

        calculateLikelihood();

        transitions = new SegmentTree(n);
        for (int i = 0; i < n; i++) {
            caches.add(new Cache(nCachedStates, multinomials(i)));
            List<Integer> ps = this.bn.ingoingEdges(i);
            Collections.sort(ps);
            distributions.add(caches.get(i).request(ps));
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

    public boolean step(long limit) {
        double ll = transitions.likelihood();
        assert ll < 0.1;
        double jump = 0.0;
        double likelihood = Math.exp(ll);
        if (likelihood < 1.0) {
            GeometricDistribution gd = new GeometricDistribution(likelihood);
            jump = gd.getNumericalMean();
        }
        jump += 1.0;
        if (random.nextDouble() < jump - (int) jump) {
            jump += 1.0;
        }
        if (steps + jump > limit) {
            return true;
        }
        steps += jump;

        int node = transitions.randomChoice(random);
        Multinomial mult = distributions.get(node);
        Short parent = mult.randomAction();
        transitions.set(node, mult.logLikelihood());
        if (parent == null) {
            return steps == limit;
        }
        if (parent >= node) {
            ++parent;
        }
        if (bn.edgeExists(parent, node)) {
//            if (!bn.pathExists(node, parent)) {
//                boolean vshape = bn.canBeVShape(node, parent);
//                if (!vshape) {
//                    tryInvert(parent, node);
//                    return steps == limit;
//                }
//            }
            removeEdge(parent, node, mult.getLastLL());
        } else {
            if (bn.pathExists(node, parent)) {
                mult.disableAction((short) (parent > node ? parent - 1 : parent), mult.getLastLL());
                transitions.set(node, mult.logLikelihood());
                return steps == limit;
            }

            addEdge(parent, node, mult.getLastLL());
        }
        return steps == limit;
    }

    private void tryInvert(int v, int u) {
        removeEdge(v, u, 0);
        addEdge(u, v, 0);
    }

    public EdgeList edgeList() {
        EdgeList edges = new EdgeList();
        for (int u = 0; u < bn.size(); u++) {
            for (int v : bn.ingoingEdges(u)) {
                edges.addEdge(new Edge(permutation.get(v), permutation.get(u), 1, 1));
            }
        }
        return edges;
    }

    public boolean[][] adjMatrix() {
        boolean[][] m = new boolean[n][n];
        for (int u = 0; u < bn.size(); u++) {
            for (int v : bn.ingoingEdges(u)) {
                m[permutation.get(v)][permutation.get(u)] = true;
            }
        }
        return m;
    }

    private void updateDistribution(int u) {
        distributions.get(u).deactivate();
        List<Integer> parentSet = bn.ingoingEdges(u);
        Collections.sort(parentSet);
        Multinomial mult = caches.get(u).request(parentSet);
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

            model.updateDistribution(u);
            other.updateDistribution(u);
        }
        double ll = model.loglik;
        model.loglik = other.loglik;
        other.loglik = ll;
    }

    public double beta() {
        return beta;
    }
}
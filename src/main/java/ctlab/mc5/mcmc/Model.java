package ctlab.mc5.mcmc;

import ctlab.mc5.algo.SegmentTree;
import ctlab.mc5.bn.BayesianNetwork;
import ctlab.mc5.bn.Variable;
import ctlab.mc5.bn.action.Multinomial;
import ctlab.mc5.bn.action.MultinomialFactory;
import ctlab.mc5.mcmc.EdgeList.Edge;
import org.apache.commons.math3.util.Pair;

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

    private final List<Multinomial> distributions;
    private final MultinomialFactory multFactory;
    private SegmentTree transitions;

    private BayesianNetwork bn;
    private SplittableRandom random;

    private List<Integer> permutation;
    private final double initLL;
    private final double reverseLL;

    private final long[][] counter;
    private final long[][] time;

    private final boolean raw;

    public Model(BayesianNetwork bn, MultinomialFactory multFactory,
                 int nCachedStates, double beta, boolean raw) {
        this.permutation = IntStream.range(0, bn.size()).boxed().collect(Collectors.toList());
        this.beta = beta;
        this.n = bn.size();
        counter = new long[n][n];
        time = new long[n][n];
        this.bn = bn;
        this.ll = new double[n];
        this.distributions = new ArrayList<>();
        this.multFactory = multFactory;
        this.nCachedStates = nCachedStates;
        this.caches = new ArrayList<>();

        double reverseProb = 1.0 / ((double) n * 50 );
        this.reverseLL = Math.log(reverseProb);
        double totalTransitions = n * (n - 1);
        this.initLL = Math.log((1.0 - reverseProb) / totalTransitions);
        setRandomGenerator(new SplittableRandom());
        this.raw = raw;
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

    public long getSteps() {
        return steps;
    }

    public double logLikelihood() {
        return loglik;
    }

    double computeLogLikelihood() {
        double ll = 0.0;
        for (int i = 0; i < bn.size(); i++) {
            ll += bn.score(i);
        }
        return ll;
    }

    private void processPathElimination(int v, int u) {
        u = u > v ? u - 1 : u;
        distributions.get(v).reEnableAction((short) u);
        transitions.set(v, distributions.get(v).logLikelihood());
    }

    private Function<List<Integer>, Multinomial> multinomials(int to_node) {
        return ps -> {
            Function<Integer, Double> computeLL = i -> {
                double currLL = ll[to_node];
//                assert(currLL == ll[to_node]);
                if (i >= to_node) {
                    ++i;
                }
                if (bn.edgeExists(i, to_node)) {
                    return bn.scoreExcluding(i, to_node) - currLL;
                } else {
                    return bn.scoreIncluding(i, to_node) - currLL;
                }
            };
            return multFactory.spark(bn.size() - 1, computeLL, initLL, beta);
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

    public int getN() {
        return n;
    }

    public void init(boolean randomDAG) {
        bn = new BayesianNetwork(bn);
        bn.randomPolicy();
        permutation = bn.shuffleVariables(new Random(random.nextInt()));

        bn.setCallback(this::processPathElimination);
        if (randomDAG) {
            sampleDAG();
        }
        calculateLikelihood();

        transitions = new SegmentTree(n);
        for (int i = 0; i < n; i++) {
            caches.add(new Cache(nCachedStates, multinomials(i)));
            List<Integer> ps = bn.ingoingEdges(i);
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

    private void fix_delete(int from, int to) {
        counter[from][to] += steps - time[from][to];
    }

    private void reverse() {
        if (bn.getEdgeCount() == 0) {
            return;
        }
        Pair<Integer, Integer> edge = bn.randomEdge(random);
        int from = edge.getFirst();
        int to = edge.getSecond();

        assert from != to;

        assert bn.edgeExists(from, to);
        if (bn.isSubscribed(to, from)) {
            return;
        }
        Set<Variable> parentFrom = bn.parentSet(from);
        Set<Variable> parentTo = bn.parentSet(to);
        Variable fromVar = bn.var(from);
        Variable toVar = bn.var(to);
        double scoreF = bn.getScoringFunction().score(fromVar, parentFrom, bn.size());
        double scoreT = bn.getScoringFunction().score(toVar, parentTo, bn.size());
        double systemLL = scoreF + scoreT;
        bn.removeEdge(from, to);
        if (!bn.pathExists(from, to)) {
            bn.addEdge(to, from);
            parentTo.remove(fromVar);
            parentFrom.add(toVar);
            double scoreFRev = bn.getScoringFunction().score(fromVar, parentFrom, bn.size());
            double scoreTRev = bn.getScoringFunction().score(toVar, parentTo, bn.size());
            double systemLLRev = scoreFRev + scoreTRev;
            if (Math.log(random.nextDouble()) < systemLLRev - systemLL) {
                bn.removeEdge(to, from);
                bn.addEdge(from, to);
                removeEdge(from, to, scoreTRev - ll[to]);
                addEdge(to, from, scoreFRev - ll[from]);
//                updateLL(to, scoreTRev - ll[to]);
//                updateLL(from, scoreFRev - ll[from]);
                time[to][from] = steps;
                fix_delete(from, to);
            } else {
                bn.removeEdge(to, from);
                bn.addEdge(from, to);
            }
        } else {
            bn.addEdge(from, to);
        }
    }

    public boolean step(long limit) {
        double trll = transitions.likelihood();
        double rmll = reverseLL;

        double all_ll = Multinomial.likelihoodsSum(trll, rmll);
        assert all_ll <= 0.01;
        int jump = 0;
        double geo = 0.0;
        double likelihood = Math.exp(all_ll);
        if (likelihood < 1.0) {
            geo = (1 - likelihood) / likelihood; // geometric distribution
        }
        jump += (int) geo;
        jump += 1.0;
        if (random.nextDouble() < geo - (int) geo) {
            jump += 1.0;
        }
        if (steps + jump > limit) {
            return true;
        }
        steps += jump;

        double proportions = Math.exp(rmll - all_ll);

        if (random.nextDouble() < proportions) {
            reverse();
            assert (Math.abs(computeLogLikelihood() - logLikelihood()) < 0.1);
            return steps == limit;
        }

        int node = transitions.randomChoice(random);
        Multinomial mult = distributions.get(node);
        Short parent = mult.randomAction(true);
        transitions.set(node, mult.logLikelihood());

        if (parent == null) {
            return steps == limit;
        }

        if (parent >= node) {
            ++parent;
        }
        if (bn.edgeExists(parent, node)) {
            removeEdge(parent, node, mult.getLastLL());
            fix_delete(parent, node);
            assert (Math.abs(computeLogLikelihood() - logLikelihood()) < 0.1);
        } else {

            if (bn.pathExists(node, parent)) {
                mult.disableAction((short) (parent > node ? parent - 1 : parent), mult.getLastLL());
                transitions.set(node, mult.logLikelihood());
                assert (Math.abs(computeLogLikelihood() - logLikelihood()) < 0.1);
                return steps == limit;
            }
            addEdge(parent, node, mult.getLastLL());
            time[parent][node] = steps;
            assert (Math.abs(computeLogLikelihood() - logLikelihood()) < 0.1);
        }
        return steps == limit;
    }

    public EdgeList edgeList() {
        EdgeList edges = new EdgeList(1);
        for (int u = 0; u < bn.size(); u++) {
            for (int v : bn.ingoingEdges(u)) {
                edges.addEdge(new Edge(permutation.get(v), permutation.get(u), 1));
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

    private void updateDistribution(int to) {
        distributions.get(to).deactivate();
        List<Integer> parentSet = bn.ingoingEdges(to);
        Collections.sort(parentSet);
        Multinomial mult = caches.get(to).request(parentSet);
        distributions.set(to, mult);
        transitions.set(to, mult.logLikelihood());
    }

    private void addEdge(int from, int to, double actionLL) {
        distributions.get(to).deactivate();
        bn.addEdge(from, to);
        ll[to] += actionLL;
        loglik += actionLL;
        List<Integer> parentSet = bn.ingoingEdges(to);
        Collections.sort(parentSet);
        Multinomial mult = caches.get(to).request(parentSet);
        distributions.set(to, mult);
        transitions.set(to, mult.logLikelihood());
    }

    private void removeEdge(int from, int to, double actionLL) {
        distributions.get(to).deactivate();
        bn.removeEdge(from, to);
        ll[to] += actionLL;
        loglik += actionLL;
        List<Integer> parentSet = bn.ingoingEdges(to);
        Collections.sort(parentSet);
        Multinomial mult = caches.get(to).request(parentSet);
        distributions.set(to, mult);
        transitions.set(to, mult.logLikelihood());
    }

    private void updateLL(int to, double actionLL) {
        ll[to] += actionLL;
        loglik += actionLL;
        updateDistribution(to);
    }

    public static void swapNetworks(Model model, Model other) {
        List<Set<Integer>> modelAllEdges = new ArrayList<>();
        List<Set<Integer>> otherModelAllEdges = new ArrayList<>();

        for (int to = 0; to < model.bn.size(); to++) {
            modelAllEdges.add(new LinkedHashSet<>(model.bn.ingoingEdges(to)));
            otherModelAllEdges.add(new LinkedHashSet<>(other.bn.ingoingEdges(to)));
        }

        {
            List<Cache> tmp = model.caches;
            model.caches = other.caches;
            other.caches = tmp;
        }
        {
            List<Integer> tmpperm = model.permutation;
            model.permutation = other.permutation;
            other.permutation = tmpperm;
        }

//        {
//            SegmentTree tmp1 = model.transitions;
//            model.transitions = other.transitions;
//            other.transitions = tmp1;
//        }

        for (int to = 0; to < model.bn.size(); to++) {
            final int finalTo = to;
            modelAllEdges.get(to)
                    .forEach(from -> {
                        model.bn.removeEdge(from, finalTo);
                    });
            otherModelAllEdges.get(to)
                    .forEach(from -> {
                        other.bn.removeEdge(from, finalTo);
                    });
//            model.updateDistribution(to);
//            other.updateDistribution(to);
        }


        for (int to = 0; to < model.bn.size(); to++) {
            final int finalTo = to;
            modelAllEdges.get(to)
                    .forEach(from -> {
                        other.bn.addEdge(from, finalTo);
                    });
            otherModelAllEdges.get(to)
                    .forEach(from -> {
                        model.bn.addEdge(from, finalTo);
                    });

            double ll = model.ll[to];
            model.ll[to] = other.ll[to];
            other.ll[to] = ll;

            model.updateDistribution(to);
            other.updateDistribution(to);
        }
        double ll = model.loglik;
        model.loglik = other.loglik;
        other.loglik = ll;

    }

    public double beta() {
        return beta;
    }

    public void finish_warmup() {
        steps = 0;
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                counter[i][j] = 0;
                time[i][j] = 0;
            }
        }
    }

    public EdgeList results() {
        EdgeList edgeList;
        if (raw) {
            edgeList = edgeList();
        } else {
            edgeList = new EdgeList(steps);
            for (int i = 0; i < n; ++i) {
                for (int j = 0; j < n; j++) {
                    long sum = counter[i][j];
                    if (bn.edgeExists(i, j)) {
                        sum += steps - time[i][j];
                    }
                    if (sum != 0) {
                        edgeList.addEdge(new Edge(permutation.get(i), permutation.get(j), sum));
                    }
                }
            }
        }
        return edgeList;
    }
}
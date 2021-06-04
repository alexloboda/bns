package ctlab.mc5.bn;


import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Variable implements Comparable<Variable> {
    private String name;

    private List<Double> data;
    private double[] u_x;
    private int[] uniq;
    private int[] orderedObs;
    private double[] logPrecomputed;
    private int[] discrete;
    private List<Double> edges;
    private LogFactorial lf;
    private int defaultDiscClasses;
    private Random random;

    private int lb;
    private int ub;

    private int number;

    public void setDiscLimits(int lb, int ub) {
        this.lb = lb;
        this.ub = ub;
    }

    private String stripName(String name) {
        if (name.charAt(0) == '\"' && name.charAt(name.length() - 1) == '\"') {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    public Variable(String name, List<Double> data, int discClasses, int number) {
        this.number = number;
        random = ThreadLocalRandom.current();
        this.name = stripName(name);
        this.data = new ArrayList<>(data);
        lf = new LogFactorial();

        orderedObs = IntStream.range(0, data.size()).toArray();

        Comparator<Integer> cmp = Comparator.comparingDouble(data::get);

        orderedObs = Arrays.stream(orderedObs)
                .boxed()
                .sorted(cmp)
                .mapToInt(Integer::intValue)
                .toArray();

        List<Integer> uniq = new ArrayList<>();
        if (data.size() > 0) {
            double last = data.get(orderedObs[0]);
            for (int i = 1; i < orderedObs.length; i++) {
                double curr = data.get(orderedObs[i]);
                if (curr != last) {
                    uniq.add(i - 1);
                    last = curr;
                }
            }
        }
        uniq.add(data.size() - 1);
        this.uniq = uniq.stream().mapToInt(Integer::intValue).toArray();
        u_x = new double[uniq.size()];
        for (int i = 0; i < uniq.size(); i++) {
            u_x[i] = data.get(getUniq(i));
        }

        logPrecomputed = IntStream.range(0, data.size() * 2 + 1)
                .mapToDouble(Math::log)
                .toArray();

        this.defaultDiscClasses = discClasses;
        initial(discClasses);
        ub = obsNum();
        lb = 1;
    }

    Variable(Variable v) {
        data = new ArrayList<>(v.data);
        u_x = Arrays.copyOf(v.u_x, v.u_x.length);
        uniq = Arrays.copyOf(v.uniq, v.uniq.length);
        orderedObs = Arrays.copyOf(v.orderedObs, v.orderedObs.length);
        logPrecomputed = v.logPrecomputed;
        discrete = Arrays.copyOf(v.discrete, v.discrete.length);
        edges = new ArrayList<>(v.edges);
        lf = new LogFactorial();
        this.name = v.name;
        defaultDiscClasses = v.defaultDiscClasses;
        this.lb = v.lb;
        this.ub = v.ub;
        random = ThreadLocalRandom.current();
        number = v.number;
    }

    void setLF(LogFactorial lf) {
        this.lf = lf;
    }

    private double getDiscEdge(int u) {
        double discEdge;
        if (u + 1 < uniq.length) {
            discEdge = (getU(u) + getU(u + 1)) / 2.0;
        } else {
            discEdge = Double.POSITIVE_INFINITY;
        }
        return discEdge;
    }

    private void writeDiscretization() {
        discrete = data.stream().mapToInt(x -> -Collections.binarySearch(edges, x)).toArray();
    }

    public Collection<Integer> cardinalities() {
        Map<Integer, Integer> cs = new TreeMap<>();
        for (int d : discrete) {
            cs.putIfAbsent(d, 0);
            cs.put(d, cs.get(d) + 1);
        }
        return cs.values();
    }

    private int getUniq(int i) {
        return orderedObs[uniq[i]];
    }

    private double getU(int i) {
        return u_x[i];
    }

    private int numberOfClasses(int[] obs) {
        return Arrays.stream(obs)
                .max().getAsInt() + 1;
    }

    public Pair<int[], int[]> mapObs(List<Variable> ps) {
        int m = obsNum();
        int[] result1 = new int[m];
        int[] result2 = new int[m];
        int ps_size = ps.size();
        int[] cds1 = new int[ps_size + 1];
        int[] cds2 = new int[ps_size + 1 + 1];
        int i1 = 0;
        for (Variable p : ps) {
            cds1[i1] = p.cardinality();
            cds2[i1] = cds1[i1];
            i1++;
        }
        cds1[ps_size] = 1;
        cds2[ps_size] = this.cardinality();
        cds2[ps_size + 1] = 1;

        Trie t1 = new Trie(cds1);
        Trie t2 = new Trie(cds2);

        Trie.Selector selector1 = t1.selector();
        Trie.Selector selector2 = t2.selector();
        for (int i = 0; i < m; i++) {
            selector1.reuse();
            selector2.reuse();
            for (Variable p : ps) {
                selector1.choose(p.discreteValue(orderedObs[i]) - 1);
                selector2.choose(p.discreteValue(orderedObs[i]) - 1);
            }

            selector2.choose(this.discreteValue(orderedObs[i]) - 1);
            result1[i] = selector1.get();
            result2[i] = selector2.get();
        }
        return new Pair<>(result1, result2);
    }

    public int[] mapObsAnd(List<Variable> ps) {
        int m = obsNum();
        int[] result = new int[m];
        int ps_size = ps.size();
        int[] cds = new int[ps_size + 1 + 1];
        int i1 = 0;
        for (Variable p : ps) {
            cds[i1] = p.cardinality();
            i1++;
        }
        cds[ps_size] = this.cardinality();
        cds[ps_size + 1] = 1;

        Trie t = new Trie(cds);

        Trie.Selector selector = t.selector();
        for (int i = 0; i < m; i++) {
            selector.reuse();
            for (Variable p : ps) {
                selector.choose(p.discreteValue(orderedObs[i]) - 1);
            }
            selector.choose(this.discreteValue(orderedObs[i]) - 1);

            result[i] = selector.get();
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public int obsNum() {
        return data.size();
    }

    public int getNumber() {
        return number;
    }

    void initial(int num_classes) {
        int num_edges = num_classes - 1;
        if (num_classes > uniq.length) {
            throw new IllegalArgumentException("Too many classes");
        }
        edges = new ArrayList<>();
        for (int i = 0; i < num_edges; i++) {
            int pos = (int) Math.round(Math.floor(uniq.length * ((double) i + 1) / num_classes));
            edges.add((data.get(getUniq(pos)) + data.get(getUniq(pos + 1))) / 2);
        }
        writeDiscretization();
    }

    void initial() {
        initial(defaultDiscClasses);
    }

    public int cardinality() {
        return edges.size() + 1;
    }

    public int discreteValue(int obs) {
        return discrete[obs];
    }

    public List<Double> discretizationEdges() {
        return Collections.unmodifiableList(edges);
    }

    public void randomPolicy() {
        double[] edges = IntStream.range(0, uniq.length - 1).mapToDouble(this::getDiscEdge).toArray();
        int k = random.nextInt(ub - lb + 1) + lb - 1;
        List<Integer> idx = IntStream.range(0, edges.length).boxed().collect(Collectors.toList());
        Collections.shuffle(idx, random);
        idx = idx.subList(0, k);
        Collections.sort(idx);
        this.edges = idx.stream().map(x -> edges[x]).collect(Collectors.toList());
        writeDiscretization();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return name.equals(variable.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(Variable variable) {
        return number - variable.number;
    }
}
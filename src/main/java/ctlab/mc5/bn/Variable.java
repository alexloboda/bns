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

        Map<Long, Integer> mapa1 = new HashMap<>();
        Map<Long, Integer> mapa2 = new HashMap<>();
        int n1 = 0;
        int n2 = 0;
        for (int i = 0; i < m; i++) {
            long val1 = 0;
            for (Variable p : ps) {
                val1 = val1 * 3 + (p.discreteValue(orderedObs[i]) - 1);
            }

            long val2 = val1 * 3 + (this.discreteValue(orderedObs[i]) - 1);
            int res1;
            if (mapa1.containsKey(val1)) {
                res1 = mapa1.get(val1);
            } else {
                res1 = n1++;
                mapa1.put(val1, res1);
            }

            int res2;
            if (mapa2.containsKey(val2)) {
                res2 = mapa2.get(val2);
            } else {
                res2 = n2++;
                mapa2.put(val2, res2);
            }
            result1[i] = res1;
            result2[i] = res2;
        }
        return new Pair<>(result1, result2);
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
        return number == variable.number;
    }

    @Override
    public int hashCode() {
        return number;
    }

    @Override
    public int compareTo(Variable variable) {
        return number - variable.number;
    }
}
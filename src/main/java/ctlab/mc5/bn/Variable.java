package ctlab.mc5.bn;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.math3.linear.MatrixUtils.inverse;
import static org.apache.commons.math3.special.Gamma.logGamma;

import org.apache.commons.math3.linear.LUDecomposition;

import org.apache.commons.math3.linear.*;

public class Variable {
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

    private double mean;
    private double variance;

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

        mean = 0;
        for (Double datum : data) {
            mean += datum;
        }
        mean /= data.size();

        variance = 0;
        for (Double datum : data) {
            variance += ((datum - mean) * (datum - mean));
        }
        variance /= (data.size() - 1);

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
        mean = v.mean;
        variance = v.variance;
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

    private static double c(double x) {
        return logGamma(x);
    }

    private static RealMatrix eye(int len) {
        RealMatrix R0 = new Array2DRowRealMatrix().createMatrix(len, len);
        for (int i = 0; i < len; ++i) {
            for (int j = 0; j < len; ++j) {
                if (i == j) {
                    R0.setEntry(i, j, 1);
                } else {
                    R0.setEntry(i, j, 0);
                }
            }
        }
        return R0;
    }

    private static RealMatrix ones(int a, int b) {
        RealMatrix R0 = new Array2DRowRealMatrix().createMatrix(a, b);
        for (int i = 0; i < a; ++i) {
            for (int j = 0; j < b; ++j) {
                R0.setEntry(i, j, 1);
            }
        }
        return R0;
    }

    private static RealMatrix zeros(int a, int b) {
        RealMatrix R0 = new Array2DRowRealMatrix().createMatrix(a, b);
        for (int i = 0; i < a; ++i) {
            for (int j = 0; j < b; ++j) {
                R0.setEntry(i, j, 0);
            }
        }
        return R0;
    }

    private static RealMatrix uminus(RealMatrix matrix) {
        for (int i = 0; i < matrix.getRowDimension(); ++i) {
            for (int j = 0; j < matrix.getColumnDimension(); ++j) {
                matrix.setEntry(i, j, -matrix.getEntry(i, j));
            }
        }
        return matrix;
    }

    public double mapObs(Set<Variable> ps) {
        int l = ps.size();
        int m = obsNum();
        double nu0 = 1;
        double sigma2_0 = 1;

        double nun = nu0 + m;

        double T0 = nu0 * sigma2_0 / 2;

        RealMatrix R0 = eye(l + 1);
        RealMatrix beta = zeros(l + 1, 1);

        RealMatrix X;
        if (l == 0) {
            X = ones(m, 1);
        } else {
            RealMatrix X2;
            X2 = new Array2DRowRealMatrix().createMatrix(m, l);
            int iter = 0;
            for (Variable v : ps) {
                assert v.data.size() == m;
                for (int i = 0; i < m; ++i) {
                    X2.setEntry(i, iter, v.data.get(i));
                }
                ++iter;
            }
            RealMatrix X1 = ones(m, 1);
            X = new Array2DRowRealMatrix().createMatrix(m, 1 + l);
            for (int i = 0; i < m; ++i) {
                for (int j = 0; j < 1; ++j) {
                    X.setEntry(i, j, X1.getEntry(i, j));
                }
            }

            for (int i = 0; i < m; ++i) {
                for (int j = 0; j < l; ++j) {
                    X.setEntry(i, 1 + j, X2.getEntry(i, j));
                }
            }
        }

        RealMatrix Y = new Array2DRowRealMatrix().createMatrix(m, 1);
        for (int i = 0; i < m; ++i) {
            Y.setEntry(i, 0, this.data.get(i));
        }

        RealMatrix Rn = R0.add(X.transpose().multiply(X));
        RealMatrix Rinv = inverse(Rn);

        double alfa2 = 2d / (nu0 * sigma2_0);

        RealMatrix betan = Rinv.multiply(R0.multiply(beta).add(X.transpose().multiply(Y)));
        double inv_alfa2n = (uminus(betan.transpose().multiply(Rn).multiply(betan))
                .add(Y.transpose().multiply(Y))
                .add(beta.transpose().multiply(R0).multiply(beta)))
                .getEntry(0, 0) / 2 + 1d / alfa2;
        double alfa2n = 1d / inv_alfa2n;
        double sigma2 = 2d / (nun * alfa2n);
        double Tm = nun * sigma2 / 2;

        return -(double)m / 2 * Math.log(2 * Math.PI) +
                1d / 2 * (Math.log(new LUDecomposition(R0).getDeterminant())
                        - Math.log(new LUDecomposition(Rn).getDeterminant()))
                + c(nun / 2)
                - c(nu0 / 2)
                + (nu0 / 2) * Math.log(T0)
                - (nun / 2) * Math.log(Tm);
    }

    public int[] mapObsAnd(Set<Variable> ps) {
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

    public int[] mapObsNoMemAnd(List<Variable> ps, Variable extra, int[] arrOut, int[] arrTmp) {
        int m = obsNum();
        int ps_size = ps.size();
        for (int i = 0; i < ps_size; i++) {
            arrTmp[i] = ps.get(i).cardinality();
        }
        arrTmp[ps_size] = extra.cardinality();
        arrTmp[ps_size + 1] = 1;

        Trie t = new Trie(arrTmp);

        Trie.Selector selector = t.selector();
        for (int i = 0; i < m; i++) {
            selector.reuse();
            for (Variable p : ps) {
                selector.choose(p.discreteValue(orderedObs[i]) - 1);
            }
            selector.choose(extra.discreteValue(orderedObs[i]) - 1);

            arrOut[i] = selector.get();
        }
        return arrOut;
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
}

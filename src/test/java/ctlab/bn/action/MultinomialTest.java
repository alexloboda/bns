package ctlab.bn.action;

import org.apache.commons.math3.special.Beta;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MultinomialTest {
    private static final int NCHOICES = 20000;
    private static final int NVARIABLES = 10;
    private static final int N_DISABLE_ACTIONS = 100;
    private static final int LOG_STEPS = 8;
    private static final int N_VARIABLES_COMPLEX = 32;
    private static final int ACTIONS_COMPLEX = 30;

    private SplittableRandom re;

    private double initialLL;
    private double[] psOriginal;
    private double[] ps;
    private boolean[] disabled;

    Function<Integer, Double> calcLL;

    public MultinomialTest() {
        init(NVARIABLES);
    }

    private void init(int vars) {
        re = new SplittableRandom(42);

        initialLL = Math.log(1.0 / vars);
        ps = re.doubles(vars).toArray();
        psOriginal = Arrays.copyOf(ps, ps.length);
        disabled = new boolean[vars];

        calcLL = i -> Math.log(psOriginal[i]);
    }

    private void resetStructures() {
        ps = Arrays.copyOf(psOriginal, psOriginal.length);
        Arrays.fill(disabled, false);
    }

    private double binomialCDF(double k, double n, double p) {
        if (k >= n) {
            return 1.0;
        }
        if (k <= 0.0) {
            return 0.0;
        }
        return Beta.regularizedBeta(1 - p, n - k, k + 1);
    }

    private double binomialTest(int k, int n, double p) {
        double mean = p * n;
        double pos = k;
        if (k > mean) {
            pos = mean - (k - mean);
        }
        double lower = binomialCDF(pos, n, p);
        double term = k < mean ? -1.0 : 0.0;
        double higher = binomialCDF(mean + (mean - pos) + term, n, p);
        return Math.min(1.0, 1.0 - (higher - lower));
    }

    private void assertComparable(double[] ps, int[] hits) {
        double psSum = Arrays.stream(ps).sum();
        int sum = Arrays.stream(hits).sum();
        StringBuilder msg = new StringBuilder("\nn = " + sum + "\n");
        for (int i = 0; i < ps.length; i++) {
            msg.append("Variable ")
                    .append(i)
                    .append(": expected p = ").append(ps[i] / psSum)
                    .append("; actual p = ").append((double) hits[i] / sum)
                    .append("; binomial two-tailed test p = ").append(binomialTest(hits[i], sum, ps[i] / psSum))
                    .append("; hits = ").append(hits[i])
                    .append("\n");
        }
        for (int i = 0; i < ps.length; i++) {
            Assert.assertTrue(msg.toString(), binomialTest(hits[i], sum, ps[i] / psSum) > 1e-4);
        }
    }

    private void testMultinomial(Multinomial mult, double[] ps) {
        int hits[] = new int[ps.length];
        for (int i = 0; i < NCHOICES; i++) {
            Short choice = mult.randomAction();
            if (choice != null) {
                ++hits[choice];
            }
        }

        assertComparable(ps, hits);
    }

    @Test
    public void multinomialTest() {
        double initialLL = Math.log(1.0 / 9);
        double[] ps = {1.0, 1.0, 0.1, 0.1, 1.0, 0.1, 1.0, 0.5, 0.5};
        double[] lls = Arrays.stream(ps).map(Math::log).toArray();

        Function<Integer, Double> calcLL = i -> lls[i];

        for (int bs = 1; bs <= NVARIABLES; bs++) {
            for (short cacheSize = 0; cacheSize <= NVARIABLES; cacheSize++) {
                Multinomial mult = new Multinomial(ps.length, bs, cacheSize, calcLL, initialLL, re);
                testMultinomial(mult, ps);
            }
        }
    }

    @Test
    public void disableActionsTest() {
        SplittableRandom re = new SplittableRandom(42);

        Multinomial multinomial = new Multinomial(NVARIABLES, 3, 2, calcLL, initialLL, re);

        for (int i = 0; i < N_DISABLE_ACTIONS; i++) {
            short var = (short)re.nextInt(NVARIABLES);
            if (!disabled[var]) {
                ps[var] = 0.0;
                multinomial.disableAction(var, calcLL.apply((int)var) + initialLL);
            } else {
                ps[var] = psOriginal[var];
                multinomial.reEnableAction(var);
            }
            disabled[var] = !disabled[var];
            testMultinomial(multinomial, ps);
        }
        resetStructures();
    }

    @Test
    public void earlyPhaseMultinomialTest() {
        init(N_VARIABLES_COMPLEX);

        SplittableRandom re = new SplittableRandom(42);
        Random rnd = new Random(42);

        List<Integer> changeOrder = re.ints(0, N_VARIABLES_COMPLEX)
                .limit(ACTIONS_COMPLEX)
                .boxed()
                .collect(Collectors.toList());

        for (int i = 0; i < LOG_STEPS; i++) {
            long steps = Math.round(Math.exp(i));
            int[] hits = new int[N_VARIABLES_COMPLEX];
            Collections.shuffle(changeOrder, rnd);
            int[] actionStep = re.ints(0, (int)steps).limit(ACTIONS_COMPLEX).toArray();
            boolean[] disabled = new boolean[N_VARIABLES_COMPLEX];

            for (int j = 0; j < NCHOICES; j++) {
                Multinomial mult = new Multinomial(N_VARIABLES_COMPLEX, 4, 10, calcLL,
                                                   Math.log(1.0 / N_VARIABLES_COMPLEX), re);
                for (int k = 0; k < steps; k++) {
                    for (int l = 0; l < ACTIONS_COMPLEX; l++) {
                        if (actionStep[l] == k) {
                            int toChange = changeOrder.get(l);
                            if (disabled[toChange]) {
                                ps[toChange] = psOriginal[toChange];
                                mult.reEnableAction((short)toChange);
                            }  else {
                                ps[toChange] = 0.0;
                                mult.disableAction((short)toChange, calcLL.apply(toChange) - initialLL);
                            }
                            disabled[toChange] = !disabled[toChange];
                        }
                    }
                    mult.randomAction();
                }
                Short action = mult.randomAction();
                if (action != null) {
                    ++hits[action];
                }
            }

            assertComparable(ps, hits);
        }
    }
}

package ctlab.bn.action;

import org.apache.commons.math3.special.Beta;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.function.Function;

public class MultinomialTest {
    public static final int NCHOICES = 10000;
    public static final int NVARIABLES = 10;
    public static final int N_DISABLE_ACTIONS = 100;

    private double binomialCDF(int k, int n, double p) {
        if (k == n) {
            return 1.0;
        }
        return Beta.regularizedBeta(1 - p, (double)(n - k), (double)(k + 1));
    }

    private double binomialTest(int k, int n, double p) {
        return 1.0 - (binomialCDF(n - k, n, p) - binomialCDF(k, n, p));
    }

    private void testMultinomial(Multinomial mult, double[] ps) {
        double psSum = Arrays.stream(ps).sum();
        int hits[] = new int[ps.length];
        for (int i = 0; i < NCHOICES; i++) {
            Short choice = mult.randomAction();
            if (choice != null) {
                ++hits[choice];
            }
        }

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

    @Test
    public void multinomialTest() {
        SplittableRandom re = new SplittableRandom(42);

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

        double initialLL = Math.log(1.0 / NVARIABLES);
        double[] ps = re.doubles(NVARIABLES).toArray();
        double[] ps_original = Arrays.copyOf(ps, ps.length);
        double[] lls = Arrays.stream(ps).map(Math::log).toArray();
        boolean[] disabled = new boolean[NVARIABLES];

        Function<Integer, Double> calcLL = i -> lls[i];

        Multinomial multinomial = new Multinomial(NVARIABLES, 3, 2, calcLL, initialLL, re);

        for (int i = 0; i < N_DISABLE_ACTIONS; i++) {
            short var = (short)re.nextInt(NVARIABLES);
            if (!disabled[var]) {
                ps[var] = 0.0;
                multinomial.disableAction(var, lls[var] + initialLL);
            } else {
                ps[var] = ps_original[var];
                multinomial.reEnableAction(var);
            }
            disabled[var] = !disabled[var];
            testMultinomial(multinomial, ps);
        }
    }
}

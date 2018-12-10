package ctlab.bn.action;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.function.Function;

public class MultinomialTest {
    private final static int NCHOICES = 200000;

    @Test
    public void multinomialTest() {
        SplittableRandom re = new SplittableRandom(42);

        double initialLL = Math.log(1.0 / 9);
        double[] ps = {1.0, 1.0, 0.1, 0.1, 1.0, 0.1, 1.0, 0.5, 0.5};
        double psSum = Arrays.stream(ps).sum();
        double[] lls = Arrays.stream(ps).map(Math::log).toArray();


        Function<Integer, Double> calcLL = i -> lls[i];

        Multinomial multinomial = new Multinomial(9, 5, (short)4, calcLL, initialLL, re);
        int[] hits = new int[9];

        for (int i = 0; i < NCHOICES; i++) {
            Short choice = multinomial.randomAction();
            if (choice != null) {
                ++hits[choice];
            }
        }

        int sum = Arrays.stream(hits).sum();
        double[] fs = Arrays.stream(hits).mapToDouble(x -> (double)x / sum).toArray();
        for (double d: fs) {
            System.out.println(d);
        }
        for (int i = 0; i < ps.length; i++) {
            Assert.assertEquals(ps[i] / psSum, fs[i], 0.01);
        }
    }
}

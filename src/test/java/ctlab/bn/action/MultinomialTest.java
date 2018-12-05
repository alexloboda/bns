package ctlab.bn.action;

import org.junit.Test;

import java.util.SplittableRandom;
import java.util.function.Function;

import static org.junit.Assert.*;

public class MultinomialTest {
    @Test
    public void multinomialTest() {
        SplittableRandom re = new SplittableRandom(42);

        double initialLL = Math.log(1.0 / 9);
        double[] lls = {0.1, 0.1, 0.01, 0.01, 0.1, 0.01, 0.1, 0.09, 0.09};

        Function<Integer, Double> calcLL = i -> lls[i];

        Multinomial multinomial = new Multinomial(9, 5, 4, calcLL, initialLL);

        for (int i = 0; i < 1000; i++) {
            multinomial.randomAction(re);
        }

    }
}

package ctlab.mcmc;

import ctlab.bn.BayesianNetwork;
import ctlab.bn.Variable;
import ctlab.bn.action.MultinomialFactory;
import ctlab.bn.sf.BDE;
import ctlab.bn.sf.ScoringFunction;
import org.apache.commons.math3.util.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.SplittableRandom;

public class ModelTest {
    @Test
    public void test() {
        ScoringFunction sf = new BDE();

        Variable var1 = new Variable("VAR1", Arrays.asList(), 2, null);
        Variable var2 = new Variable("VAR2", Arrays.asList(), 2, null);
        Variable var3 = new Variable("VAR3", Arrays.asList(), 2, null);
        BayesianNetwork bn = new BayesianNetwork(Arrays.asList(var1, var2, var3));

        SplittableRandom sr = new SplittableRandom(42);
        Model model = new Model(bn, sf, sr,
                false, true,
                new MultinomialFactory(2, 3, 1, sr),
                0);
        model.run();
        while(model.steps() < 1000000) {
            model.step();
        }
        model.finish();
        double[][] fs = model.frequencies();
        double[][] expectedFs = {{}, {}, {}};
        for (int i = 0; i < 3; i++) {
            Assert.assertArrayEquals(expectedFs[i], fs[i], 1e-4);
        }
    }

    private Pair<double[][], Double> exactSolve(BayesianNetwork bn, ScoringFunction sf, int v, int u) {
        if (u >= bn.size()) {
            return exactSolve(bn, sf, v + 1, u + 2);
        }
        double[][] res = new double[bn.size()][bn.size()];
        if (v == bn.size()) {
            double score = 0.0;
            for (int i = 0; i < bn.size(); i++) {
                score += bn.score(i, sf);
            }
            for (int i = 0; i < bn.size(); i++) {
                for (int j = 0; j < bn.size(); j++) {
                    res[i][j] = bn.edgeExists(i, j) ? 1.0 : 0.0;
                }
            }
            return new Pair<>(res, score);
        }
        BayesianNetwork bnWithEdge = new BayesianNetwork(bn);
        bnWithEdge.addEdge(v, u);
        Pair<double[][], Double> resWOEdge = exactSolve(bn, sf, v, u + 1);
        Pair<double[][], Double> resWithEdge = exactSolve(bnWithEdge, sf, v, u + 1);
        double sum = resWithEdge.getSecond() + resWOEdge.getSecond();
        double k1 = resWOEdge.getSecond() / sum;
        double k2 = resWithEdge.getSecond() / sum;
        for (int i = 0; i < bn.size(); i++) {
            for (int j = 0; j < bn.size(); j++) {
                res[i][j] = resWOEdge.getFirst()[i][j] * k1 + resWithEdge.getFirst()[i][j] * k2;
            }
        }
        return new Pair<>(res, sum);
    }
}

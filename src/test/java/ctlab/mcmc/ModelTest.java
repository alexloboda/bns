package ctlab.mcmc;

import ctlab.bn.BayesianNetwork;
import ctlab.bn.Variable;
import ctlab.bn.action.MultinomialFactory;
import ctlab.bn.sf.BDE;
import ctlab.bn.sf.ScoringFunction;
import org.apache.commons.math3.util.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class ModelTest {
    @Test
    public void test() {
        Random random = new Random(0xC0FFEE);
        ScoringFunction sf = new BDE();
        int sampleSize = 750;

        List<Double> data1 = random.doubles(sampleSize).boxed().collect(Collectors.toList());
        List<Double> data2 = random.doubles(sampleSize).boxed().collect(Collectors.toList());
        List<Double> data3 = new ArrayList<>();

        for (int i = 0; i < sampleSize; i++) {
            if (data1.get(i) > 2.0 / 3.0 && data2.get(i) > 2.0 / 3.0) {
                data3.add(random.nextDouble() / 3.0);
            } else if (data1.get(i) < 1.0 / 3.0 && data2.get(i) < 1.0 / 3.0) {
                data3.add(0.6 + random.nextDouble() / 3.0);
            } else {
                data3.add(random.nextDouble());
            }
        }

        Variable var1 = new Variable("VAR1", data1, 3, Variable.DiscretizationPrior.UNIFORM);
        Variable var2 = new Variable("VAR2", data2, 3, Variable.DiscretizationPrior.UNIFORM);
        Variable var3 = new Variable("VAR3", data3, 3, Variable.DiscretizationPrior.UNIFORM);

        BayesianNetwork bn = new BayesianNetwork(Arrays.asList(var1, var2, var3));

        SplittableRandom sr = new SplittableRandom(42);
        double[][] actual = new double[bn.size()][bn.size()];

        int models = 2000;

        for (int i = 0; i < models; i++) {
            Model model = new Model(bn, sf, sr,
                    false, true,
                    new MultinomialFactory(2, 1, 1, sr),
                    2);
            model.run();
            while (model.steps() < 500) {
                model.step();
            }
            double[][] fs = model.adjMatrix();
            for (int v = 0; v < bn.size(); v++) {
                for (int u = 0; u < bn.size(); u++) {
                    actual[v][u] += fs[v][u] / models;
                }
            }
            model.finish();
        }
        double[][] expectedFs = exactSolve(new BayesianNetwork(bn), sf, 0, 1).getFirst();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.err.print(expectedFs[i][j] + "\t");
            }
            System.err.println();
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.err.print(actual[i][j] + "\t");
            }
            System.err.println();
        }
        for (int i = 0; i < 3; i++) {
            Assert.assertArrayEquals(expectedFs[i], actual[i], 0.025);
        }
    }

    private double likelihoodsSum(double ll1, double ll2) {
        double maxLL = Math.max(ll1, ll2);
        ll1 -= maxLL;
        ll2 -= maxLL;
        return Math.log(Math.exp(ll1) + Math.exp(ll2)) + maxLL;
    }

    private Pair<double[][], Double> exactSolve(BayesianNetwork bn, ScoringFunction sf, int v, int u) {
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

        if (u >= bn.size()) {
            return exactSolve(bn, sf, v + 1, 0);
        }

        if (v == u || bn.pathExists(u, v)) {
            return exactSolve(bn, sf, v, u + 1);
        }

        BayesianNetwork bnWithEdge = new BayesianNetwork(bn);
        bnWithEdge.addEdge(v, u);
        Pair<double[][], Double> resWOEdge = exactSolve(bn, sf, v, u + 1);
        Pair<double[][], Double> resWithEdge = exactSolve(bnWithEdge, sf, v, u + 1);
        double sum = likelihoodsSum(resWithEdge.getSecond(), resWOEdge.getSecond());
        double k1 = Math.exp(resWOEdge.getSecond() - sum);
        double k2 = Math.exp(resWithEdge.getSecond() - sum);
        for (int i = 0; i < bn.size(); i++) {
            for (int j = 0; j < bn.size(); j++) {
                res[i][j] = resWOEdge.getFirst()[i][j] * k1 + resWithEdge.getFirst()[i][j] * k2;
            }
        }
        return new Pair<>(res, sum);
    }
}

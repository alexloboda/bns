package ctlab.mc5;

import ctlab.mc5.bn.Variable;
import ctlab.mc5.bn.sf.ScoringFunction;
import picocli.CommandLine.*;

import java.io.File;

interface Parameters {
    @Option(names = "--main-sf", defaultValue = "BDE 1")
    ScoringFunction mainSF();

    @Option(names = "--discretization-sf", defaultValue = "BDE 1")
    ScoringFunction discSF();

    @Option(names = "--preranking")
    File preranking();

    @Option(names = {"-g", "--gene-expression-table"}, required = true)
    File geneExpressionFile();

    @Option(names = {"-o", "-output"}, required = true)
    File output();

    @Option(names = "--initial-classes", defaultValue = "3")
    int defaultCls();

    @Option(names = "--preranking-limit", defaultValue = "8")
    int prerankingLimit();

    @Option(names = {"-p", "--discretization-prior"}, defaultValue = "MULTINOMIAL")
    Variable.DiscretizationPrior discPrior();

    @Option(names = "--optimal-bn-runs", defaultValue = "0")
    int nOptimizer();

    @Option(names = "--disc-lb", defaultValue = "3")
    int discLB();

    @Option(names = "--disc-ub", defaultValue = "3")
    int discUB();

    @Option(names = "--seed", defaultValue = "42")
    int seed();

    @Option(names = {"-gs", "-golden-standard"})
    File gold();
}

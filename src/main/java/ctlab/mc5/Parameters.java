package ctlab.mc5;

import picocli.CommandLine;

@CommandLine.Command(mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
        resourceBundle = "ctlab.mc5.Parameters")
class Parameters {
    //private final ScoringFunction mainSF;
    //private final ScoringFunction discSF;

    //private File preranking;
    //private final File geneExpressionFile;
    //private final File output;

    //private final int nSteps;
    //private final int executions;
    //private final int nThreads;
    //private final int defaultCls;

    //private final int prerankingLimit;
    //private final Variable.DiscretizationPrior discPrior;
    //private final Integer nOptimizer;
    //private Integer discLB;
    //private Integer discUB;

    Parameters() {
        //OptionParser optionParser = new OptionParser();
        //optionParser.allowsUnrecognizedOptions();
        //optionParser.acceptsAll(asList("h", "help"), "Print a short help message");
        //OptionSet optionSet = optionParser.parse(args);

        //OptionSpec<String> ge = optionParser.acceptsAll(asList("g", "gene-expression-table"),
        //        "gene expression data file").withRequiredArg().ofType(String.class).required();
        //OptionSpec<Integer> steps = optionParser.acceptsAll(asList("s", "steps"),
        //        "Number of steps for each init").withRequiredArg().ofType(Integer.class).required();
        //OptionSpec<Integer> execs = optionParser.acceptsAll(asList("r", "runs"),
        //        "Number of independent runs").withRequiredArg().ofType(Integer.class).defaultsTo(1);
        //OptionSpec<Integer> defaultClasses = optionParser.acceptsAll(asList("c", "classes"),
        //        "Default number of classes").withRequiredArg().ofType(Integer.class).defaultsTo(3);
        //OptionSpec<String> outfile = optionParser.acceptsAll(asList("o", "out"),
        //        "output file").withRequiredArg().ofType(String.class).required();
        //OptionSpec<Integer> cores = optionParser.acceptsAll(asList("m", "threads"),
        //        "number of cores").withRequiredArg().ofType(Integer.class).defaultsTo(1);
        //OptionSpec<String> mainSFOpt = optionParser.accepts("main-sf",
        //        "scoring function used in main algorithm").withRequiredArg().ofType(String.class).defaultsTo("BDE 1");
        //OptionSpec<String> discSFOpt = optionParser.accepts("disc-sf",
        //        "SF used in discretization").withRequiredArg().ofType(String.class).defaultsTo("BDE 1");
        //OptionSpec<Integer> optimizerStepsOpt = optionParser.accepts("n-optimizer",
        //        "Number of optimizer steps").withRequiredArg().ofType(Integer.class).defaultsTo(0);

        //OptionSpec<String> prerankingOpt = optionParser.accepts("preranking",
        //        "Preranking for preprocessing").withRequiredArg().ofType(String.class);
        //OptionSpec<Integer> discLBOpt = optionParser.accepts("disc-lb",
        //        "discretization minimum class size").withRequiredArg().ofType(Integer.class);
        //OptionSpec<Integer> discUBOpt = optionParser.accepts("disc-ub",
        //        "discretization maximum class size").withRequiredArg().ofType(Integer.class);
        //OptionSpec<Integer> prerankLimitOpt = optionParser.accepts("preranking-limit",
        //        "preranking limit on preprocessing").withRequiredArg().ofType(Integer.class).defaultsTo(7);
        //OptionSpec<String> discPriorOpt = optionParser.accepts("disc-prior",
        //        "discretization priors(UNIFORM, EXP, MULTINOMIAL)").withRequiredArg().ofType(String.class)
        //        .defaultsTo("EXP");

        //if (optionSet.has("h")) {
        //    optionParser.printHelpOn(System.out);
        //    System.exit(0);
        //}

        //optionSet = optionParser.parse(args);
        //nSteps = optionSet.valueOf(steps);
        //executions = optionSet.valueOf(execs);
        //nThreads = optionSet.valueOf(cores);
        //defaultCls = optionSet.valueOf(defaultClasses);

        //geneExpressionFile = new File(optionSet.valueOf(ge));
        //output = new File(optionSet.valueOf(outfile));
        //if (optionSet.has(discLBOpt)) {
        //    discLB = optionSet.valueOf(discLBOpt);
        //}
        //if (optionSet.has(discUBOpt)) {
        //    discUB = optionSet.valueOf(discUBOpt);
        //}
        //mainSF = ScoringFunction.parse(optionSet.valueOf(mainSFOpt));
        //discSF = ScoringFunction.parse(optionSet.valueOf(discSFOpt));
        //discPrior = Variable.DiscretizationPrior.valueOf(optionSet.valueOf(discPriorOpt).toUpperCase());
        //nOptimizer = optionSet.valueOf(optimizerStepsOpt);
        //prerankingLimit = optionSet.valueOf(prerankLimitOpt);
        //if (optionSet.has(prerankingOpt)) {
        //    preranking = new File(optionSet.valueOf(prerankingOpt));
        //    if (!preranking.exists()) {
        //        throw new FileNotFoundException("Preranking does not exist");
        //    }
        //}
    }

    //public ScoringFunction mainSF() {
    //    return mainSF;
    //}

    //public ScoringFunction discSF() {
    //    return discSF;
    //}

    //public int nSteps() {
    //    return nSteps;
    //}

    //public int executions() {
    //    return executions;
    //}

    //public int nThreads() {
    //    return nThreads;
    //}

    //public int defaultNumberOfClasses() {
    //    return defaultCls;
    //}

    //public int prerankingLimit() {
    //    return prerankingLimit;
    //}

    //public Variable.DiscretizationPrior discretizationPrior() {
    //    return discPrior;
    //}

    //public Integer optimizerSteps() {
    //    return nOptimizer;
    //}

    //public Integer discretizationLowerBound() {
    //    return discLB;
    //}

    //public Integer discretizationUpperBound() {
    //    return discUB;
    //}

    //public File outputFile() {
    //    return output;
    //}

    //public File geneExpressionDataFile() {
    //    return geneExpressionFile;
    //}

    //public File prerankingFile() {
    //    return preranking;
    //}
}

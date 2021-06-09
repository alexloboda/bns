package ctlab.mc5;

import ctlab.mc5.bn.BayesianNetwork;
import ctlab.mc5.bn.Variable;
import ctlab.mc5.bn.sf.ScoringFunction;
import ctlab.mc5.mcmc.EdgeList;
import ctlab.mc5.mcmc.EstimatorParams;
import ctlab.mc5.mcmc.NetworkEstimator;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Command(mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
        resourceBundle = "ctlab.mc5.Parameters")
public class SparkMain {
    private static final String ESTIMATOR_PARAMS = "EstimatorParams";
    private static final String MAIN_PARAMS = "MainParams";

    private Parameters params;
    private BayesianNetwork bn;

    private List<Variable> parseGETable(File file) throws FileNotFoundException {
        List<Variable> res = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<List<Double>> data = new ArrayList<>();
        try (Scanner sc = new Scanner(file).useLocale(Locale.US)) {
            String firstLine = sc.nextLine();
            Scanner line_sc = new Scanner(firstLine).useLocale(Locale.US);
            while (line_sc.hasNext()) {
                names.add(line_sc.next());
            }

            int n = names.size();
            for (int i = 0; i < n; i++) {
                data.add(new ArrayList<>());
            }
            while (sc.hasNext()) {
                for (int i = 0; i < n; i++) {
                    data.get(i).add(sc.nextDouble());
                }
            }

            for (int i = 0; i < n; i++) {
                res.add(new Variable(names.get(i), data.get(i), params.defaultCls(), i));
            }
        }
        return res;
    }

    public static void main(String[] args) {
        SparkMain app = new SparkMain();
        CommandLine cmd = new CommandLine(app);
        cmd.registerConverter(ScoringFunction.class, ScoringFunction::parse);
        cmd.addMixin(ESTIMATOR_PARAMS, EstimatorParams.class);
        cmd.addMixin(MAIN_PARAMS, Parameters.class);
        try {
            cmd.parse(args);
            if (cmd.isUsageHelpRequested()) {
                cmd.usage(System.out);
                return;
            } else if (cmd.isVersionHelpRequested()) {
                cmd.printVersionHelp(System.out);
                return;
            }
            Map<String, Object> mixins = cmd.getMixins();
            app.run((Parameters) mixins.get(MAIN_PARAMS), (EstimatorParams) mixins.get(ESTIMATOR_PARAMS));
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            if (!UnmatchedArgumentException.printSuggestions(ex, System.err)) {
                ex.getCommandLine().usage(System.err);
            }
        } catch (Exception ex) {
            throw new ExecutionException(cmd, "Runtime error: ", ex);
        }
    }

    private static int maxMin(int min, int max, int actual) {
        return Math.min(max, Math.max(min, actual));
    }

    private void printResultsToOutput(EdgeList edges, PrintWriter pw) {
        for (EdgeList.Edge e : edges.edges()) {
            pw.println(bn.var(e.v()).getName() + "\t" + bn.var(e.u()).getName() + "\t" + e.p(edges.get_number_merged()));
        }
    }

    private synchronized void writeResults(EdgeList results) {
        try (PrintWriter pw = new PrintWriter(params.output())) {
            printResultsToOutput(results, pw);
        } catch (IOException e) {
            try {
                File tmp = File.createTempFile("bn_inference_", "");
                try (PrintWriter pw = new PrintWriter(tmp)) {
                    pw.println("Can't write to specified output file. Created temp file " + tmp);
                    printResultsToOutput(results, pw);
                }
            } catch (IOException e1) {
                try (PrintWriter pw = new PrintWriter(System.out)) {
                    pw.println("Can't write to specified output file. Failed to write to temp file. Output: ");
                    printResultsToOutput(results, pw);
                }
            }
        }
    }

    private void run(Parameters params, EstimatorParams estimatorParams) throws IOException {
        this.params = params;
        final List<Variable> genes = parseGETable(params.geneExpressionFile());

        for (Variable v : genes) {
            int lb = maxMin(2, v.obsNum(), params.discLB());
            int ub = maxMin(2, v.obsNum(), params.discUB());
            if (lb > ub) {
                int tmp = lb;
                lb = ub;
                ub = tmp;
            }
            v.setDiscLimits(lb, ub);
        }

        if (params.sparkMaster() == null) {
            System.out.println("Spark Master not specified:");
            System.out.println("--spark-site=<site>");
            return;
        }
        if (params.jar() == null) {
            System.out.println("Jar not specified:");
            System.out.println("--jar=<file>");
            return;
        }

        SparkConf sparkConf = new SparkConf().setMaster(params.sparkMaster()).setAppName("BNS").setJars(new String[]{params.jar().getAbsolutePath()});

        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        final ScoringFunction sf = params.mainSF();
        final int chains = estimatorParams.chains();
        final int batchSize = estimatorParams.batchSize();
        final int mainCacheSize = estimatorParams.mainCacheSize();
        final int numberOfCachedStates = estimatorParams.numberOfCachedStates();
        final int multipleCollectors = estimatorParams.multipleCollectors();
        final long swapPeriod = estimatorParams.swapPeriod();
        final long coldChainSteps = estimatorParams.coldChainSteps();
        final long warmup = estimatorParams.warmup();
        final double powerBase = estimatorParams.powerBase();

        bn = new BayesianNetwork(genes, sf);

        JavaRDD<EdgeList> RddTasks =
                sparkContext.parallelize(IntStream.range(0, estimatorParams.nRuns()).boxed().collect(Collectors.toList()))
                        .repartition(estimatorParams.nRuns())
                        .map(integer -> {
                                    final NetworkEstimator.Task task = new NetworkEstimator.Task(new BayesianNetwork(genes, sf),
                                            null,
                                            new SplittableRandom(),
                                            chains,
                                            batchSize,
                                            mainCacheSize,
                                            numberOfCachedStates,
                                            multipleCollectors,
                                            swapPeriod,
                                            coldChainSteps,
                                            warmup,
                                            powerBase);
                                    task.run();
                                    return task.getResult();
                                }
                        );
        EdgeList resultData = RddTasks.reduce(EdgeList::merge);
        writeResults(resultData);
    }
}
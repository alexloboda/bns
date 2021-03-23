package ctlab.mc5;

import ctlab.mc5.bn.BayesianNetwork;
import ctlab.mc5.bn.Variable;
import ctlab.mc5.bn.sf.ScoringFunction;
import ctlab.mc5.graph.Graph;
import ctlab.mc5.mcmc.EdgeList;
import ctlab.mc5.mcmc.EstimatorParams;
import ctlab.mc5.mcmc.Task;
import org.apache.spark.SparkConf;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
        try (Scanner sc = new Scanner(file)) {
            String firstLine = sc.nextLine();
            Scanner line_sc = new Scanner(firstLine);
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
                res.add(new Variable(names.get(i), data.get(i), params.defaultCls(),
                        params.discPrior()));
            }
        }

        return res;
    }

    private Graph parseBound(BayesianNetwork bn) throws FileNotFoundException {
        Graph g = new Graph(bn.size());
        try (Scanner scanner = new Scanner(params.preranking())) {
            while (scanner.hasNext()) {
                int v = bn.getID(scanner.next());
                int u = bn.getID(scanner.next());
                scanner.next();
                if (g.inDegree(u) < params.prerankingLimit()) {
                    g.addEdge(v, u);
                }
            }
        }
        return g;
    }

    public static void main(String[] args) {
        SparkMain app = new SparkMain();
        CommandLine cmd = new CommandLine(app);
        cmd.registerConverter(Variable.DiscretizationPrior.class, Variable.DiscretizationPrior::valueOf);
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
            pw.println(bn.var(e.v()).getName() + "\t" + bn.var(e.u()).getName() + "\t" + e.p());
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
        List<Variable> genes = parseGETable(params.geneExpressionFile());

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

        SparkConf sparkConf = new SparkConf().setMaster(params.sparkMaster()).setAppName("BNS").setJars(new String[]{params.libsDir() + "/grn.jar"});
        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);
        List<Integer> runned = new ArrayList<>();
        for (int i = 0; i < estimatorParams.nRuns(); ++i) {
            runned.add(i);
        }
        final ScoringFunction sf = params.mainSF();


        final int chains = estimatorParams.chains();
        final int batchSize = estimatorParams.batchSize();
        final int mainCacheSize = estimatorParams.mainCacheSize();
        final int numberOfCachedStates = estimatorParams.numberOfCachedStates();
        final double deltaT = estimatorParams.deltaT();
        final long swapPeriod = estimatorParams.swapPeriod();
        final long coldChainSteps = estimatorParams.coldChainSteps();
        final double powerBase = estimatorParams.powerBase();

        bn = new BayesianNetwork(genes, sf);
        JavaRDD<Integer> RddInt = sparkContext.parallelize(runned);

        JavaRDD<Task> RddTasks = RddInt.map(integer -> new Task(new BayesianNetwork(genes, sf),
                chains,
                batchSize,
                mainCacheSize,
                numberOfCachedStates,
                deltaT,
                swapPeriod,
                coldChainSteps,
                powerBase));
        JavaRDD<EdgeList> RDDedges = RddTasks.map(Task::run);

        EdgeList resultData = RDDedges.reduce(EdgeList::mergeWithRet);
        writeResults(resultData);
    }
}
package ctlab.mc5;

import ctlab.mc5.bn.BayesianNetwork;
import ctlab.mc5.bn.Variable;
import ctlab.mc5.bn.sf.ScoringFunction;
import ctlab.mc5.graph.Graph;
import ctlab.mc5.mcmc.EdgeList;
import ctlab.mc5.mcmc.EstimatorParams;
import ctlab.mc5.mcmc.NetworkEstimator;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
        resourceBundle = "ctlab.mc5.Parameters")
public class Main {
    private static final String ESTIMATOR_PARAMS = "EstimatorParams";
    private static final String MAIN_PARAMS = "MainParams";

    private Parameters params;
    private boolean completed;
    private NetworkEstimator estimator;
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

    private Graph parseBound(BayesianNetwork bn) throws FileNotFoundException {
        Graph g = new Graph(bn.size());
        try (Scanner scanner = new Scanner(params.preranking())) {
            while (scanner.hasNext()) {
                int from = bn.getID(scanner.next());
                int to = bn.getID(scanner.next());
                scanner.next();
                if (g.inDegree(to) < params.prerankingLimit()) {
                    g.addEdge(from, to);
                }
            }
        }
        return g;
    }

    public static void main(String[] args) {
        Main app = new Main();
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

    private synchronized void writeResults(OutputStreamWriter outputStreamWriter) {
        if (completed || estimator == null) {
            return;
        }
        EdgeList results = estimator.resultsFromCompletedTasks();
        try (PrintWriter pw = new PrintWriter(outputStreamWriter)) {
            printResultsToOutput(results, pw);
        }
    }

    private void printParameters(Parameters params, EstimatorParams estimatorParams) {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        System.out.println("Parameters:");
        System.out.println("geneExpressionFile = " + params.geneExpressionFile().getPath());
        if (params.gold() != null)
            System.out.println("gold = " + params.gold().getPath());
        System.out.println("threads = " + estimatorParams.nThreads());
        System.out.println("runs = " + estimatorParams.nRuns());
        System.out.println("chains = " + estimatorParams.chains());
        System.out.println("cached-states = " + estimatorParams.numberOfCachedStates());
        System.out.println("batch-size = " + estimatorParams.batchSize());
        System.out.println("cache-size = " + estimatorParams.mainCacheSize());
        System.out.println("warmup = " + estimatorParams.warmup());
        System.out.println("steps = " + estimatorParams.coldChainSteps());
        System.out.println("steps-power-base = " + estimatorParams.powerBase());
        System.out.println("temperature-delta = " + estimatorParams.deltaT());
        System.out.println("swap-period = " + estimatorParams.swapPeriod());
    }

    private void run(Parameters params, EstimatorParams estimatorParams) throws IOException {
        printParameters(params, estimatorParams);
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

        bn = new BayesianNetwork(genes, params.mainSF());

        bn.clearEdges();

        estimator = new NetworkEstimator(estimatorParams, new SplittableRandom(params.seed()));
        long cur_time = System.currentTimeMillis();
        estimator.run(bn);
        long elapsed_time = System.currentTimeMillis() - cur_time;

        System.out.printf("%02d:%02d:%02d\n",
                TimeUnit.MILLISECONDS.toHours(elapsed_time),
                TimeUnit.MILLISECONDS.toMinutes(elapsed_time) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsed_time)),
                TimeUnit.MILLISECONDS.toSeconds(elapsed_time) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed_time)));

        writeResults(new OutputStreamWriter(new FileOutputStream(params.output())));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                writeResults(new OutputStreamWriter(new FileOutputStream(params.output())));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }));

        analyzeGS();

        if (params.print() != 0) {
            System.out.println("Edges:");
            writeResults(new OutputStreamWriter(System.out));
        }
    }

    private void analyzeGS() {
        if (params.gold() != null) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("python3", "analyze.py", params.output().getAbsolutePath(), params.gold().getAbsolutePath());
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

                while (true) {
                    String str = br.readLine();
                    if (str == null) {
                        break;
                    }
                    System.out.println(str);
                }

                int exitCode = process.waitFor();
                System.out.println("Python finished with: " + exitCode);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            Set<GeneEdge> edgesGS = new HashSet<>();
            Set<GeneEdge> edgesMy = new HashSet<>();
            List<GeneEdge> edgesMyList = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(params.gold()))) {
                String str;
                String[] arr;

                while ((str = br.readLine()) != null) {
                    arr = str.split("\t");
                    if (arr.length != 3) {
                        throw new IllegalStateException();
                    }
                    if (Integer.parseInt(arr[2]) == 1) {
                        edgesGS.add(new GeneEdge(arr[0], arr[1], 0));
                    }
                }
                EdgeList results = estimator.resultsFromCompletedTasks();
                for (EdgeList.Edge edge : results.edges()) {
                    GeneEdge mEdge = new GeneEdge(bn.var(edge.v()).getName(), bn.var(edge.u()).getName(), edge.p(results.get_number_merged()));
                    edgesMy.add(mEdge);
                    edgesMyList.add(mEdge);
                }
            } catch (IOException e) {
                System.out.println("GS file read fail");
                return;
            } catch (NumberFormatException e) {
                System.out.println("Wrong GS format");
                return;
            }

            edgesMyList.sort(Comparator.comparing(GeneEdge::getProbability));

            int tp = 0;
            int fp = 0;
            int tn = 0;
            int fn = 0;

            // n rows * n columns - n number of elements, such as a[i][i]
            int totalEdges = bn.size() * bn.size() - bn.size();

            for (GeneEdge edge : edgesMy) {
                if (edgesGS.contains(edge)) {
                    tp++;
                } else {
                    fp++;
                }
            }

            for (GeneEdge edge : edgesGS) {
                if (!edgesMy.contains(edge)) {
                    fn++;
                }
            }
            tn = totalEdges - tp - fp - fn;
            System.out.println("True  positive: " + tp);
            System.out.println("False positive: " + fp);
            System.out.println("True  negative: " + tn);
            System.out.println("False negative: " + fn);
        }

    }
}

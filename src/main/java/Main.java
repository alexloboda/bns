import ctlab.bn.BayesianNetwork;
import ctlab.bn.Variable;
import ctlab.mcmc.Model;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class Main {
    private static File ge_file;
    private static File output;
    private static File log;

    private static int n_steps;
    private static int warmup_steps;
    private static int executors;
    private static int disc_limit;
    private static int n_cores;
    private static int default_cls;

    private static boolean parse_args(String[] args) {
        OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();
        optionParser.acceptsAll(asList("h", "help"), "Print a short help message");
        OptionSet optionSet = optionParser.parse(args);

        OptionSpec<String> ge = optionParser.acceptsAll(asList("g", "ge"),
                "gene expression data file").withRequiredArg().ofType(String.class).required();
        OptionSpec<Integer> steps = optionParser.acceptsAll(asList("s", "steps"),
                "Number of steps for each run").withRequiredArg().ofType(Integer.class).required();
        OptionSpec<Integer> warmup = optionParser.acceptsAll(asList("w", "warmup"),
                "Number of steps before main loop").withRequiredArg().ofType(Integer.class).defaultsTo(0);
        OptionSpec<Integer> exec = optionParser.acceptsAll(asList("r", "runs"),
                "Number of independent runs").withRequiredArg().ofType(Integer.class).defaultsTo(1);
        OptionSpec<Integer> default_classes = optionParser.acceptsAll(asList("classes"),
                "Default number of classes").withRequiredArg().ofType(Integer.class).defaultsTo(2);
        OptionSpec<Integer> bound = optionParser.acceptsAll(asList("b", "disc_limit"),
                "Limit on number of discretization algorithm steps").withRequiredArg().ofType(Integer.class)
                .defaultsTo(5);
        OptionSpec<String> outfile = optionParser.acceptsAll(asList("o", "out"),
                "output file").withRequiredArg().ofType(String.class).required();
        OptionSpec<String> log_file = optionParser.acceptsAll(asList("l", "log"),
                "log directory").withRequiredArg().ofType(String.class);
        OptionSpec<Integer> cores = optionParser.acceptsAll(asList("c", "cores"),
                "number of cores").withRequiredArg().ofType(Integer.class).defaultsTo(1);

        if (optionSet.has("h")) {
            try {
                optionParser.printHelpOn(System.out);
            } catch (IOException ignored) { }
            return false;
        }

        optionSet = optionParser.parse(args);

        n_steps = optionSet.valueOf(steps);
        warmup_steps = optionSet.valueOf(warmup);
        executors = optionSet.valueOf(exec);
        disc_limit = optionSet.valueOf(bound);
        n_cores = optionSet.valueOf(cores);
        default_cls = optionSet.valueOf(default_classes);

        ge_file = new File(optionSet.valueOf(ge));
        output = new File(optionSet.valueOf(outfile));
        if (optionSet.has(log_file)) {
            log = new File(optionSet.valueOf(log_file));
        }

        return true;
    }

    private static List<Variable> parse_ge_table(File file) throws FileNotFoundException {
        List<Variable> res = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<List<Double>> data = new ArrayList<>();
        try (Scanner sc = new Scanner(file)) {
            String firstLine = sc.nextLine();
            Scanner line_sc = new Scanner(firstLine);
            while(line_sc.hasNext()) {
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
                res.add(new Variable(names.get(i), data.get(i), default_cls));
            }
        }
        return res;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (!parse_args(args)) {
            System.exit(0);
        }

        List<Variable> genes = parse_ge_table(ge_file);
        int n = genes.size();
        BayesianNetwork bn = new BayesianNetwork(genes, false, true);

        List<Model> models = new ArrayList<>();

        for (int i = 0; i < executors; i++) {
            models.add(new Model(new BayesianNetwork(bn), disc_limit));
        }

        ExecutorService es = Executors.newFixedThreadPool(n_cores);
        models.forEach(x -> es.submit(new Task(x, n_steps, warmup_steps)));
        es.shutdown();
        es.awaitTermination(1_000_000, TimeUnit.HOURS);

        List<Edge> edges = count_hits(n, models);

        edges.forEach(x -> x.scale((n_steps * executors)));
        try(PrintWriter pw = new PrintWriter(output)) {
            for (Edge e: edges) {
                pw.println(genes.get(e.v).getName() + "\t" + genes.get(e.u).getName() + "\t" + e.weight);
            }
        }
    }

    private static class Task implements Runnable {
        private int steps;
        private Model m;
        private int warmup;

        Task(Model m, int steps, int warmup) {
            this.m = m;
            this.steps = steps;
            this.warmup = warmup;
        }

        @Override
        public void run() {
            for (int i = 0; i < warmup; i++) {
                m.step(true);
            }
            for (int i = 0; i < steps; i++){
                m.step(false);
            }
            m.finish();
        }
    }

    private static List<Edge> count_hits(int n, List<Model> models) {
        int[][] hits = new int[n][n];
        for (Model m : models) {
            int[][] hs = m.hits();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    hits[i][j] += hs[i][j];
                }
            }
        }

        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                edges.add(new Edge(i, j, hits[i][j]));
            }
        }

        edges.sort(Comparator.comparingDouble(o -> -o.weight));
        return edges;
    }

    public static class Edge {
         int v;
         int u;
         double weight;

         Edge(int v, int u, double weight) {
            this.v = v;
            this.u = u;
            this.weight = weight;
        }

        void scale(double f) {
            weight /= f;
        }
    }
}

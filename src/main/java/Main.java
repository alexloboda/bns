import ctlab.bn.*;
import ctlab.bn.sf.BDE;
import ctlab.mcmc.Logger;
import ctlab.mcmc.Model;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class Main {
    private static File ge_file;
    private static File output;
    private static File log;
    private static File bound_graph;

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
        OptionSpec<Integer> default_classes = optionParser.acceptsAll(asList("c", "classes"),
                "Default number of classes").withRequiredArg().ofType(Integer.class).defaultsTo(3);
        OptionSpec<Integer> bound = optionParser.acceptsAll(asList("b", "disc_limit"),
                "Limit on number of discretization algorithm steps").withRequiredArg().ofType(Integer.class)
                .defaultsTo(5);
        OptionSpec<String> outfile = optionParser.acceptsAll(asList("o", "out"),
                "output file").withRequiredArg().ofType(String.class).required();
        OptionSpec<String> log_file = optionParser.acceptsAll(asList("l", "log"),
                "log directory").withRequiredArg().ofType(String.class);
        OptionSpec<Integer> cores = optionParser.acceptsAll(asList("m", "cores"),
                "number of cores").withRequiredArg().ofType(Integer.class).defaultsTo(1);
        OptionSpec<String> bound_file = optionParser.accepts("bound",
                "bound graph for score").withRequiredArg().ofType(String.class);

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
        bound_graph = new File(optionSet.valueOf(bound_file));
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

    static Graph parseGraph(int n) throws FileNotFoundException {
        Graph g = new Graph(n);
        try(Scanner sc = new Scanner(bound_graph)) {
            while(sc.hasNext()) {
                String from = sc.next().substring(1);
                String to = sc.next().substring(1);
                int v = Integer.parseInt(from) - 1;
                int u = Integer.parseInt(to) - 1;
                g.add_edge(v, u);
            }
        }
        return g;
    }

    public static void main(String[] args) throws IOException {
        if (!parse_args(args)) {
            System.exit(0);
        }

        List<Variable> genes = parse_ge_table(ge_file);
        int n = genes.size();
        BayesianNetwork bn = new BayesianNetwork(genes, false, true);

        Solver solver = new Solver(new BDE(1));
        solver.solve(bn, parseGraph(bn.size()), 10);
        //try (Scanner scanner = new Scanner(new File("gs_w_cycles"))) {
        //    while(scanner.hasNext()){
        //        int v = scanner.nextInt() - 1;
        //        int u = scanner.nextInt() - 1;
        //        bn.add_edge(v, u);
        //    }
        //}

        //try (Scanner scanner = new Scanner(new File("rgbm1.tsv"))) {
        //    while(scanner.hasNext()) {
        //        int v = Integer.parseInt(scanner.next().substring(1)) - 1;
        //        int u = Integer.parseInt(scanner.next().substring(1)) - 1;
        //        scanner.nextDouble();
        //        if (!bn.path_exists(u, v) && bn.ingoing_edges(u).size() < 5) {
        //            bn.add_edge(v, u);
        //        }
        //    }
        //}

        //bn.discretize(100);
        for (int j = 0; j < bn.size(); j++) {
            System.err.print(bn.var(j).getName() + ": ");
            System.err.println(String.join(" ", bn.var(j).cardinalities().stream()
                    .map(x -> Integer.toString(x))
                    .collect(Collectors.toList())));
        }

        bn.clear_edges();

        List<Model> models = new ArrayList<>();

        long timeBeforeExecution = System.currentTimeMillis();
        try {
            for (int i = 0; i < executors; i++) {
                Model model = new Model(bn, new BDE(1), disc_limit, false);
                if (log != null) {
                    model.setLogger(new Logger(new File(log, Integer.toString(i + 1)), 4));
                } else {
                    model.setLogger(new Logger(null, 4));
                }
                models.add(model);
            }

            AtomicInteger counter = new AtomicInteger();
            ExecutorService es = Executors.newFixedThreadPool(n_cores);
            List<Future<?>> fs = models.stream()
                    .map(x -> es.submit(new Task(x, n_steps, warmup_steps, counter)))
                    .collect(Collectors.toList());
            es.shutdown();
            es.awaitTermination(1_000_000, TimeUnit.HOURS);
            try {
                for (Future f: fs) {
                    f.get();
                }
            } catch (InterruptedException|ExecutionException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            models.forEach(Model::closeLogger);
        }

        System.err.println();
        System.err.println("Computing took " + ((System.currentTimeMillis() - timeBeforeExecution) / 1000.0) + " seconds");

        List<Edge> edges = count_hits(n, models);

        edges.forEach(x -> x.scale((n_steps * executors)));
        try (PrintWriter pw = new PrintWriter(output)) {
            for (Edge e : edges) {
                if (e.v != e.u) {
                    pw.println(genes.get(e.v).getName() + "\t" + genes.get(e.u).getName() + "\t" + e.weight);
                }
            }
        }
    }

    private static class Task implements Runnable {
        private int steps;
        private Model m;
        private int warmup;
        private AtomicInteger counter;

        Task(Model m, int steps, int warmup, AtomicInteger counter) {
            this.m = m;
            this.steps = steps;
            this.warmup = warmup;
            this.counter = counter;
        }

        @Override
        public void run() {
            m.run();
            for (int i = 0; i < warmup; i++) {
                m.step(true);
            }
            for (int i = 0; i < steps; i++){
                m.step(false);
            }
            m.finish();
            synchronized (System.err) {
                int c = counter.incrementAndGet();
                System.err.print("\r" + c);
                System.err.flush();
            }
        }
    }

    private static List<Edge> count_hits(int n, List<Model> models) {
        long[][] hits = new long[n][n];
        for (Model m : models) {
            long[][] hs = m.hits();
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

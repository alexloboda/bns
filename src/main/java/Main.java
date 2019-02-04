import ctlab.bn.*;
import ctlab.bn.sf.ScoringFunction;
import ctlab.graph.Graph;
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
    private static File geneExpressionFile;
    private static File output;
    private static File log;
    private static File preranking;
    private static File priorsFile;

    private static int nSteps;
    private static int warmupSteps;
    private static int executors;
    private static int nThreads;
    private static int defaultCls;
    private static int nOptimizer;
    private static int prerankingLimit;
    private static Integer discLB;
    private static Integer discUB;
    private static boolean randomPolicy;
    private static boolean randomDAG;

    private static ScoringFunction mainSF;
    private static ScoringFunction discSF;
    private static Variable.DiscretizationPrior disc_prior;

    private static boolean parse_args(String[] args) throws FileNotFoundException {
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
        OptionSpec<String> outfile = optionParser.acceptsAll(asList("o", "out"),
                "output file").withRequiredArg().ofType(String.class).required();
        OptionSpec<String> log_file = optionParser.acceptsAll(asList("l", "log"),
                "log directory").withRequiredArg().ofType(String.class);
        OptionSpec<Integer> cores = optionParser.acceptsAll(asList("m", "threads"),
                "number of cores").withRequiredArg().ofType(Integer.class).defaultsTo(1);
        OptionSpec<String> main_sf_opt = optionParser.accepts("main-sf",
                "scoring function used in main algorithm").withRequiredArg().ofType(String.class).defaultsTo("BDE 1");
        OptionSpec<String> disc_sf_opt = optionParser.accepts("disc-sf",
                "SF used in discretization").withRequiredArg().ofType(String.class).defaultsTo("BDE 1");
        OptionSpec<Integer> n_optimizer_opt = optionParser.accepts("n-optimizer",
                "Number of optimizer steps").withRequiredArg().ofType(Integer.class).defaultsTo(0);
        OptionSpec<String> preranking_opt = optionParser.accepts("preranking",
                "Preranking for preprocessing").withRequiredArg().ofType(String.class);
        OptionSpec<Integer> disc_lb_opt = optionParser.accepts("disc-lb",
                "discretization minimum class size").withRequiredArg().ofType(Integer.class);
        OptionSpec<Integer> disc_ub_opt = optionParser.accepts("disc-ub",
                "discretization maximum class size").withRequiredArg().ofType(Integer.class);
        OptionSpec<Integer> prerank_limit_opt = optionParser.accepts("preranking-limit",
                "preranking limit on preprocessing").withRequiredArg().ofType(Integer.class).defaultsTo(7);
        OptionSpec<String> disc_prior_opt = optionParser.accepts("disc-prior",
                "discretization priors(UNIFORM, EXP, MULTINOMIAL)").withRequiredArg().ofType(String.class)
                .defaultsTo("EXP");
        OptionSpec<String> priors_opt = optionParser.accepts("prior",
                "prior distribution over edges").withRequiredArg().ofType(String.class);
        optionParser.accepts("random-policy", "random  discretization each step");
        optionParser.accepts("random-dag", "DAG");


        if (optionSet.has("h")) {
            try {
                optionParser.printHelpOn(System.out);
            } catch (IOException ignored) { }
            return false;
        }

        optionSet = optionParser.parse(args);

        nSteps = optionSet.valueOf(steps);
        warmupSteps = optionSet.valueOf(warmup);
        executors = optionSet.valueOf(exec);
        nThreads = optionSet.valueOf(cores);
        defaultCls = optionSet.valueOf(default_classes);
        randomPolicy = optionSet.has("random-policy");
        randomDAG = optionSet.has("random-dag");

        geneExpressionFile = new File(optionSet.valueOf(ge));
        output = new File(optionSet.valueOf(outfile));
        if (optionSet.has(log_file)) {
            log = new File(optionSet.valueOf(log_file));
        }
        if (optionSet.has("priors")) {
            priorsFile = new File(optionSet.valueOf(priors_opt));
        }
        if (optionSet.has(disc_lb_opt)) {
            discLB = optionSet.valueOf(disc_lb_opt);
        }
        if (optionSet.has(disc_ub_opt)) {
            discUB = optionSet.valueOf(disc_ub_opt);
        }
        mainSF = ScoringFunction.parse(optionSet.valueOf(main_sf_opt));
        discSF = ScoringFunction.parse(optionSet.valueOf(disc_sf_opt));
        disc_prior = Variable.DiscretizationPrior.valueOf(optionSet.valueOf(disc_prior_opt).toUpperCase());
        nOptimizer = optionSet.valueOf(n_optimizer_opt);
        prerankingLimit = optionSet.valueOf(prerank_limit_opt);
        if (optionSet.has(preranking_opt)) {
            preranking = new File(optionSet.valueOf(preranking_opt));
            if (!preranking.exists()) {
                throw new FileNotFoundException("Preranking does not exist");
            }
        }

        return true;
    }

    private static void parsePriors(BayesianNetwork bn) throws FileNotFoundException {
        int n = bn.size();
        double[][] priors = new double[n][n];
        try(Scanner scanner = new Scanner(priorsFile)) {
            int v = bn.getID(scanner.next());
            int u = bn.getID(scanner.next());
            double logprior = scanner.nextDouble();
            priors[v][u] = logprior;
        }
        //bn.set_prior_distribution(new ExplicitPrior(priors));
    }

    private static List<Variable> parseGETable(File file) throws FileNotFoundException {
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
                res.add(new Variable(names.get(i), data.get(i), defaultCls, disc_prior));
            }
        }

        return res;
    }

    private static Graph parse_bound(BayesianNetwork bn) throws FileNotFoundException {
        Graph g = new Graph(bn.size());
        try (Scanner scanner = new Scanner(preranking)) {
            while(scanner.hasNext()) {
                int v = bn.getID(scanner.next());
                int u = bn.getID(scanner.next());
                scanner.next();
                if (g.inDegree(u) < prerankingLimit) {
                    g.addEdge(v, u);
                }
            }
        }
        return g;
    }

    public static void main(String[] args) throws IOException {
        if (!parse_args(args)) {
            System.exit(0);
        }

        List<Variable> genes = parseGETable(geneExpressionFile);

        for (Variable v : genes) {
            int lb = 1;
            int ub = v.obsNum();
            if (discLB != null) {
                lb = discLB;
            }
            if (discUB != null) {
                ub = discUB;
            }
            v.setDiscLimits(lb, ub);
        }

        int n = genes.size();
        BayesianNetwork bn = new BayesianNetwork(genes);

        if (priorsFile != null) {
            parsePriors(bn);
        }

        if (nOptimizer > 0) {
            Solver solver = new Solver(discSF);
            solver.solve(bn, parse_bound(bn), nOptimizer);
        }

        bn.clearEdges();

        List<Model> models = new ArrayList<>();
        SplittableRandom re = new SplittableRandom();

        try {
            for (int i = 0; i < executors; i++) {
                //Model model = new Model(bn, mainSF, re.split(), randomPolicy, randomDAG, 100);
                Model model = null;
                models.add(model);
            }

            AtomicInteger counter = new AtomicInteger();
            ExecutorService es = Executors.newFixedThreadPool(nThreads);
            List<Future<?>> fs = models.stream()
                    .map(x -> es.submit(new Task(x, nSteps, warmupSteps, counter)))
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
        }

        List<Edge> edges = countHits(n, models);

        edges.forEach(x -> x.scale((nSteps * executors)));
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
                m.step(1);
            }
            for (int i = 0; i < steps; i++){
                m.step(1);
            }
            m.finish();
            synchronized (System.err) {
                int c = counter.incrementAndGet();
                System.err.print("\r" + c);
                System.err.flush();
            }
        }
    }

    private static List<Edge> countHits(int n, List<Model> models) {
        double[][] fs = new double[n][n];
        for (Model m : models) {
            double[][] hs = m.frequencies();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    fs[i][j] += hs[i][j] / models.size();
                }
            }
        }

        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                edges.add(new Edge(i, j, fs[i][j]));
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

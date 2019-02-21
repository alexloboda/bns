package ctlab.mc5;

import ctlab.mc5.bn.BayesianNetwork;
import ctlab.mc5.bn.Solver;
import ctlab.mc5.bn.Variable;
import ctlab.mc5.bn.sf.ScoringFunction;
import ctlab.mc5.graph.Graph;
import ctlab.mc5.mcmc.EstimatorParams;
import ctlab.mc5.mcmc.NetworkEstimator;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@Command(mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
        resourceBundle = "ctlab.mc5.Parameters")
public class Main {
    private static final String ESTIMATOR_PARAMS = "EstimatorParams";
    private static final String MAIN_PARAMS = "MainParams";

    private Parameters params;
    private EstimatorParams estimatorParams;

    private List<Variable> parseGETable(File file) throws FileNotFoundException {
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
                res.add(new Variable(names.get(i), data.get(i), params.defaultCls(),
                        params.discPrior()));
            }
        }

        return res;
    }

    private Graph parseBound(BayesianNetwork bn) throws FileNotFoundException {
        Graph g = new Graph(bn.size());
        try (Scanner scanner = new Scanner(params.preranking())) {
            while(scanner.hasNext()) {
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
        Main app = new Main();
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
            app.run((Parameters)mixins.get(MAIN_PARAMS), (EstimatorParams)mixins.get(ESTIMATOR_PARAMS));
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

    private void run(Parameters params, EstimatorParams estimatorParams) throws IOException {
        this.params = params;
        this.estimatorParams = estimatorParams;
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

        BayesianNetwork bn = new BayesianNetwork(genes);

        if (params.nOptimizer() > 0) {
            Solver solver = new Solver(params.discSF());
            solver.solve(bn, parseBound(bn), params.nOptimizer());
        }

        bn.clearEdges();

        NetworkEstimator estimator = new NetworkEstimator(estimatorParams, new SplittableRandom(params.seed()));
    }
}

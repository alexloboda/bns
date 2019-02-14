package ctlab.mc5;

import ctlab.mc5.bn.*;
import ctlab.mc5.graph.Graph;
import org.apache.commons.io.IOUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Main {
    private Parameters params;

    public Main(String[] args) throws IOException {
        CommandLine cli = new CommandLine(new Parameters());
        cli.parse(args);
        if (cli.isUsageHelpRequested()) {
            cli.usage(System.out);
            return;
        } else if (cli.isVersionHelpRequested()) {
            cli.printVersionHelp(System.out);
            return;
        }
    }

    //private List<Variable> parseGETable(File file) throws FileNotFoundException {
    //    List<Variable> res = new ArrayList<>();
    //    List<String> names = new ArrayList<>();
    //    List<List<Double>> data = new ArrayList<>();
    //    try (Scanner sc = new Scanner(file)) {
    //        String firstLine = sc.nextLine();
    //        Scanner line_sc = new Scanner(firstLine);
    //        while(line_sc.hasNext()) {
    //            names.add(line_sc.next());
    //        }

    //        int n = names.size();
    //        for (int i = 0; i < n; i++) {
    //            data.add(new ArrayList<>());
    //        }

    //        while (sc.hasNext()) {
    //            for (int i = 0; i < n; i++) {
    //                data.get(i).add(sc.nextDouble());
    //            }
    //        }

    //        for (int i = 0; i < n; i++) {
    //            res.add(new Variable(names.get(i), data.get(i), params.defaultNumberOfClasses(),
    //                    params.discretizationPrior()));
    //        }
    //    }

    //    return res;
    //}

    //private Graph parseBound(BayesianNetwork bn) throws FileNotFoundException {
    //    Graph g = new Graph(bn.size());
    //    try (Scanner scanner = new Scanner(params.prerankingFile())) {
    //        while(scanner.hasNext()) {
    //            int v = bn.getID(scanner.next());
    //            int u = bn.getID(scanner.next());
    //            scanner.next();
    //            if (g.inDegree(u) < params.prerankingLimit()) {
    //                g.addEdge(v, u);
    //            }
    //        }
    //    }
    //    return g;
    //}

    public static void main(String[] args) {
        try {
            Main app = new Main(args);
            app.run();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private void run() throws IOException {
        //List<Variable> genes = parseGETable(params.geneExpressionDataFile());

        //for (Variable v : genes) {
        //    int lb = 1;
        //    int ub = v.obsNum();
        //    if (params.discretizationLowerBound() != null) {
        //        lb = params.discretizationLowerBound();
        //    }
        //    if (params.discretizationUpperBound() != null) {
        //        ub = params.discretizationUpperBound();
        //    }
        //    v.setDiscLimits(lb, ub);
        //}

        //int n = genes.size();
        //BayesianNetwork bn = new BayesianNetwork(genes);

        //if (params.optimizerSteps() > 0) {
        //    Solver solver = new Solver(params.discSF());
        //    solver.solve(bn, parseBound(bn), params.optimizerSteps());
        //}

        //bn.clearEdges();
    }
}

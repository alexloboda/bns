import ctlab.bn.BayesianNetwork;
import ctlab.bn.Variable;
import ctlab.mcmc.Model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Main {
    private static List<Variable> parse_ge_table(String filename) throws FileNotFoundException {
    }

    public static void main(String[] args) throws IOException {
        String gene_file = args[0];

        int steps = Integer.parseInt(args[1]);
        int warmup = Integer.parseInt(args[2]);
        int models_num = Integer.parseInt(args[3]);
        int disc_bound = Integer.parseInt(args[4]);

        String out_file = args[5];
        String log_dir = args[6];

        List<Variable> genes = parse_ge_table(gene_file);
        int n = genes.size();
        BayesianNetwork bn = new BayesianNetwork(genes, false, true);

        List<Model> models = new ArrayList<>();

        List<Edge> edges = count_hits(n, models);

        for (int i = 0; i < models_num; i++) {

        }

        edges.forEach(x -> x.scale((steps * models_num)));
        try(PrintWriter pw = new PrintWriter(out_file)) {
            for (Edge e: edges) {
                pw.println(genes.get(e.v).getName() + "\t" + genes.get(e.u).getName() + "\t" + e.weight);
            }
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

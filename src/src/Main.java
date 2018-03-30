import ctlab.mcmc.Model;
import ctlab.mcmc.Worker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static List<String> gene_names(String filename) throws FileNotFoundException {
        List<String> res;
        try (Scanner s = new Scanner(new File(filename))) {
            String l = s.nextLine();
            res = Arrays.asList(l.split("\\s+"));
        }
        return res;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int n = Integer.parseInt(args[0]);
        String gene_file = args[1];
        int steps = Integer.parseInt(args[2]);
        int warmup = Integer.parseInt(args[3]);
        String out_file = args[4];
        List<String> gene_names = gene_names(gene_file);

        List<Model> models = new ArrayList<>();
        int size = args.length - 5;
        for (int i = 0; i < size; i++) {
            ScoringFunction sf = new ScoringFunction(gene_names, Integer.parseInt(args[i + 5]));
            models.add(new Model(n, 10, sf));
        }
        List<Worker> workers = models.stream().map(x -> new Worker(x, steps, warmup)).collect(Collectors.toList());
        for (Worker w : workers) {
            w.start();
        }
        for (Worker w : workers) {
            w.join();
        }

        for (Model m : models) {
            System.err.println(m.accepts());
        }

        List<Edge> edges = count_hits(n, models);
        edges.forEach(x -> x.scale(steps * size));
        try(PrintWriter pw = new PrintWriter(out_file)) {
            for (Edge e: edges) {
                pw.println(gene_names.get(e.v) + "\t" + gene_names.get(e.u) + "\t" + e.weight);
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

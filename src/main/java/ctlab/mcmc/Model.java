package ctlab.mcmc;

import ctlab.graph.Graph;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static java.lang.Math.log;

public class Model extends Thread {
    private static final double LADD = log(0.5);
    private static final double LOTH = log(2.0);

    private int n;
    private int bound;
    private int[][] hits;
    private int[][] time;
    private double[] loglik;
    private Graph graph;
    private ScoringFunction sf;
    private int steps;
    private Random random;
    private int accepts;

    public Model(int n, int bound, ScoringFunction sf) {
        super();
        this.n = n;
        this.bound = bound;
        hits = new int[n][n];
        loglik = new double[n];
        graph = new Graph(n);
        this.sf = sf;
        random = new Random();
        time = new int[n][n];
        for (int i = 0; i < n; i++) {
            loglik[i] = sf.score(i, Collections.emptyList());
        }
    }

    public void step(boolean warmingup) {
        if (!warmingup) {
            steps++;
        }
        int v = 0;
        int u = 0;
        while (v == u) {
            v = random.nextInt(n);
            u = random.nextInt(n);
        }
        if (graph.edge_exists(v, u)) {
            if (random.nextBoolean()) {
                try_remove(v, u);
            } else {
                try_inverse(v, u);
            }
        } else {
            try_add(v, u);
        }
    }

    public int[][] hits() {
        return hits;
    }

    public int accepts() {
        return accepts;
    }

    private double score_after_deletion(int v, int u) {
        List<Integer> ingoing = graph.ingoing_edges(u);
        ingoing.remove((Integer)v);
        return sf.score(u, ingoing);
    }

    private double score_after_insertion(int v, int u) {
        List<Integer> ingoing = graph.ingoing_edges(u);
        ingoing.add(v);
        return sf.score(u, ingoing);
    }

    private void try_inverse(int v, int u) {
        graph.remove_edge(v, u);
        if (graph.path_exists(v, u)) {
            graph.add_edge(v, u);
            return;
        }
        graph.add_edge(v, u);
        double sd = score_after_deletion(v, u);
        double sa = score_after_insertion(u, v);
        double log_accept = sd + sa - loglik[v] - loglik[u];
        if (log(random.nextDouble()) < log_accept) {
            remove_edge(v, u, sd);
            add_edge(u, v, sa);
        }
    }

    private void remove_edge(int v, int u, double score) {
        graph.remove_edge(v, u);
        hits[v][u] += steps - time[v][u];
        loglik[u] = score;
    }

    private void add_edge(int v, int u, double score) {
        graph.add_edge(v, u);
        time[v][u] = steps;
        loglik[u] = score;
    }

    private void try_remove(int v, int u) {
        double score = score_after_deletion(v, u);
        double log_accept = score - loglik[u] + LOTH;
        if (log(random.nextDouble()) < log_accept) {
            accepts++;
            remove_edge(v, u, score);
        }
    }

    private void try_add(int v, int u){
       if (graph.in_degree(u) > bound) {
           return;
       }
       if (graph.path_exists(u, v)) {
           return;
       }
       double score = score_after_insertion(v, u);
       double log_accept = score - loglik[u] + LADD;
       if (log(random.nextDouble()) < log_accept) {
           accepts++;
           add_edge(v, u, score);
       }
   }

    public void finish() {
        for (int u = 0; u < n; u++) {
            for (int v : graph.ingoing_edges(u)) {
                remove_edge(v, u, sf.score(u, Collections.emptyList()));
            }
        }
    }
}
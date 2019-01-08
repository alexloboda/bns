package ctlab.graph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Graph {
    private List<List<Edge>> adj;
    private List<List<Edge>> radj;
    private Edge[][] edges;

    public Graph(int n) {
        adj = new ArrayList<>();
        radj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
            radj.add(new ArrayList<>());
        }
        edges = new Edge[n][n];
    }

    public Graph(Graph g) {
        this(g.size());
        for (int v = 0; v < g.size(); v++) {
            for (int u : g.outgoing_edges(v)) {
                add_edge(v, u);
            }
        }
    }

    private boolean step(Queue<Integer> q, int[] vis, boolean[] player, int i) {
        while (!q.isEmpty()) {
            int v = q.peek();
            if (vis[v] != i) {
                return false;
            }
            q.poll();
            List<Edge> nei = player[v] ? adj.get(v) : radj.get(v);
            for (Edge e : nei) {
                int u = player[v] ? e.u : e.v;
                if (vis[u] == 0) {
                    vis[u] = vis[v] + 1;
                    player[u] = player[v];
                    q.add(u);
                } else {
                    if (player[v] != player[u]) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<Integer> top_sort() {
        List<Boolean> vis = new ArrayList<>(Collections.nCopies(size(), false));
        List<Integer> out = new ArrayList<>(Collections.nCopies(size(), 0));
        int time = 0;
        for (int i = 0; i < size(); i++) {
            if (radj.get(i).isEmpty()) {
                time = dfs(i, vis, out, time);
            }
        }

        List<Integer> result = IntStream.range(0, size()).boxed().collect(Collectors.toList());

        Comparator<Integer> cmp = Comparator.comparingInt(out::get);

        result.sort(cmp);

        return result;
    }

    public int size() {
        return adj.size();
    }

    private int dfs(int v, List<Boolean> vis, List<Integer> out, int time) {
        vis.set(v, true);
        for (int u : outgoing_edges(v)) {
            if (!vis.get(u)) {
                time = dfs(u, vis, out, time + 1);
            }
        }
        out.set(v, ++time);
        return time;
    }

    public List<Integer> ingoing_edges(int v) {
        List<Integer> ingoing = new ArrayList<>(radj.get(v).size());
        for (Edge e: radj.get(v)) {
            ingoing.add(e.v);
        }
       return ingoing;
    }

    public List<Integer> outgoing_edges(int v) {
        return adj.get(v).stream().map(e -> e.u).collect(Collectors.toList());
    }

    public int out_degree(int v) {
        return adj.get(v).size();
    }

    public boolean path_exists(int v, int u) {
        // men meet in the middle
        int[] vis = new int[adj.size()];
        boolean[] player = new boolean[adj.size()];
        Queue<Integer> direct = new ArrayDeque<>();
        Queue<Integer> back = new ArrayDeque<>();
        direct.add(v);
        back.add(u);
        vis[v] = 1;
        vis[u] = 1;
        player[v] = true;
        for (int i = 0; i < adj.size() / 2; i++) {
            if (step(direct, vis, player, i + 1)) {
                return true;
            }
            if (direct.isEmpty()) {
                return false;
            }
            if (step(back, vis, player, i + 1)) {
                return true;
            }
            if (back.isEmpty()) {
                return false;
            }
        }
        return false;
    }

    public boolean edge_exists(int v, int u) {
        return edges[v][u] != null;
    }

    public void add_edge(int v, int u) {
        edges[v][u] = new Edge(v, u, adj.get(v).size(), radj.get(u).size());
        adj.get(v).add(edges[v][u]);
        radj.get(u).add(edges[v][u]);
    }

    public void remove_edge(int v, int u) {
        Edge e = edges[v][u];
        List<Edge> nei = adj.get(v);
        List<Edge> rnei = radj.get(u);
        Collections.swap(nei, e.pos, nei.size() - 1);
        Collections.swap(rnei, e.rpos, rnei.size() - 1);
        nei.get(e.pos).pos = e.pos;
        rnei.get(e.rpos).rpos = e.rpos;
        nei.remove(nei.size() - 1);
        rnei.remove(rnei.size() - 1);
        edges[v][u] = null;
    }

    public int in_degree(int v) {
        return radj.get(v).size();
    }

    public class Edge {
        private int v;
        private int u;
        private int pos;
        private int rpos;

        Edge(Edge e) {
            v = e.v;
            u = e.u;
            pos = e.pos;
            rpos = e.rpos;
        }

        Edge(int v, int u, int pos, int rpos) {
            this.v = v;
            this.u = u;
            this.pos = pos;
            this.rpos = rpos;
        }

        int v() {
            return v;
        }

        int u() {
            return u;
        }
    }
}
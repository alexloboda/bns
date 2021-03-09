package ctlab.mc5.graph;

import org.apache.commons.math3.util.Pair;


import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Graph {
    private List<List<Edge>> adj;
    private List<List<Edge>> radj;
    private Edge[][] edges;
    private int[][] subscriptions;

    private List<Map<Integer, DynamicGraph.EdgeToken>> tokens;
    private DynamicGraph dgraph;
    private BiConsumer<Integer, Integer> noPathSupportCallback;

    public Graph(int n) {
        adj = new ArrayList<>();
        radj = new ArrayList<>();
        dgraph = new DynamicGraph(n);
        tokens = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
            radj.add(new ArrayList<>());
            tokens.add(new HashMap<>());
        }
        edges = new Edge[n][n];
        subscriptions = new int[n][n];
    }

    public void setCallback(BiConsumer<Integer, Integer> callback) {
        this.noPathSupportCallback = callback;
    }

    public Graph(Graph g) {
        this(g.size());
        for (int v = 0; v < g.size(); v++) {
            for (int u : g.outgoingEdges(v)) {
                addEdge(v, u);
            }
        }
    }

    private Pair<Integer, Integer> step(Queue<Integer> q, int[] vis, boolean[] player, int[] parent) {
        int v = q.peek();
        q.poll();
        List<Edge> nei = player[v] ? adj.get(v) : radj.get(v);
        for (Edge e : nei) {
            int u = player[v] ? e.u : e.v;
            if (vis[u] == 0) {
                vis[u] = vis[v] + 1;
                player[u] = player[v];
                parent[u] = v;
                q.add(u);
            } else {
                if (player[v] != player[u]) {
                    return player[v] ? new Pair<>(v, u) : new Pair<>(u, v);
                }
            }
        }
        return null;
    }

    public List<Integer> topSort() {
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
        for (int u : outgoingEdges(v)) {
            if (!vis.get(u)) {
                time = dfs(u, vis, out, time + 1);
            }
        }
        out.set(v, ++time);
        return time;
    }

    public List<Integer> ingoingEdges(int v) {
        List<Integer> ingoing = new ArrayList<>(radj.get(v).size());
        for (Edge e : radj.get(v)) {
            ingoing.add(e.v);
        }
        return ingoing;
    }

    public List<Integer> outgoingEdges(int v) {
        return adj.get(v).stream().map(e -> e.u).collect(Collectors.toList());
    }

    public int outDegree(int v) {
        return adj.get(v).size();
    }

    private void processPath(Pair<Integer, Integer> meet, int first, int last, int[] parent) {
        Subscription subscription = new Subscription(first, last);
        ++subscriptions[first][last];
        int v = meet.getFirst();
        int u = meet.getSecond();
        while (u != first) {
            subscription.addBackReference(edges[v][u].subscribe(subscription));
            u = v;
            v = parent[v];
        }

        // back part
        v = meet.getSecond();
        u = parent[v];
        while (v != last) {
            subscription.addBackReference(edges[v][u].subscribe(subscription));
            v = u;
            u = parent[u];
        }
    }

    public boolean pathExists(int v, int u) {
        if (!dgraph.isConnected(v, u)) {
            return false;
        }
        if (subscriptions[v][u] > 0) {
            return true;
        }
        // men meet in the middle
        int[] vis = new int[adj.size()];
        int[] parent = new int[adj.size()];
        boolean[] player = new boolean[adj.size()];
        Queue<Integer> direct = new ArrayDeque<>();
        Queue<Integer> back = new ArrayDeque<>();
        direct.add(v);
        back.add(u);
        vis[v] = 1;
        vis[u] = 1;
        player[v] = true;
        for (int i = 0; i < adj.size() / 2; i++) {
            Pair<Integer, Integer> directResult = step(direct, vis, player, parent);
            if (directResult != null) {
                processPath(directResult, v, u, parent);
                return true;
            }
            if (direct.isEmpty()) {
                return false;
            }
            Pair<Integer, Integer> backResult = step(back, vis, player, parent);
            if (backResult != null) {
                processPath(backResult, v, u, parent);
                return true;
            }
            if (back.isEmpty()) {
                return false;
            }
        }
        return false;
    }

    public boolean edgeExists(int v, int u) {
        return edges[v][u] != null;
    }

    public void addEdge(int v, int u) {
        edges[v][u] = new Edge(v, u, adj.get(v).size(), radj.get(u).size());
        adj.get(v).add(edges[v][u]);
        radj.get(u).add(edges[v][u]);
        DynamicGraph.EdgeToken token = dgraph.add(v, u);
        assert token != null;

        if (u < v) {
            int t = u;
            u = v;
            v = t;
        }
        assert v < u;
        tokens.get(v).put(u, token);
    }

    public void removeEdge(int v, int u) {
        Edge e = edges[v][u];
        e.unsubscribe();
        List<Edge> nei = adj.get(v);
        List<Edge> rnei = radj.get(u);
        Collections.swap(nei, e.pos, nei.size() - 1);
        Collections.swap(rnei, e.rpos, rnei.size() - 1);
        nei.get(e.pos).pos = e.pos;
        rnei.get(e.rpos).rpos = e.rpos;
        nei.remove(nei.size() - 1);
        rnei.remove(rnei.size() - 1);
        edges[v][u] = null;

        if (u < v) {
            int t = u;
            u = v;
            v = t;
        }
        DynamicGraph.EdgeToken token = tokens.get(v).remove(u);
        assert token != null;
        dgraph.remove(token);
    }

    public int inDegree(int v) {
        return radj.get(v).size();
    }

    private class Subscription {
        private int v;
        private int u;
        private List<LinkedList.Entry> backRefs;

        public Subscription(int v, int u) {
            this.v = v;
            this.u = u;
            backRefs = new ArrayList<>();
        }

        public void addBackReference(LinkedList.Entry backRef) {
            backRefs.add(backRef);
        }

        public void processDeletion() {
            --subscriptions[v][u];
            if (noPathSupportCallback != null) {
                noPathSupportCallback.accept(v, u);
            }
            for (LinkedList.Entry ref : backRefs) {
                ref.remove();
            }
        }
    }

    private static class LinkedList {
        private Entry head;

        public Entry add(Subscription subscription) {
            if (head == null) {
                head = new Entry(subscription);
            } else {
                head = new Entry(head.left, head, subscription);
                head.left.right = head;
                head.right.left = head;
            }
            return head;
        }

        public void removeAll() {
            List<Subscription> subscriptions = new ArrayList<>();
            if (head == null) {
                return;
            }
            Entry curr = head;
            while (true) {
                subscriptions.add(curr.subscription());
                curr = curr.right;
                if (curr == head) {
                    break;
                }
            }
            for (Subscription s : subscriptions) {
                s.processDeletion();
            }
        }

        private class Entry {
            private Entry left;
            private Entry right;
            private Subscription subscription;

            Entry(Subscription subscription) {
                this.left = this;
                this.right = this;
                this.subscription = subscription;
            }

            Entry(Entry left, Entry right, Subscription subscription) {
                this.left = left;
                this.right = right;
                this.subscription = subscription;
            }

            public void remove() {
                left.right = right;
                right.left = left;
                if (head == this) {
                    head = left != this ? left : null;
                }
            }

            public Subscription subscription() {
                return subscription;
            }
        }
    }

    public class Edge {
        private int v;
        private int u;
        private int pos;
        private int rpos;
        private LinkedList subscriptions;

        Edge(int v, int u, int pos, int rpos) {
            this.v = v;
            this.u = u;
            this.pos = pos;
            this.rpos = rpos;
            subscriptions = new LinkedList();
        }

        LinkedList.Entry subscribe(Subscription subscription) {
            return subscriptions.add(subscription);
        }

        void unsubscribe() {
            subscriptions.removeAll();
        }
    }
}
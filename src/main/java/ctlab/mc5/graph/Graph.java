package ctlab.mc5.graph;

import ctlab.mc5.bn.Variable;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Graph {
    private List<List<Edge>> adj;
    private List<List<Edge>> radj;
    private List<Edge> edgelist;
    private Edge[][] edges;
    private int[][] subscriptions;
    private int edgeCount;

    private List<Map<Integer, DynamicGraph.EdgeToken>> tokens;
    private DynamicGraph dgraph;
    private BiConsumer<Integer, Integer> noPathSupportCallback;

    public Graph(int n) {
        adj = new ArrayList<>();
        radj = new ArrayList<>();
        edgelist = new ArrayList<>();
        dgraph = new DynamicGraph(n);
        tokens = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
            radj.add(new ArrayList<>());
            tokens.add(new HashMap<>());
        }
        edges = new Edge[n][n];
        subscriptions = new int[n][n];
        edgeCount = 0;
    }

    public void setCallback(BiConsumer<Integer, Integer> callback) {
        this.noPathSupportCallback = callback;
    }

    public Graph(Graph g) {
        this(g.size());
        for (int from = 0; from < g.size(); from++) {
            for (int to : g.outgoingEdges(from)) {
                addEdge(from, to);
            }
        }
        edgeCount = g.edgeCount;
    }

    private Pair<Integer, Integer> step(Queue<Integer> q, int[] vis, boolean[] player, int[] parent) {
        int v = q.peek();
        q.poll();
        List<Edge> nei = player[v] ? adj.get(v) : radj.get(v);
        for (Edge e : nei) {
            int u = player[v] ? e.to : e.from;
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

    public Pair<Integer, Integer> randomEdge(SplittableRandom rd) {
        Edge e = edgelist.get(rd.nextInt(edgelist.size()));
        return new Pair<>(e.from, e.to);
    }

    public List<Integer> ingoingEdges(int to) {
        List<Integer> ingoing = new ArrayList<>(radj.get(to).size());
        for (Edge e: radj.get(to)) {
            ingoing.add(e.from);
        }
       return ingoing;
    }

    public List<Integer> outgoingEdges(int from) {
        return adj.get(from).stream().map(e -> e.to).collect(Collectors.toList());
    }

    public int outDegree(int from) {
        return adj.get(from).size();
    }

    public boolean isSubscribed(int first, int last) {
        return subscriptions[first][last] != 0;
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

    public boolean meetAtTheMiddle(int from, int to) {
        // men meet in the middle
        int[] vis = new int[adj.size()];
        int[] parent = new int[adj.size()];
        boolean[] player = new boolean[adj.size()];
        Queue<Integer> direct = new ArrayDeque<>();
        Queue<Integer> back = new ArrayDeque<>();
        direct.add(from);
        back.add(to);
        vis[from] = 1;
        vis[to] = 1;
        player[from] = true;
        for (int i = 0; i < adj.size() / 2; i++) {
            Pair<Integer, Integer> directResult = step(direct, vis, player, parent);
            if (directResult != null) {
                processPath(directResult, from, to, parent);
                return true;
            }
            if (direct.isEmpty()) {
                return false;
            }
            Pair<Integer, Integer> backResult = step(back, vis, player, parent);
            if (backResult != null) {
                processPath(backResult, from, to, parent);
                return true;
            }
            if (back.isEmpty()) {
                return false;
            }
        }
        return false;
    }

    public boolean pathExists(int from, int to) {
        if (!dgraph.isConnected(from, to)) {
            return false;
        }
        if (subscriptions[from][to] > 0) {
            return true;
        }
        return meetAtTheMiddle(from, to);
    }

    public boolean edgeExists(int from, int to) {
        return edges[from][to] != null;
    }

    public void addEdge(int from, int to) {
        assert edges[from][to] == null;
        edges[from][to] = new Edge(from, to, adj.get(from).size(), radj.get(to).size(), edgelist.size());
        adj.get(from).add(edges[from][to]);
        radj.get(to).add(edges[from][to]);
        edgelist.add(edges[from][to]);
        DynamicGraph.EdgeToken token = dgraph.add(from, to);
        assert token != null;

        if (to < from) {
            int t = to;
            to = from;
            from = t;
        }
        assert from < to;
        tokens.get(from).put(to, token);
        edgeCount++;
    }

    public void removeEdge(int from, int to) {
        Edge e = edges[from][to];
        e.unsubscribe();
        List<Edge> nei = adj.get(from);
        List<Edge> rnei = radj.get(to);

        Collections.swap(nei, e.pos, nei.size() - 1);
        Collections.swap(rnei, e.rpos, rnei.size() - 1);
        Collections.swap(edgelist, e.listpos, edgelist.size() - 1);


        nei.get(e.pos).pos = e.pos;
        rnei.get(e.rpos).rpos = e.rpos;
        edgelist.get(e.listpos).listpos = e.listpos;

        nei.remove(nei.size() - 1);
        rnei.remove(rnei.size() - 1);
        edgelist.remove(edgelist.size() - 1);
        edges[from][to] = null;

        if (to < from) {
            int t = to;
            to = from;
            from = t;
        }
        DynamicGraph.EdgeToken token = tokens.get(from).remove(to);
        assert token != null;
        assert edgeCount != 0;
        dgraph.remove(token);
        edgeCount--;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public int inDegree(int to) {
        return radj.get(to).size();
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
            for (LinkedList.Entry ref: backRefs) {
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
            do {
                subscriptions.add(curr.subscription());
                curr = curr.right;
            } while (curr != head);
            for (Subscription s: subscriptions) {
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
        private int from;
        private int to;
        private int pos;
        private int rpos;
        private int listpos;
        private LinkedList subscriptions;

        Edge(int from, int to, int pos, int rpos, int listpos) {
            this.from = from;
            this.to = to;
            this.pos = pos;
            this.rpos = rpos;
            this.listpos = listpos;
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
package ctlab.mc5.mcmc;

import java.io.Serializable;
import java.util.*;

public class EdgeList implements Serializable {
    private List<Edge> edgeList;
    private Map<Integer, Map<Integer, Edge>> edgeMap;
    private int number_merged = 1;

    public EdgeList() {
        edgeList = new ArrayList<>();
        edgeMap = new HashMap<>();
        number_merged = 0;
    }

    public Edge getEdge(int from, int to) {
        if (!edgeMap.containsKey(from)) {
            return null;
        }
        return edgeMap.get(from).get(to);
    }

    public void addEdge(Edge edge) {
        if (getEdge(edge.v(), edge.u()) != null) {
            throw new IllegalArgumentException();
        }
        Edge mEdge = new Edge(edge);
        mEdge.num += number_merged;
        edgeList.add(mEdge);
        if (!edgeMap.containsKey(edge.v())) {
            edgeMap.put(edge.v(), new HashMap<>());
        }
        edgeMap.get(edge.v()).put(edge.u(), edge);
    }

    public List<Edge> edges() {
        List<Edge> ret = new ArrayList<>(edgeList);
        Collections.sort(ret);
        return ret;
    }

    public void merge(EdgeList other) {
        for (Edge e : other.edgeList) {
            Edge local = getEdge(e.v(), e.u());
            if (local == null) {
                addEdge(e);
            } else {
                local.merge(e);
            }
        }
        number_merged++;
    }

    public EdgeList mergeWithRet(EdgeList other) {
        for (Edge e : other.edgeList) {
            Edge local = getEdge(e.v(), e.u());
            if (local == null) {
                addEdge(e);
            } else {
                local.merge(e);
            }
        }
        number_merged++;
        return this;
    }

    public int size() {
        return edgeList.size();
    }

    public static class Edge implements Comparable<Edge>, Serializable {
        private int v;
        private int u;
        private int num;
        private double k;

        public Edge(int v, int u, int num, double k) {
            this.v = v;
            this.u = u;
            this.num = num;
            this.k = k;
        }

        public Edge(Edge other) {
            this.v = other.v;
            this.u = other.u;
            this.num = other.num;
            this.k = other.k;
        }

        public void merge(Edge other) {
            if (!equals(other)) {
                throw new IllegalArgumentException();
            }
            k += other.k;
            num += other.num;
        }

        public int v() {
            return v;
        }

        public int u() {
            return u;
        }

        public double p() {
            return k / num;
        }

        public double k() {
            return k;
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() != Edge.class) {
                return false;
            }
            Edge e = (Edge) o;
            return this.v == e.v && this.u == e.u;
        }

        @Override
        public int compareTo(Edge o) {
            return Double.compare(k / num, o.k / o.num);
        }
    }
}

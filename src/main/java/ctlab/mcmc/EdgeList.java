package ctlab.mcmc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EdgeList {
    private List<Edge> edgeList;
    private Map<Integer, Map<Integer, Edge>> edgeMap;

    public EdgeList() {
        edgeList = new ArrayList<>();
        edgeMap = new HashMap<>();
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
        edgeList.add(edge);
        if (!edgeMap.containsKey(edge.v())) {
            edgeMap.put(edge.v(), new HashMap<>());
        }
        edgeMap.get(edge.v()).put(edge.u(), edge);
    }

    public void merge(EdgeList other) {
        for (Edge e: other.edgeList) {
            Edge local = getEdge(e.v(), e.u());
            if (local == null) {
                addEdge(e);
            } else {
                local.merge(e);
            }
        }
    }

    public int size() {
        return edgeList.size();
    }

    public static class Edge {
        private int v;
        private int u;
        private double p;
        private double k;

        public Edge(int v, int u, double p, double k) {
            this.v = v;
            this.u = u;
            this.p = p;
            this.k = k;
        }

        public void merge(Edge other) {
            if (!equals(other)) {
                throw new IllegalArgumentException();
            }
            double sumK = k + other.k;
            p = (k / sumK) * p + (other.k / sumK) * other.p;
            k = sumK;
        }

        public int v() {
            return v;
        }

        public int u() {
            return u;
        }

        public double p() {
            return p;
        }

        public double k() {
            return k;
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() != Edge.class) {
                return false;
            }
            Edge e = (Edge)o;
            return this.v == e.v && this.u == e.u;
        }
    }
}

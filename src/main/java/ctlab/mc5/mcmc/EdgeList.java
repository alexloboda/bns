package ctlab.mc5.mcmc;

import java.util.*;

public class EdgeList {
    private List<Edge> edgeList;
    private Map<Integer, Map<Integer, Edge>> edgeMap;
    private int number_merged = 1;

    public EdgeList(int count) {
        edgeList = new ArrayList<>();
        edgeMap = new HashMap<>();
        number_merged = count;
    }

    public int get_number_merged() {
        return number_merged;
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
        edgeList.add(mEdge);
        if (!edgeMap.containsKey(edge.v())) {
            edgeMap.put(edge.v(), new HashMap<>());
        }
        edgeMap.get(edge.v()).put(edge.u(), mEdge);
    }

    public List<Edge> edges() {
        List<Edge> ret = new ArrayList<>(edgeList);
        Collections.sort(ret, Collections.reverseOrder());
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
        number_merged+=other.number_merged;
    }

    public int size() {
        return edgeList.size();
    }

    public static class Edge implements Comparable<Edge> {
        private int v;
        private int u;
        private int count;

        public Edge(int v, int u, int count) {
            this.v = v;
            this.u = u;
            this.count = count;
        }

        public Edge(Edge other) {
            this.v = other.v;
            this.u = other.u;
            this.count = other.count;
        }

        public void merge(Edge other) {
            if (!equals(other)) {
                throw new IllegalArgumentException();
            }
            if (other == this) {
                count++;
            } else {
                count += other.count;
            }
        }

        public int v() {
            return v;
        }

        public int u() {
            return u;
        }

        public double p(int number_merged) {
            return count * 1.0 / number_merged;
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
            return Double.compare(count, o.count);
        }
    }
}

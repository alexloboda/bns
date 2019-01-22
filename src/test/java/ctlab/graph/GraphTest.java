package ctlab.graph;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

public class GraphTest {
    private static final int TEST_SIZE = 16;
    private static final int TEST_ACTIONS = 1000;
    private static final int TEST_CASES = 10;

    private class ReferenceGraph {
        private boolean[][] adj;

        ReferenceGraph(int n) {
            adj = new boolean[n][n];
        }

        void addEdge(int v, int u) {
            adj[v][u] = true;
        }

        void removeEdge(int v, int u) {
            adj[v][u] = false;
        }

        boolean edgeExists(int v, int u) {
            return adj[v][u];
        }

        boolean pathExists(int v, int u) {
            int n = adj.length;

            boolean[][] closure = Arrays.stream(adj).map(boolean[]::clone).toArray(boolean[][]::new);

            // Floyd-Warshall
            for (int k = 0; k < n; k++) {
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        closure[i][j] = closure[i][j] || (closure[i][k] && closure[k][j]);
                    }
                }
            }

            return closure[v][u];
        }
    }

    @Test
    public void test() {
        Random random = new Random(0xC0FFEE);
        int n = TEST_SIZE;

        for (int t = 0; t < TEST_CASES; t++) {
            Graph g = new Graph(TEST_SIZE);
            ReferenceGraph rg = new ReferenceGraph(TEST_SIZE);
            int edges = 0;
            for (int i = 0; i < TEST_ACTIONS; i++) {
                boolean remove = false;
                if (edges != 0) {
                    if (edges > n + 5) {
                        remove = random.nextDouble() < 0.75;
                    } else {
                        remove = random.nextDouble() < 0.25;
                    }
                }
                if (edges == n * (n - 1) / 2) {
                    remove = true;
                }
                while (true) {
                    int v = random.nextInt(n);
                    int u = random.nextInt(n);
                    Assert.assertEquals(rg.edgeExists(v, u), g.edgeExists(v, u));
                    if (v == u || rg.edgeExists(v, u) != remove || rg.pathExists(u, v)) {
                        continue;
                    }
                    if (remove) {
                        rg.removeEdge(v, u);
                        g.removeEdge(v, u);
                        --edges;
                    } else {
                        rg.addEdge(v, u);
                        g.addEdge(v, u);
                        ++edges;
                    }
                    for (int j = 0; j < n; j++) {
                        for (int k = 0; k < n; k++) {
                            if (j != k) {
                                Assert.assertEquals(rg.pathExists(j, k), g.pathExists(j, k));
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
}

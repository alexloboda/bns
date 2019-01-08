package ctlab;

import ctlab.graph.Graph;
import ctlab.graph.DynamicGraph;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class DynamicGraphTest {
    public static final int MAX_SIZE = 33;
    public static final int TESTS_PER_SIZE = 1;
    public static final Function<Integer, Integer> actions = x -> 2 * x * x;

    @Test
    public void testDynamicGraphs() {
        Random re = new Random(42);
        for (int i = 2; i < MAX_SIZE; i += 3) {
            for (int j = 0; j < TESTS_PER_SIZE; j++) {
                Graph g = new Graph(i);
                DynamicGraph dg = new DynamicGraph(i);
                List<List<DynamicGraph.EdgeToken>> tokens = new ArrayList<>();
                for (int k = 0; k < i; k++) {
                    tokens.add(new ArrayList<>(Collections.nCopies(i, null)));
                }
                for (int k = 0; k < actions.apply(i); k++) {
                    int v = re.nextInt(i);
                    int u = re.nextInt(i);
                    if (v == u) {
                        continue;
                    }
                    if (g.edge_exists(v, u)) {
                        g.remove_edge(v, u);
                        g.remove_edge(u, v);
                        dg.remove(tokens.get(v).get(u));
                        tokens.get(v).set(u, null);
                        tokens.get(u).set(v, null);
                    } else {
                        g.add_edge(v, u);
                        g.add_edge(u, v);
                        DynamicGraph.EdgeToken token = dg.add(v, u);
                        tokens.get(v).set(u, token);
                        tokens.get(u).set(v, token);
                    }
                    for (int w = 0; w < i; w++) {
                        for (int z = 0; z < i; z++) {
                            if (w != z) {
                                String errMsg = "TEST SIZE: " + i + ", TEST #" + j + ", ACTION #" + k + "\n" + w + "\t" + z;
                                Assert.assertEquals(errMsg, g.path_exists(w, z), dg.isConnected(w, z));
                            }
                        }
                    }
                }
            }
        }
    }
}

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BayesianNetwork {
    private List<Variable> variables;
    private Graph g;

    BayesianNetwork(List<Variable> variables) {
        g = new Graph(variables.size());
        this.variables = variables;
        LogFactorial lf = new LogFactorial();
        variables.forEach(v -> v.setLF(lf));
    }

    void add_edge(int v, int u) {
        g.add_edge(v, u);
    }

    void discretization_step() {
        List<Integer> order = g.top_sort();
        for (int v : order) {
            Variable var = variables.get(v);
            List<Variable> ps = g.ingoing_edges(v).stream()
                    .map(x -> variables.get(x))
                    .collect(Collectors.toList());
            List<Variable> cs = g.outgoing_edges(v).stream()
                    .map(x -> variables.get(x))
                    .collect(Collectors.toList());
            List<List<Variable>> ss = g.outgoing_edges(v).stream()
                    .map(x -> g.ingoing_edges(x))
                    .map(x -> x.stream()
                            .map(y -> variables.get(y))
                            .filter(y -> !y.equals(var))
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
            var.discretize(ps, cs, ss);
        }
    }

    void print_descrtization() {
        for (Variable v : variables) {
            for (double edge : v.discretization_edges()) {
                System.out.print(edge + "\t");
            }
            System.out.println();
        }
    }

    public void print_graph(File gfile) throws FileNotFoundException {
        try(PrintWriter pw = new PrintWriter(gfile)) {
            List<Integer> top_sort = g.top_sort();
            Collections.reverse(top_sort);
            for (int v : top_sort) {
                for (int u : g.ingoing_edges(v)) {
                    pw.print((u + 1) + "\t");
                }
                pw.println(v + 1);
            }
        }
    }
}

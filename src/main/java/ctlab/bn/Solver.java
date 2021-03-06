package ctlab.bn;

import ctlab.bn.sf.ScoringFunction;
import joptsimple.internal.Strings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Solver {
    private ScoringFunction sf;
    private BayesianNetwork bn;
    private Graph bound;

    public Solver(ScoringFunction sf) {
        this.sf = sf;
    }

    public void solve(BayesianNetwork bayesianNetwork, Graph bound, int steps) throws IOException {
        this.bound = bound;
        this.bn = bayesianNetwork;
        for (int i = 0; i < steps; i++) {
            System.err.println("step");
            solver_step();
            bayesianNetwork.discretize(100);

        }
    }

    private void solver_step() throws IOException {
        File scoresFile = File.createTempFile("baynet", ".scores");
        scoresFile.deleteOnExit();

        try (PrintWriter pw = new PrintWriter(scoresFile)) {
            pw.println(bn.size());
            for (int i = 0; i < bn.size(); i++) {
                List<Record> records = new ArrayList<>();
                List<Integer> vars = new ArrayList<>();
                double ub = sf.score(bn.var(i), new ArrayList<>());
                records.add(new Record(ub));
                rec(i, 0, vars, records);

                pw.println(i + " " + records.size());
                for (Record r : records) {
                    pw.print(r.loglik + " " + r.ps.size() + " ");
                    pw.println(Strings.join(r.ps.stream()
                            .map(Object::toString)
                            .collect(Collectors.toList()), " "));
                }
            }
        }

        runGobnilp(scoresFile);
    }

    public void runGobnilp(File scoresFile) throws IOException {
        bn.clear_edges();
        String[] args = {"gobnilp", "-v=0", "-g=settings", scoresFile.getAbsolutePath()};
        Process p = Runtime.getRuntime().exec(args);
        InputStream is = p.getInputStream();
        Scanner sc = new Scanner(is);
        int m = 0;
        while (sc.hasNext()) {
            String line = sc.next();
            String log = sc.nextLine();
            if (line.startsWith("BN")) {
                System.err.println(line + " " + log);
                continue;
            }
            String[] lr = line.split("<-");
            int u = Integer.parseInt(lr[0]);
            if (lr.length > 1) {
                for (String s : lr[1].split(",")) {
                    int v = Integer.parseInt(s);
                    bn.add_edge(v, u);
                    m++;
                }
            }
        }
        System.err.println("Solver: " + m);
    }

    private void rec(int i, int j, List<Integer> vars, List<Record> rs) {
        if (j == i) {
            rec(i, j + 1, vars, rs);
            return;
        }
        if (j == bn.size()) {
            return;
        }
        if (bound.edge_exists(j, i)) {
            vars.add(j);
            double score = sf.score(bn.var(i), vars.stream().map(x -> bn.var(x)).collect(Collectors.toList()));
            Record r = new Record(score);
            vars.forEach(r::addParent);
            rs.add(r);
            rec(i, j + 1, vars, rs);
            vars.remove(vars.size() - 1);
        }
        rec(i, j + 1, vars, rs);
    }

    static class Record {
        double loglik;
        List<Integer> ps;

        Record(double ll) {
            loglik = ll;
            ps = new ArrayList<>();
        }

        void addParent(int v) {
            ps.add(v);
        }
    }
}

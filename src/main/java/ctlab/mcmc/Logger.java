package ctlab.mcmc;

import joptsimple.internal.Strings;

import javax.xml.ws.WebServiceException;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Logger implements Closeable  {
    private PrintWriter pw;
    private Map<String, String> vs;
    private List<String> keys;
    private int limit;

    public Logger(File logFile, int card_limit) throws IOException {
        if (logFile != null) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            pw = new PrintWriter(writer);
        }
        vs = new HashMap<>();
        keys = new ArrayList<>();
        clear_all();
        if (pw != null) {
            pw.println(String.join("\t", keys));
        }
        limit = card_limit;
    }

    Logger() {}

    void edge(int v, int u, double ll) {
        if (pw != null) {
            set_key("v", v);
            set_key("u", u);
            set_key("loglik", ll);
        }
    }

    void disc_steps(int n) {
        if (pw != null) {
            set_key("disc", n);
        }
    }

    void card(int[] card) {
        if (pw != null) {
            vs.put("card", Strings.join(Arrays.stream(card)
                    .mapToObj(Integer::toString)
                    .skip(1)
                    .limit(limit)
                    .collect(Collectors.toList()), ","));
        }
    }

    void action(Action a) {
        if (pw != null) {
            vs.put("action", a.toString());
        }
    }

    void prior(double v) {
        if (pw != null) {
            set_key("prior", v);
        }
    }

    void status(Status s) {
        if (pw != null) {
            vs.put("status", s.toString());
        }
    }

    private void set_key(String key, Number n) {
        vs.put(key, n.toString());
    }

    private void clear_all() {
        if (pw != null) {
            List<String> keys_na = Arrays.asList("loglik", "v", "u", "action", "status", "disc", "card", "prior");
            vs.put("ll_after", "-inf");
            vs.put("p_accept", "0");

            if (keys.isEmpty()) {
                keys.addAll(keys_na);
                keys.addAll(Arrays.asList("ll_after", "p_accept"));
            }
            keys_na.forEach(x -> vs.put(x, "n/a"));
        }
    }

    void submit() {
        if (pw != null) {
            String str = String.join("\t", keys.stream().map(x -> vs.get(x)).collect(Collectors.toList()));
            pw.println(str);
            clear_all();
        }
    }

    @Override
    public void close() throws WebServiceException {
        if (pw != null) {
            pw.close();
        }
    }

    void log_accept(double log_accept) {
        if (pw != null) {
            set_key("p_accept", Math.exp(log_accept));
        }
    }

    void score(double score) {
        set_key("ll_after", score);
    }

    static enum Status {
        CYCLE, REJECTED, ACCEPTED
    }

    static enum  Action {
        INSERT, REMOVE, REVERSE
    }
}

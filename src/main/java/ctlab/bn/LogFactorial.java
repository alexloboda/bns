package ctlab.bn;

import java.util.ArrayList;
import java.util.List;

public class LogFactorial {
    private List<Double> values;

    LogFactorial() {
        values = new ArrayList<>();
        values.add(0.0);
    }

    double value(int n) {
        if (n > 10000) {
            System.err.println(n);
            Thread.dumpStack();
        }
        if (n >= values.size()) {
            for (int i = values.size(); i <= n; i++) {
                values.add(values.get(i - 1) + Math.log(i));
            }
        }
        return values.get(n);
    }
}

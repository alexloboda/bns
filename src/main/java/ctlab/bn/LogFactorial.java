package ctlab.bn;

import java.util.ArrayList;
import java.util.List;

public class LogFactorial {
    private final List<Double> values;

    public LogFactorial() {
        values = new ArrayList<>();
        values.add(0.0);
    }

    public double value(int n) {
        if (n >= values.size()) {
            synchronized (values) {
                for (int i = values.size(); i <= n; i++) {
                    values.add(values.get(i - 1) + Math.log(i));
                }
            }
        }
        return values.get(n);
    }
}

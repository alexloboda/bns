package ctlab.bn.action;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ActionList implements Iterable<Action> {
    public static double EPS = 1e-12;
    public static double logEPS = Math.log(EPS);

    private List<Action> actions;
    private int maxSize;

    public ActionList(int maxSize) {
        actions = new ArrayList<>();
        this.maxSize = maxSize;
    }

    public Action cutoff() {
        Collections.sort(actions);
        Collections.reverse(actions);
        int u = actions.get(0).u();
        List<Action> remain = actions.subList(maxSize - 1, actions.size());
        Action result = null;
        Action toAdd = null;

        double[] lls = remain.stream().mapToDouble(Action::loglik).toArray();

        if (lls.length != 0) {
            double maxLL = Arrays.stream(lls).max().getAsDouble();
            double threshold = logEPS - Math.log(lls.length);
            lls = Arrays.stream(lls)
                    .map(x -> x - maxLL)
                    .filter(x -> x > threshold)
                    .map(Math::exp)
                    .toArray();
            double sum = Arrays.stream(lls).sum();
            double geneLL = maxLL - Math.log(sum);
            toAdd = new Action(ActionType.GENE, u, u, geneLL);
            double[] ps = Arrays.stream(lls).map(x -> x / sum).toArray();
            int i = 0;
            double rvalue = ThreadLocalRandom.current().nextDouble();
            double prefix = 0.0;
            for (double p: ps) {
                prefix += p;
                if (rvalue < prefix + EPS) {
                    result = remain.get(i);
                    break;
                }
                ++i;
            }
        }

        remain.clear();
        if (toAdd != null) {
            actions.add(toAdd);
        }

        return result;
    }

    public int size() {
        return actions.size();
    }

    public void add(Action action) {
        actions.add(action);
    }

    @Override
    public Iterator<Action> iterator() {
        return actions.iterator();
    }
}

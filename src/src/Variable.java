import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Variable {
    private String name;

    private List<Integer> data;
    private List<Integer> disc_edges;

    public Variable(String name, List<Double> data, int disc_points) {
        this.name = name;
        this.data = IntStream.range(0, data.size()).boxed().collect(Collectors.toList());

        Comparator<Integer> cmp = new Comparator<Integer>() {
            @Override
            public int compare(Integer i, Integer j) {
                return Double.compare(data.get(i), data.get(j));
            }
        };

        this.data.sort(cmp);

        for (int i = 0; i < disc_points; i++) {
            disc_edges.add((int) Math.round(data.size() * ((i + 1) / (double)disc_points)));
        }
    }

    public int experiment(int n) {
        int index = data.get(n);
        int discrete = Collections.binarySearch(data, index);
        if (discrete < 0) {
            discrete = -discrete - 1;
        }
        return discrete;
    }
}

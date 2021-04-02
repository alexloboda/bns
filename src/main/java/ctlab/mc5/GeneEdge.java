package ctlab.mc5;

import java.util.Objects;

public class GeneEdge implements Comparable<GeneEdge> {
    public final String first;
    public final String second;
    public double probability;

    GeneEdge(String first, String second, double p) {
        this.first = first;
        this.second = second;
        probability = p;
    }

    double getProbability() {
        return probability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneEdge geneEdge = (GeneEdge) o;
        return first.equals(geneEdge.first) &&
                second.equals(geneEdge.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public int compareTo(GeneEdge geneEdge) {
        int first1 = first.compareTo(geneEdge.first);
        if (first1 == 0) {
            return second.compareTo(geneEdge.second);
        }
        return first1;
    }
}

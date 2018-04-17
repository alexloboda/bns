import ctlab.bn.PriorDistribution;

public class Test {
    public static void main(String[] args) {
        PriorDistribution pd = new PriorDistribution(10, 2);
        System.err.println(pd.value());
        pd.add(0);
        System.err.println(pd.value());
        pd.remove(1);
        System.err.println(pd.value());
    }
}

import ctlab.bn.prior.ScaleFreePrior;

public class Test {
    public static void main(String[] args) {
        ScaleFreePrior pd = new ScaleFreePrior(10, 2);
        System.err.println(pd.value());
        pd.add(0);
        System.err.println(pd.value());
        pd.remove(1);
        System.err.println(pd.value());
    }
}

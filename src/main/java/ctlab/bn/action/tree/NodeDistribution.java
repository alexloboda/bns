package ctlab.bn.action.tree;

public class NodeDistribution {
    private double left;
    private double right;

    public NodeDistribution(double left, double right) {
        this.left = left;
        this.right = right;
    }

    public double left() {
        return left;
    }

    public double right() {
        return right;
    }
}

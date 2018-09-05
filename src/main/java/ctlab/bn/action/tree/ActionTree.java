package ctlab.bn.action.tree;

import ctlab.bn.action.Action;

public class ActionTree {
    public static double EPS = 1e-12;
    private SplayEntry root;

    public ActionTree() {
        root = null;
    }

    public void addAction(Action action) {
        if (root == null) {
            root = new SplayEntry(action);
        } else {
            root = SplayEntry.merge(root, new SplayEntry(action));
        }
    }

}

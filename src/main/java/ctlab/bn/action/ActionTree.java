package ctlab.bn.action;

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
            root = root.add(new SplayEntry(action));
        }
    }

    public Action randomAction() {

    }

    public class SplayEntry {
        private SplayEntry parent;
        private SplayEntry left;
        private SplayEntry right;
        private Action action;
        private double sum;
        private double llf;

        public SplayEntry(Action action) {
            this(action, null, null);
        }

        public SplayEntry(Action action, SplayEntry left, SplayEntry right) {
            this.left = left;
            this.right = right;
            this.action = action;
            this.sum = 1;
            this.llf = action.loglik();
        }

        private void recalc() {

        }

        public SplayEntry splay() {
            while (parent != null) {
                SplayEntry grandpa = parent.parent;
                boolean isLeft = parent.left == this;
                if (grandpa != null) {
                    boolean pIsLeft = grandpa.left == parent;
                    if (isLeft == pIsLeft) {
                        grandpa.rotate(pIsLeft);
                        parent.rotate(isLeft);
                    } else {
                        parent.rotate(isLeft);
                        grandpa.rotate(pIsLeft);
                    }
                }
            }
        }

        private void rotate(boolean leftRotate) {
            SplayEntry child = null;
            if (leftRotate) {
                child = left;
                left = child.right;
                if (left != null) {
                    left.parent = this;
                }
                child.right = this;
            } else {
                child = right;
                right = child.left;
                if (right != null) {
                    right.parent = this;
                }
                child.left = this;
            }
            if (parent != null) {
                if (parent.left == this) {
                    parent.left = child;
                } else {
                    parent.right = child;
                }
            }
            child.parent = parent;
            parent = child;
            recalc();
            child.recalc();
            if (parent != null) {
                parent.recalc();
            }
        }

        private SplayEntry findRoot() {
            SplayEntry e = this;
            while (e.parent != null) {
                e = e.parent;
            }
            return e;
        }

        private SplayEntry rightmost() {
            SplayEntry e = this;
            while (e.right != null) {
                e = e.right;
            }
            return e;
        }

        private SplayEntry merge(SplayEntry l, SplayEntry r) {
            if (l == null) {
                return r;
            }
            if (r == null) {
                return l;
            }
            r = r.findRoot();
            l = l.findRoot().rightmost();

            l.splay();
            l.right = r;
            r.parent = l;
            l.recalc();
            return l;
        }

        public void remove() {
            splay();
            if (left != null) {
                left.parent = null;
            }
            if (right != null) {
                right.parent = null;
            }
            if (left == null || right == null) {
                return;
            }
            merge(left, right);
        }
    }
}

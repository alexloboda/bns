package ctlab.bn.action.tree;

import ctlab.bn.action.Action;

class SplayEntry {
    private SplayEntry parent;
    private SplayEntry left;
    private SplayEntry right;
    private Action action;
    private double ll;
    private NodeDistribution p;

    public SplayEntry(Action action) {
        this.action = action;
    }

    private void recalc() {
        double LL = action.loglik();
        double maxLL = LL;
        double leftLL = Double.NEGATIVE_INFINITY;
        double rightLL = Double.NEGATIVE_INFINITY;
        if (left != null) {
            leftLL = left.ll;
        }
        if (right != null) {
            rightLL = right.ll;
        }
        maxLL = Math.max(leftLL, maxLL);
        maxLL = Math.max(rightLL, maxLL);
        LL = Math.exp(LL - maxLL);
        leftLL = Math.exp(leftLL - maxLL);
        rightLL = Math.exp(rightLL - maxLL);
        double sum = LL + leftLL + rightLL;
        p = new NodeDistribution(leftLL / sum, (leftLL + LL) / sum);
        ll = maxLL + Math.log(sum);
    }

     static SplayEntry merge(SplayEntry l, SplayEntry r) {
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

    private void splay() {
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
        SplayEntry child;
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

    public SplayEntry remove() {
        splay();
        if (left != null) {
            left.parent = null;
        }
        if (right != null) {
            right.parent = null;
        }
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return merge(left, right);
    }
}

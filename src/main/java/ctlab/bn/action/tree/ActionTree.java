package ctlab.bn.action.tree;

import ctlab.bn.action.Action;

import java.util.*;

public class ActionTree {
    private SplayEntry root;
    private List<SplayEntry> map;
    private Queue<Integer> freeIDs;

    public ActionTree() {
        root = null;
        map = new ArrayList<>();
        freeIDs = new ArrayDeque<>();
    }

    public double likelihood() {
        return Math.exp(root.loglik());
    }

    public int add(Action action) {
        SplayEntry entry = new SplayEntry(action);
        if (root == null) {
            root = entry;
        } else {
            root = SplayEntry.merge(root, entry);
        }
        int id;
        if (freeIDs.isEmpty()) {
            id = map.size();
            map.add(entry);
        } else {
            id = freeIDs.poll();
            map.set(id, entry);
        }
        return id;
    }

    public Action randomAction(SplittableRandom re) {
        return root.randomEntry(re).action();
    }

    public void removeAction(int id) {
        SplayEntry entry = map.get(id);
        root = entry.remove();
        freeIDs.add(id);
    }
}
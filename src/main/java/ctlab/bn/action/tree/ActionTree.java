package ctlab.bn.action.tree;

import ctlab.bn.action.Action;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ActionTree {
    private SplayEntry root;
    private List<SplayEntry> map;
    private Queue<Integer> freeIDs;

    public ActionTree() {
        root = null;
        map = new ArrayList<>();
        freeIDs = new ArrayDeque<>();
    }

    public int addAction(Action action) {
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

    public void removeAction(int id) {
        SplayEntry entry = map.get(id);
        root = entry.remove();
        freeIDs.add(id);
    }
}

package ctlab.bn.action.tree;

import ctlab.bn.action.Action;
import ctlab.bn.action.ActionType;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.SplittableRandom;

public class ActionTreeTest {
    /**
     * Test that tree produces correct random actions
     */
    @Test
    public void testRandomAction() {
        ActionTree actions = new ActionTree();
        SplittableRandom re = new SplittableRandom();
        actions.add(new Action(ActionType.ADD, 1, 2, 1));
        actions.add(new Action(ActionType.ADD, 2, 2, -2000));
        actions.add(new Action(ActionType.ADD, 3, 2, 1));
        actions.add(new Action(ActionType.ADD, 4, 2, -2000));
        actions.add(new Action(ActionType.ADD, 5, 2, 1));
        actions.add(new Action(ActionType.ADD, 6, 2, -2000));
        actions.add(new Action(ActionType.ADD, 7, 2, 1));
        actions.add(new Action(ActionType.ADD, 8, 2, -2000));
        actions.add(new Action(ActionType.ADD, 9, 2, 1));
        actions.add(new Action(ActionType.ADD, 10, 2, -2000));
        int[] bins = new int[10];
        for (int i = 0; i < 10000; i++) {
            bins[actions.randomAction(re).v()]++;
        }
        actions.removeAction(1);
        actions.removeAction(3);
        actions.removeAction(5);
        actions.removeAction(7);
        actions.removeAction(9);
        for (int i = 0; i < 10000; i++) {
            bins[actions.randomAction(re).v()]++;
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(bins[i] > 1500);
            assertTrue(bins[i] > 2500);
        }
    }
}

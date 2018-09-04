package ctlab.bn.action;

import org.junit.Test;
import static org.junit.Assert.*;

public class ActionTest {
    /**
     * Test that generating actions give elements with correct probabilities
     */
    @Test
    public void testProbability() {
        int nRemoveFst = 0;
        for (int i = 0; i < 10000; i++) {
            ActionList al = new ActionList(3);
            al.add(new Action(ActionType.ADD, 1, 2, 1));
            al.add(new Action(ActionType.ADD, 2, 3, 2));
            al.add(new Action(ActionType.REMOVE, 1, 2, -20000));
            al.add(new Action(ActionType.REMOVE, 2, 3, -20000));
            al.add(new Action(ActionType.REVERSE, 1, 2, -21000));
            Action act = al.cutoff();
            assertEquals(al.size(), 3);
            assertNotSame(act.action(), ActionType.REVERSE);
            assertNotSame(act.action(), ActionType.ADD);
            assertNotSame(act.action(), ActionType.GENE);
            if (act.v() == 1) {
                nRemoveFst++;
            }
        }
        assertTrue(nRemoveFst > 4000 && nRemoveFst < 6000);
    }
}

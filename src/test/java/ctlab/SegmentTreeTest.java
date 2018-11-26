package ctlab;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.SplittableRandom;

public class SegmentTreeTest {
    /**
     * Test that tree produces correct random actions
     */
    @Test
    public void testRandomAction() {
        SegmentTree tree = new SegmentTree(10);
        SplittableRandom re = new SplittableRandom();
        tree.set(0, 1);
        tree.set(1, -2000);
        tree.set(2, 1);
        tree.set(3, -2000);
        tree.set(4, 1);
        tree.set(5, -2000);
        tree.set(6, 1);
        tree.set(7, -2000);
        tree.set(8, 1);
        tree.set(9, -2000);
        int[] bins = new int[10];
        for (int i = 0; i < 10000; i++) {
            bins[tree.randomChoice(re)]++;
        }
        tree.set(0, Double.NEGATIVE_INFINITY);
        tree.set(2, Double.NEGATIVE_INFINITY);
        tree.set(4, Double.NEGATIVE_INFINITY);
        tree.set(6, Double.NEGATIVE_INFINITY);
        tree.set(8, Double.NEGATIVE_INFINITY);
        for (int i = 0; i < 10000; i++) {
            bins[tree.randomChoice(re)]++;
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(bins[i] > 1800);
            assertTrue(bins[i] < 2200);
        }
        assertEquals(-2000 + Math.log(5), tree.likelihood(), 0.1);
    }
}

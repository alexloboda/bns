package ctlab.mcmc;

import ctlab.bn.action.Multinomial;

import java.util.List;

public class LinkedList {
    private ListNode fst;

    private static class ListNode {
        private ListNode left;
        private ListNode right;
        private Multinomial distribution;
        private List<Integer> ps;

        public Multinomial getDistribution() {
            return distribution;
        }

        public List<Integer> getParentSet() {
            return ps;
        }
    }
}

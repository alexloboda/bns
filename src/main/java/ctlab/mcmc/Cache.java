package ctlab.mcmc;

import ctlab.bn.action.Multinomial;

import java.util.*;
import java.util.function.Function;

class Cache {
    private Map<List<Integer>, LinkedList.Entry> cacheMap;
    private Function<List<Integer>, Multinomial> spark;
    private LinkedList queue;
    private int capacity;

    Cache(int nCachedStates, Function<List<Integer>, Multinomial> multinomialSpark) {
        cacheMap = new HashMap<>();
        spark = multinomialSpark;
        capacity = nCachedStates;
        queue = new LinkedList();
    }

    Multinomial request(List<Integer> ps) {
        LinkedList.Entry entry = cacheMap.get(ps);
        Multinomial mult;
        if (entry != null) {
            mult = entry.multinomial;
            entry.remove();
            queue.push(entry.multinomial, entry.ps);
        } else {
            mult = spark.apply(ps);
            if (queue.size == capacity) {
                LinkedList.Entry worst = queue.pop();
                cacheMap.remove(worst.ps);
            }
            LinkedList.Entry e = queue.push(mult, ps);
            cacheMap.put(ps, e);
        }
        return mult;
    }

    private static class LinkedList {
        private Entry first;
        private int size;

        Entry push(Multinomial mult, List<Integer> ps) {
            Entry e;
            if (first != null) {
                e = new Entry(mult, ps, first.left, first.right);
            } else {
                e = new Entry(mult, ps, null, null);
            }
            e.left.right = e;
            e.right.left = e;
            first = e;
            size++;
            return e;
        }

        Entry pop() {
            if (first == null) {
                throw new NoSuchElementException();
            }
            Entry e = first.left;
            e.remove();
            size--;
            return e;
        }

        class Entry {
            private Multinomial multinomial;
            private List<Integer> ps;
            private Entry left;
            private Entry right;

            private Entry(Multinomial m, List<Integer> ps, Entry left, Entry right) {
                this.multinomial = m;
                this.ps = ps;
                this.left = left != null ? left : this;
                this.right = right != null ? right : this;
            }

            public void remove() {
                if (first == this) {
                    first = first.right;
                    if (first == this) {
                        first = null;
                    }
                }
                left.right = right;
                right.left = left;
            }
        }
    }
}

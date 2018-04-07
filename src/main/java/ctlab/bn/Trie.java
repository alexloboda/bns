package ctlab.bn;

import java.util.ArrayList;
import java.util.List;

public class Trie {
    private int n;
    private Node root;
    private List<Integer> choices;

    public Trie(List<Integer> choices) {
        this.choices = new ArrayList<>(choices);
        this.choices.add(1);
        root = new Node(0);
    }

    public Selector selector() {
        return new Selector(root);
    }

    class Node {
        int c = -1;
        int lvl;
        Node[] ss;

        Node(int lvl) {
            ss = new Node[choices.get(lvl)];
            this.lvl = lvl;
        }
    }

    public class Selector {
        private Node ptr;

        Selector(Node ptr) {
            this.ptr = ptr;
        }

        public int get() {
            if (ptr.c == -1) {
                ptr.c = n++;
            }
            return ptr.c;
        }

        public void choose(int x) {
            if (ptr.ss[x] == null) {
                ptr.ss[x] = new Node(ptr.lvl + 1);
            }
            ptr = ptr.ss[x];
        }

        public void reuse() {
            ptr = root;
        }
    }
}
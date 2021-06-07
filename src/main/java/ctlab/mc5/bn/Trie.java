package ctlab.mc5.bn;

public class Trie {
    private int n;
    private Node root;
    private int[] choices;

    public Trie(int[] choices) {
        this.choices = choices;
        root = new Node();
    }

    public Selector selector() {
        return new Selector(root);
    }

    static class Node {
        int c = -1;
        Node[] ss;

        Node() {
            ss = new Node[16];
        }
    }

    static public class Selector {
        private Node ptr;
        private final Node ptrBase;
        private int n = 0;

        Selector(Node ptr) {
            this.ptr = ptr; this.ptrBase = ptr;
        }

        public int get() {
            if (ptr.c == -1) {
                ptr.c = n++;
            }
            return ptr.c;
        }

        public void choose(int x) {
            if (ptr.ss[x] == null) {
                ptr.ss[x] = new Node();
            }
            ptr = ptr.ss[x];
        }

        public void reuse() {
            ptr = ptrBase;
        }
    }
}
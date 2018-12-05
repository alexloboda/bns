package ctlab.bn.action;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

public class HashTable {
    private final static short INITIAL_CAPACITY = 4;
    private final static int p = 9871;

    private int capacity;
    private short size;
    private short[] table;

    private short a, b;

    private void init() {
        for (int i = 0; i < capacity; i++) {
            table[i] = -1;
        }
    }

    private void rehash() {
        int[] content = IntStream.range(0, table.length)
            .map(x -> table[x])
            .filter(x -> x != -1)
            .toArray();
        table = new short[capacity];
        init();
        size = 0;
        Arrays.stream(content).forEach(x -> add((short)x));
    }

    public HashTable() {
        SplittableRandom random = new SplittableRandom();
        a = (short)random.nextInt(p);
        b = (short)random.nextInt(-p + 1, p);
        capacity = INITIAL_CAPACITY;
        table = new short[capacity];
        init();
    }

    private int hash(short k) {
        return Math.floorMod((a * k + b) % p, capacity);
    }

    public void add(short k) {
        ensureCapacity(size + 1);
        Position pos = new Position(capacity, hash(k));
        for (int i = 0; i < capacity; i++) {
            int p = pos.step();
            if (table[p] == -1) {
                table[p] = k;
                size++;
                return;
            }
        }
        throw new IllegalStateException();
    }

    private Position locate(short k) {
        Position pos = new Position(capacity, hash(k));
        for (int i = 0; i < capacity; i++) {
            int p = pos.step();
            if (table[p] == k) {
                return pos;
            }
            if (table[p] == -1) {
                return null;
            }
        }
        return null;
    }

    public boolean contains(short k) {
        return locate(k) != null;
    }

    public void remove(short k) {
        Position pos = locate(k);
        int hash = hash(k);
        if (pos == null) {
            throw new NoSuchElementException();
        }
        int chainpos = pos.pos();
        for (int i = 0; i < capacity; i++) {
            int p = pos.step();
            if (table[p] == -1) {
                table[chainpos] = -1;
                return;
            }
            if (hash(table[p]) == hash) {
                table[chainpos] = table[p];
                chainpos = p;
            }
        }
    }

    private void ensureCapacity(int size) {
        if (size > capacity / 2) {
            capacity *= 2;
            rehash();
        }
    }

    private static class Position {
        private int pos;
        private int capacity;
        private int i;

        Position(int capacity, int pos) {
            this.capacity = capacity;
            this.pos = pos % capacity;
        }

        int pos() {
            return pos;
        }

        int step() {
            pos += i;
            pos %= capacity;
            i++;
            return pos;
        }
    }
}

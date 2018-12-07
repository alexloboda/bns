package ctlab.bn.action;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

public class HashTable {
    private final static int p = 9871;
    public static final int STEP = 3;

    private int capacity;
    private short size;
    private short[] table;

    private short a, b;

    private void rehash() {
        int[] content = IntStream.range(0, table.length)
            .map(x -> table[x])
            .filter(x -> x != -1)
            .toArray();
        assert content.length == size;
        table = new short[capacity];
        Arrays.fill(table, (short)-1);
        size = 0;
        Arrays.stream(content).forEach(x -> add((short)x));
    }

    public HashTable(int initialCapacity) {
        SplittableRandom random = new SplittableRandom();
        a = (short)random.nextInt(p);
        b = (short)random.nextInt(-p + 1, p);
        capacity = initialCapacity * 2;
        table = new short[capacity];
        Arrays.fill(table, (short)-1);
    }

    private int hash(short k) {
        return Math.floorMod((a * k + b) % p, capacity);
    }

    public void add(short k) {
        ensureCapacity(size + 1);
        int pos = hash(k) % capacity;
        for (int i = 0; i < capacity; i++) {
            pos = (pos + STEP) % capacity;
            if (table[pos] == -1) {
                table[pos] = k;
                size++;
                return;
            }
        }
        throw new IllegalStateException();
    }

    private int locate(short k) {
        int pos = hash(k) % capacity;
        for (int i = 0; i < capacity; i++) {
            pos = (pos + STEP) % capacity;
            if (table[pos] == k) {
                return pos;
            }
            if (table[pos] == -1) {
                return -1;
            }
        }
        return -1;
    }

    public boolean contains(short k) {
        return locate(k) != -1;
    }

    public int capacity() {
        return capacity;
    }

    public void remove(short k) {
        int pos = locate(k);
        if (pos == -1) {
            throw new NoSuchElementException();
        }
        size--;
        table[pos] = -1;
        for (int i = 0; i < capacity; i++) {
            pos = (pos + STEP) % capacity;
            short key = table[pos];
            if (key == -1) {
                return;
            }
            table[pos] = -1;
            size--;
            add(key);
        }
    }

    private void ensureCapacity(int size) {
        if (size > capacity / 2) {
            capacity *= 2;
            rehash();
        }
    }
}

package ctlab.bn.action;

import java.util.Comparator;
import java.util.NoSuchElementException;

public class Heap {
    private short[] heap;
    private int size;
    private Comparator<Short> comparator;

    public Heap(int capacity, Comparator<Short> comparator) {
        heap = new short[capacity];
        this.comparator = comparator;
    }

    private void siftDown(int k) {
        while (true) {
            int c1 = 2 * k + 1;
            int c2 = 2 * k + 2;
            if (c1 < size) {
                int min = comparator.compare(heap[k], heap[c1]) <= 0 ? k : c1;
                if (c2 < size) {
                    min = comparator.compare(heap[min], heap[c2]) <= 0 ? min : c2;
                }
                if (k == min) {
                    return;
                } else {
                    short t = heap[k];
                    heap[k] = heap[min];
                    heap[min] = t;
                    k = min;
                }
            } else {
                return;
            }
        }
    }

    private void siftUp(int k) {
        while (k != 0) {
            int j = (k - 1) / 2;
            if (comparator.compare(heap[k], heap[j]) < 0) {
                short t = heap[k];
                heap[k] = heap[j];
                heap[j] = t;
                k = j;
            } else {
                return;
            }
        }
    }

    public void add(short value) {
        heap[size] = value;
        siftUp(size);
        size++;
    }

    public short min() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return heap[0];
    }

    public short extractMin() {
        short min = min();
        size--;
        if (size != 0) {
            heap[0] = heap[size];
            siftDown(0);
        }
        return min;
    }
}

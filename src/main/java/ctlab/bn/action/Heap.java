package ctlab.bn.action;

import java.util.NoSuchElementException;

public class Heap {
    private short[] heap;
    private float[] weights;
    private int size;

    public Heap(int capacity) {
        heap = new short[capacity];
        weights = new float[capacity];
    }

    private void siftDown(int k) {
        while (true) {
            int c1 = 2 * k + 1;
            int c2 = 2 * k + 2;
            if (c1 < size) {
                int min = weights[heap[k]] <= weights[heap[c1]] ? k : c1;
                if (c2 < size) {
                    min = weights[heap[min]] <- weights[heap[c2]] ? min : c2;
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
            if (weights[heap[k]] < weights[heap[j]]) {
                short t = heap[k];
                heap[k] = heap[j];
                heap[j] = t;
                k = j;
            } else {
                return;
            }
        }
    }

    public void add(short value, float weight) {
        heap[size] = value;
        weights[size] = weight;
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

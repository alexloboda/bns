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

    private void swap(int i, int j) {
        short t = heap[i];
        heap[i] = heap[j];
        heap[j] = t;
        float tmp = weights[i];
        weights[i] = weights[j];
        weights[j] = tmp;
    }

    private void siftDown(int k) {
        while (true) {
            int c1 = 2 * k + 1;
            int c2 = 2 * k + 2;
            if (c1 < size) {
                int min = weights[k] <= weights[c1] ? k : c1;
                if (c2 < size) {
                    min = weights[min] <= weights[c2] ? min : c2;
                }
                if (k == min) {
                    return;
                } else {
                    swap(k, min);
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
            if (weights[k] < weights[j]) {
                swap(k, j);
                k = j;
            } else {
                return;
            }
        }
    }

    public void add(short value, double weight) {
        heap[size] = value;
        weights[size] = (float)weight;
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
            weights[0] = weights[size];
            siftDown(0);
        }
        return min;
    }
}

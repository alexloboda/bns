package ctlab.mc5.bn.action;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HeapTest {
    @Test
    public void heapTest() {
        int inputSize = 1024;
        Random random = new Random(42);
        double[] weights = random.doubles(100).toArray();
        int[] positions = random.ints(inputSize, 0, 100).toArray();
        Comparator<Short> cmp = Comparator.comparingDouble(i -> weights[positions[i]]);
        PriorityQueue<Short> queue = new PriorityQueue<>(cmp);
        Heap heap = new Heap(inputSize);
        List<Short> input = IntStream.range(0, inputSize).mapToObj(x -> (short)x).collect(Collectors.toList());
        Collections.shuffle(input, random);
        for (int i = 0; i < inputSize; i++) {
            if (!queue.isEmpty() && random.nextDouble() < 0.1) {
                Assert.assertEquals(weights[positions[queue.poll()]], weights[positions[heap.extractMin()]], 1e-6);
            }
            short w = input.get(i);
            queue.add(w);
            heap.add(w, (float)weights[positions[w]]);
        }
        while(!queue.isEmpty()) {
            Assert.assertEquals(weights[positions[queue.poll()]], weights[positions[heap.extractMin()]], 1e-6);
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyHeapTest() {
        Heap heap = new Heap(16);
        heap.add((short)5, 0.5f);
        heap.extractMin();
        heap.extractMin();
    }
}

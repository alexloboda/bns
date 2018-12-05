package ctlab.bn.action;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;

public class HashTableTest {
    public static final int MAX_VALUE = 128;

    @Test
    public void test() {
        HashTable table = new HashTable();
        HashSet<Short> reference = new HashSet<>();
        Random random = new Random(40);
        for (int i = 0; i < 10000; i++) {
            short key = (short)random.nextInt(MAX_VALUE);
            if (reference.contains(key)) {
                reference.remove(key);
                table.remove(key);
            } else {
                reference.add(key);
                table.add(key);
            }
            for (short j = 0; j < MAX_VALUE; j++) {
                if (reference.contains(j) != table.contains(j)) {
                    System.out.println(i);
                }
                Assert.assertEquals(reference.contains(j), table.contains(j));
            }
        }
    }
}

package ctlab.bn.action;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Random;

public class HashTableTest {
    public static final int MAX_VALUE = 256;

    @Test
    public void test() {
        Random random = new Random(42);
        for (int k = 0; k < 1000; k++) {
            HashTable table = new HashTable(4);
            HashMap<Short, Short> reference = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                short key = (short) random.nextInt(MAX_VALUE);
                if (reference.containsKey(key)) {
                    reference.remove(key);
                    table.remove(key);
                } else {
                    short value = (short) (random.nextInt() % MAX_VALUE);
                    reference.put(key, value);
                    table.put(key, value);
                }
                for (short j = 0; j < MAX_VALUE; j++) {
                    Assert.assertEquals(reference.containsKey(j), table.contains(j));
                    if (reference.containsKey(j)) {
                        Assert.assertEquals((short) reference.get(j), table.get(j));
                    }
                }
            }
        }
    }
}

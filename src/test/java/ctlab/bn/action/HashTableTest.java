package ctlab.bn.action;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;

public class HashTableTest {
    @Test
    public void test() {
        HashTable table = new HashTable();
        HashSet<Short> reference = new HashSet<>();
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            short key = (short)random.nextInt(Short.MAX_VALUE);
            if (reference.contains(key)) {
                reference.remove(key);
                table.remove(key);
            } else {
                reference.add(key);
                table.add(key);
            }
            for (short j = 0; j < Short.MAX_VALUE; j++) {
                Assert.assertEquals(reference.contains(j), table.contains(j));
            }
        }
    }
}

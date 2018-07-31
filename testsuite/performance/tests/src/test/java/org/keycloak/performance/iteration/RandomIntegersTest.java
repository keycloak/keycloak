package org.keycloak.performance.iteration;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.performance.AbstractTest;

/**
 *
 * @author tkyjovsk
 */
public class RandomIntegersTest extends AbstractTest {

    Random r;

    @Before
    public void before() {
        r = new Random();
    }

    @Test
    @Ignore
    public void testRandoms() {
        int seed1 = r.nextInt();
        int seed2 = r.nextInt();

        List<Integer> l1 = new RandomIntegers(seed1, 20).stream().limit(10).collect(Collectors.toList());
        List<Integer> l2 = new RandomIntegers(seed1, 20).stream().limit(10).collect(Collectors.toList());
        List<Integer> l3 = new RandomIntegers(seed2, 20).stream().limit(10).collect(Collectors.toList());

        logger.info("L1: " + l1);
        logger.info("L2: " + l2);
        logger.info("L3: " + l3);

        assertFalse(l1.isEmpty());
        assertFalse(l2.isEmpty());
        assertFalse(l3.isEmpty());
        assertEquals(l1, l2);
        assertNotEquals(l2, l3);
    }

    @Test
    @Ignore
    public void testUniqueRandoms() {
        int seed1 = r.nextInt();
        int seed2 = r.nextInt();

        List<Integer> l1 = new UniqueRandomIntegers(seed1, 20).stream().limit(10).collect(Collectors.toList());
        List<Integer> l2 = new UniqueRandomIntegers(seed1, 20).stream().limit(10).collect(Collectors.toList());
        List<Integer> l3 = new UniqueRandomIntegers(seed2, 20).stream().limit(10).collect(Collectors.toList());

        logger.info("unique L1: " + l1);
        logger.info("unique L2: " + l2);
        logger.info("unique L3: " + l3);

        assertFalse(l1.isEmpty());
        assertFalse(l2.isEmpty());
        assertFalse(l3.isEmpty());
        assertEquals(l1, l2);
        assertNotEquals(l2, l3);
    }

    @Test
    @Ignore
    public void testStream() {
        ThreadLocalRandom.current().ints(0, 100).distinct().limit(5).forEach(System.out::println);
    }

}

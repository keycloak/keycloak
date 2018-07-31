package org.keycloak.performance.iteration;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.performance.AbstractTest;

/**
 *
 * @author tkyjovsk
 */
public class RandomBooleansTest extends AbstractTest {

    Random r;

    @Before
    public void before() {
        r = new Random();
    }

    @Test
//    @Ignore
    public void testRandoms() {
        int seed1 = r.nextInt();
        int seed2 = r.nextInt();

        List<Boolean> list1 = new RandomBooleans(seed1, 50).stream().limit(10).collect(Collectors.toList());
        List<Boolean> list2 = new RandomBooleans(seed1, 50).stream().limit(10).collect(Collectors.toList());
        List<Boolean> list3 = new RandomBooleans(seed2, 50).stream().limit(10).collect(Collectors.toList());

        logger.info(String.format("List1(seed: %s): %s", seed1, list1));
        logger.info(String.format("List2(seed: %s): %s", seed1, list2));
        logger.info(String.format("List3(seed: %s): %s", seed2, list3));

        assertFalse(list1.isEmpty());
        assertFalse(list2.isEmpty());
        assertFalse(list3.isEmpty());
        assertEquals(list1, list2);
        assertNotEquals(list2, list3);
    }

}

package org.keycloak.performance.dataset;

import java.util.Iterator;
import java.util.stream.Stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.performance.templates.DatasetTemplate;
import org.keycloak.performance.util.Loggable;

/**
 *
 * @author tkyjovsk
 */
public abstract class EntityTest<T extends Entity> implements Loggable {

    private Dataset d1;
    private Dataset d2;

    @Before
    public void prepareDatasets() {
        freemarker.template.Configuration fmc = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_26);
        fmc.setBooleanFormat("true,false");
        fmc.setNumberFormat("computer");

        DatasetTemplate dt = new DatasetTemplate();
        d1 = dt.produce();
//        logger().info("created: " + d1);
        d2 = dt.produce();
//        logger().info("created: " + d2);
    }

    public Dataset getD1() {
        return d1;
    }

    public Dataset getD2() {
        return d2;
    }

    public abstract Stream<T> entityStream(Dataset dataset);

    public Stream<T> d1EntityStream() {
        return entityStream(getD1());
    }

    public Stream<T> d2EntityStream() {
        return entityStream(getD2());
    }

    @Test
    public void testHashCode() {
        logger().info(this.getClass().getSimpleName() + " testHashCode()");
        Iterator<T> e1i = d1EntityStream().iterator();
        Iterator<T> e2i = d2EntityStream().iterator();
        int checkCount = 0;
        while (e1i.hasNext()) {
            T e1 = e1i.next();
            assertTrue(e2i.hasNext());
            T e2 = e2i.next();

//            logger().info(String.format("entities: %s %s", e1, e2));
//            logger().info(String.format("hashCodes: %s %s", e1.hashCode(), e2.hashCode()));
            assertEquals(e1.hashCode(), e1.hashCode());
            assertEquals(e2.hashCode(), e2.hashCode());
            assertEquals(e1.hashCode(), e2.hashCode());

            checkCount++;
//            if (checkCount > 10) {
//                break;
//            }
        }
        logger().info("checkCount: " + checkCount);
//        assertTrue(checkCount > 0);
    }

    @Test
    public void testEquals() {
        logger().info(this.getClass().getSimpleName() + " testEquals()");
        Iterator<T> e1i = d1EntityStream().iterator();
        Iterator<T> e2i = d2EntityStream().iterator();
        int checkCount = 0;
        while (e1i.hasNext()) {
            T e1 = e1i.next();
            assertTrue(e2i.hasNext());
            T e2 = e2i.next();
//            logger().info(String.format("entities: %s %s", e1, e2));
            assertTrue(e1.equals(e2));
            assertTrue(e2.equals(e1));

            checkCount++;
//            if (checkCount > 10) {
//                break;
//            }
        }
        logger().info("checkCount: " + checkCount);
//        assertTrue(checkCount > 0);
    }

}

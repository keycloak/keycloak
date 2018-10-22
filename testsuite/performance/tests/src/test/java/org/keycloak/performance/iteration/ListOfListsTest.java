package org.keycloak.performance.iteration;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.performance.util.Loggable;

/**
 *
 * @author tkyjovsk
 */
public class ListOfListsTest implements Loggable {

    int[] sizes;
    List<List<String>> lists;
    List<String> items;

    @Before
    public void prepareLists() {
        sizes = new Random().ints(10, 1, 10).toArray();
        lists = new LinkedList<>();
        items = new LinkedList<>();
        for (int l = 0; l < sizes.length; l++) {
            List<String> list = new LinkedList<>();
            for (int i = 0; i < sizes[l]; i++) {
                list.add(String.format("list %s item %s", l, i));
            }
            lists.add(list);
            items.addAll(list);
        }
    }

    @Test
    public void testSize() {
        lists.forEach(l -> logger().debug(l));
        ListOfLists<String> lol = new ListOfLists<>(lists);
        assertEquals(items.size(), lol.size());
        for (int i = 0; i < items.size(); i++) {
            assertEquals(items.get(i), lol.get(i));
        }
    }

}

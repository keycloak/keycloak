package org.keycloak.services.models.utils;

import org.junit.Assert;
import org.junit.Test;

public class ArrayUtilsTest {

    @Test
    public void add() {
        String[] a = new String[] { "a" };
        a = ArrayUtils.add(a, "b");
        Assert.assertArrayEquals(new String[] { "a", "b" }, a);

        a = ArrayUtils.add(a, "c");
        Assert.assertArrayEquals(new String[] { "a", "b", "c" }, a);
    }

    @Test
    public void remove() {
        String[] a = new String[] { "a", "b", "c", "d" };

        a = ArrayUtils.remove(a, "b");
        Assert.assertArrayEquals(new String[] { "a", "c", "d" }, a);

        a = ArrayUtils.remove(a, "d");
        Assert.assertArrayEquals(new String[] { "a", "c" }, a);

        a = ArrayUtils.remove(a, "a");
        Assert.assertArrayEquals(new String[] { "c" }, a);

        a = ArrayUtils.remove(a, "c");
        Assert.assertArrayEquals(new String[] {}, a);
    }

}

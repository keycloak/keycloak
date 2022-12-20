package org.keycloak.testsuite.runonserver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MyFlakyTest {

    @Test
    public void flakyTest() {
        assertEquals(1, Count.getAndIncrease());
    }

}

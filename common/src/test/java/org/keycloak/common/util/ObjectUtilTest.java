package org.keycloak.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectUtilTest {

    @Test
    public void shouldCapitalizeCorrectly() {
        assertEquals("", ObjectUtil.capitalize(""));
        assertEquals("T", ObjectUtil.capitalize("t"));
        assertEquals("Test", ObjectUtil.capitalize("test"));
        assertEquals("Test string", ObjectUtil.capitalize("test string"));
    }

}

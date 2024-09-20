package org.keycloak.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeystoreUtilTest {

    @Test
    public void testGetType() {
        assertEquals("x", KeystoreUtil.getKeystoreType("x", "y", "z"));
        assertEquals("z", KeystoreUtil.getKeystoreType(null, "y", "z"));
        assertEquals(KeystoreUtil.KeystoreFormat.PKCS12.name(), KeystoreUtil.getKeystoreType(null, "y.pfx", "z"));
    }

}
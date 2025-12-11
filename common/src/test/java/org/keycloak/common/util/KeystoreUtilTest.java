package org.keycloak.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class KeystoreUtilTest {

    @Test
    public void testGetType() {
        assertEquals("x", KeystoreUtil.getKeystoreType("x", "y", "z"));
        assertEquals("z", KeystoreUtil.getKeystoreType(null, "y", "z"));
        assertEquals(KeystoreUtil.KeystoreFormat.PKCS12.name(), KeystoreUtil.getKeystoreType(null, "y.pfx", "z"));
        assertEquals(KeystoreUtil.KeystoreFormat.PKCS12.name(), KeystoreUtil.getKeystoreType(null, "y.pkcs12", "z"));
    }
    
    @Test
    public void testGetFormat() {
        assertFalse(KeystoreUtil.getKeystoreFormat("some.file").isPresent());
        assertFalse(KeystoreUtil.getKeystoreFormat("somepfx").isPresent());
        assertEquals(KeystoreUtil.KeystoreFormat.PKCS12, KeystoreUtil.getKeystoreFormat("file.pfx").get());
        assertEquals(KeystoreUtil.KeystoreFormat.JKS, KeystoreUtil.getKeystoreFormat("file.jks").get());
    }

}
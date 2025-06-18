package org.keycloak.common.enums;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class SslRequiredTest {

    @Test
    public void sslRequiredExternalTest() throws IOException {
        assertFalse(SslRequired.EXTERNAL.isRequired("127.0.0.1"));
        assertTrue(SslRequired.EXTERNAL.isRequired((String)null));
        assertTrue(SslRequired.EXTERNAL.isRequired(""));
        assertTrue(SslRequired.EXTERNAL.isRequired("0.0.0.0"));
    }

}

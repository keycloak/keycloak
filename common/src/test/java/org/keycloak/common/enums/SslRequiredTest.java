package org.keycloak.common.enums;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SslRequiredTest {

    @Test
    public void sslRequiredExternalTest() throws IOException {
        assertFalse(SslRequired.EXTERNAL.isRequired("127.0.0.1"));
        assertTrue(SslRequired.EXTERNAL.isRequired((String)null));
        assertTrue(SslRequired.EXTERNAL.isRequired(""));
        assertTrue(SslRequired.EXTERNAL.isRequired("0.0.0.0"));
    }

}

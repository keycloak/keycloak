package org.keycloak.common.crypto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class CryptoIntegrationTest {
    private static CryptoProvider originalProvider;

    @BeforeAll
    public static void keepOriginalProvider() {
        CryptoIntegrationTest.originalProvider = getSelectedProvider();
    }

    // doing our best to avoid any side effects on other tests by restoring the initial state of CryptoIntegration
    @AfterAll
    public static void restoreOriginalProvider() {
        CryptoIntegration.setProvider(originalProvider);
    }

    @Test
    public void canSetNullProvider() {
        CryptoIntegration.setProvider(null);
        assertNull(getSelectedProvider());
    }

    private static CryptoProvider getSelectedProvider() {
        try {
            return CryptoIntegration.getProvider();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}

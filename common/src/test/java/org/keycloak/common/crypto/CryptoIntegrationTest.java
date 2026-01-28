package org.keycloak.common.crypto;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class CryptoIntegrationTest {
    private static CryptoProvider originalProvider;

    @BeforeClass
    public static void keepOriginalProvider() {
        CryptoIntegrationTest.originalProvider = getSelectedProvider();
    }

    // doing our best to avoid any side effects on other tests by restoring the initial state of CryptoIntegration
    @AfterClass
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

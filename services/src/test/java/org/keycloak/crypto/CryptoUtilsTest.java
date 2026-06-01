package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.utils.AbstractUtilSessionTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CryptoUtilsTest extends AbstractUtilSessionTest {

    @Test
    public void getSignatureProviderReturnsProviderForSupportedAlgorithm() throws VerificationException {
        assertNotNull(CryptoUtils.getSignatureProvider(session, "RS256"));
    }

    @Test
    public void getSignatureProviderThrowsForNoneAlgorithm() {
        VerificationException thrown = assertThrows(VerificationException.class,
                () -> CryptoUtils.getSignatureProvider(session, "none"));
        assertThat(thrown.getMessage(), containsString("none"));
    }

    @Test
    public void getSignatureProviderThrowsForUnknownAlgorithm() {
        VerificationException thrown = assertThrows(VerificationException.class,
                () -> CryptoUtils.getSignatureProvider(session, "FAKE256"));
        assertThat(thrown.getMessage(), containsString("FAKE256"));
    }

    @Test
    public void getSignatureProviderThrowsForNullAlgorithm() {
        assertThrows(VerificationException.class,
                () -> CryptoUtils.getSignatureProvider(session, null));
    }

}

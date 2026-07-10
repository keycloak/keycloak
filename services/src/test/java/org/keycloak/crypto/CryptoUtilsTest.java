package org.keycloak.crypto;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.utils.AbstractUtilSessionTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CryptoUtilsTest extends AbstractUtilSessionTest {

    @Test
    public void getSignatureProviderReturnsProviderForSupportedAlgorithm() throws VerificationException {
        assertNotNull(CryptoUtils.getSignatureProvider(session, "RS256"));
        assertNotNull(CryptoUtils.getSignatureProvider(session, "ML-DSA-44"));
        assertNotNull(CryptoUtils.getSignatureProvider(session, "ML-DSA-65"));
        assertNotNull(CryptoUtils.getSignatureProvider(session, "ML-DSA-87"));
    }

    @Test
    public void mldsaSignaturesCanBeCreatedAndVerified() throws Exception {
        byte[] data = "ML-DSA signature test".getBytes(StandardCharsets.UTF_8);
        byte[] differentData = "tampered ML-DSA signature test".getBytes(StandardCharsets.UTF_8);

        for (String algorithm : new String[] { Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87 }) {
            KeyPair keyPair = CryptoIntegration.getProvider().getKeyPairGen(algorithm).generateKeyPair();
            KeyWrapper key = new KeyWrapper();
            key.setAlgorithm(algorithm);
            key.setType(KeyType.AKP);
            key.setPrivateKey(keyPair.getPrivate());
            key.setPublicKey(keyPair.getPublic());

            SignatureProvider provider = CryptoUtils.getSignatureProvider(session, algorithm);
            byte[] signature = provider.signer(key).sign(data);

            assertTrue(provider.verifier(key).verify(data, signature), algorithm);
            assertFalse(provider.verifier(key).verify(differentData, signature), algorithm);
        }
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

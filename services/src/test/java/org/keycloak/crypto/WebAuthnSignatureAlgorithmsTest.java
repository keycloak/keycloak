package org.keycloak.crypto;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;

import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebAuthnSignatureAlgorithmsTest {

    @BeforeEach
    @AfterEach
    public void resetProfile() {
        Profile.defaults();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void featureControlsMLDSA(boolean enabled) {
        if (enabled) {
            Profile.configure(new CommaSeparatedListProfileConfigResolver("webauthn-ml-dsa", null));
        }

        assertEquals(enabled, WebAuthnSignatureAlgorithms.getSupportedAlgorithms().containsKey(Algorithm.ML_DSA_44));
        assertEquals(enabled, WebAuthnSignatureAlgorithms.getSupportedAlgorithms().containsKey(Algorithm.ML_DSA_65));
        assertEquals(enabled, WebAuthnSignatureAlgorithms.getSupportedAlgorithms().containsKey(Algorithm.ML_DSA_87));
        assertEquals(enabled, WebAuthnSignatureAlgorithms.isEnabled(COSEAlgorithmIdentifier.ML_DSA_44));
        assertEquals(enabled, WebAuthnSignatureAlgorithms.isEnabled(COSEAlgorithmIdentifier.ML_DSA_65));
        assertEquals(enabled, WebAuthnSignatureAlgorithms.isEnabled(COSEAlgorithmIdentifier.ML_DSA_87));

        assertTrue(WebAuthnSignatureAlgorithms.isEnabled(COSEAlgorithmIdentifier.ES256));
        assertTrue(WebAuthnSignatureAlgorithms.isEnabled(COSEAlgorithmIdentifier.RS256));
    }
}

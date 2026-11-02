package org.keycloak.crypto;

import java.util.Map;

import org.keycloak.common.Profile;

import com.google.common.collect.ImmutableMap;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;

public final class WebAuthnSignatureAlgorithms {

    private static final Map<String, COSEAlgorithmIdentifier> BASE_ALGORITHMS = ImmutableMap.of(
            Algorithm.ES256, COSEAlgorithmIdentifier.ES256,
            Algorithm.ES384, COSEAlgorithmIdentifier.ES384,
            Algorithm.ES512, COSEAlgorithmIdentifier.ES512,
            Algorithm.RS256, COSEAlgorithmIdentifier.RS256,
            Algorithm.RS384, COSEAlgorithmIdentifier.RS384,
            Algorithm.RS512, COSEAlgorithmIdentifier.RS512,
            Algorithm.Ed25519, COSEAlgorithmIdentifier.EdDSA,
            "RS1", COSEAlgorithmIdentifier.RS1);

    private static final Map<String, COSEAlgorithmIdentifier> ML_DSA_ALGORITHMS = ImmutableMap.of(
            Algorithm.ML_DSA_44, COSEAlgorithmIdentifier.ML_DSA_44,
            Algorithm.ML_DSA_65, COSEAlgorithmIdentifier.ML_DSA_65,
            Algorithm.ML_DSA_87, COSEAlgorithmIdentifier.ML_DSA_87);

    private static final Map<String, COSEAlgorithmIdentifier> ALL_ALGORITHMS =
            ImmutableMap.<String, COSEAlgorithmIdentifier>builder()
                    .putAll(BASE_ALGORITHMS)
                    .putAll(ML_DSA_ALGORITHMS)
                    .buildOrThrow();

    private WebAuthnSignatureAlgorithms() {
    }

    public static Map<String, COSEAlgorithmIdentifier> getSupportedAlgorithms() {
        return Profile.isFeatureEnabled(Profile.Feature.WEBAUTHN_ML_DSA) ? ALL_ALGORITHMS : BASE_ALGORITHMS;
    }

    public static boolean isEnabled(COSEAlgorithmIdentifier algorithm) {
        return getSupportedAlgorithms().containsValue(algorithm);
    }
}

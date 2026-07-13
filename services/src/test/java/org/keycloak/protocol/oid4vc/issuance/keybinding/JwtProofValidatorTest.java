package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.Base64Url;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.SupportedProofTypeData;
import org.keycloak.utils.AbstractUtilSessionTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JwtProofValidatorTest extends AbstractUtilSessionTest {

    @Test
    public void testValidateNoPrivateKeyInHeaderClaims_RS256_Blocked() {
        assertBlocked("RS256", "RSA", "d", "secret");
    }

    @Test
    public void testValidateNoPrivateKeyInHeaderClaims_RS256_BlockedBy_P_Claim() {
        // Test that RSA rejects other private claims like 'p' even if 'd' is absent
        assertBlocked("RS256", "RSA", "p", "secret-p");
    }

    @Test
    public void testValidateNoPrivateKeyInHeaderClaims_RS256_Allowed() {
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("n", "modulus");
        jwk.put("e", "exponent");
        assertAllowed("RS256", jwk);
    }

    @Test
    public void testValidateNoPrivateKeyInHeaderClaims_PS256_Blocked() {
        assertBlocked("PS256", "RSA", "d", "secret");
    }

    @Test
    public void testValidateNoPrivateKeyInHeaderClaims_ES256_Blocked() {
        assertBlocked("ES256", "EC", "d", "secret");
    }

    @Test
    public void testValidateNoPrivateKeyInHeaderClaims_ES256_Allowed() {
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "EC");
        jwk.put("x", "coord-x");
        jwk.put("y", "coord-y");
        assertAllowed("ES256", jwk);
    }

    @Test
    public void testValidateNoPrivateKeyInHeaderClaims_EdDSA_Blocked() {
        assertBlocked("EdDSA", "OKP", "d", "secret");
    }

    @Test
    public void testValidateNoPrivateKeyInHeaderClaims_UnknownAlgorithm_Blocked() {
        // FAKE256 has no registered provider, should throw
        assertBlocked("FAKE256", "RSA", "d", "secret");
    }

    /**
     * Regression test for OID4VC JWT proof JWK claim type confusion.
     * <p>
     * Verifies that a malformed JWT proof with {@code "crv": []} (array instead of string)
     * in the JWK header does NOT throw {@link ClassCastException}, but instead
     * produces a proper {@link VCIssuerException} with {@link ErrorType#INVALID_PROOF}.
     * <p>
     * Before the fix, the attacker-supplied array was stored as {@link java.util.ArrayList}
     * in the JWK's otherClaims map, and the cast {@code String.class.cast(arrayList)}
     * thrown an unchecked {@link ClassCastException} that escaped the expected error path.
     */
    @Test
    public void testMalformedJwkCrvInJwtProofReturnsInvalidProof() {
        // Build a compact JWT with malformed jwk: "crv": [] (array instead of string)
        String headerJson = "{\"alg\":\"ES256\",\"typ\":\"openid4vci-proof+jwt\",\"jwk\":{\"crv\":[]}}";
        // Payload is minimal since the malformed crv fails validation before payload checks are reached
        String payloadJson = "{}";
        String signature = Base64Url.encode("fake-signature".getBytes(StandardCharsets.UTF_8));

        String compactJwt = Base64Url.encode(headerJson.getBytes(StandardCharsets.UTF_8)) + "."
                + Base64Url.encode(payloadJson.getBytes(StandardCharsets.UTF_8)) + "."
                + signature;

        // Build a VCIssuanceContext with the malformed proof
        SupportedCredentialConfiguration credentialConfig = new SupportedCredentialConfiguration();
        ProofTypesSupported proofTypesSupported = new ProofTypesSupported();
        SupportedProofTypeData jwtProofTypeData = new SupportedProofTypeData();
        proofTypesSupported.setSupportedProofTypes("jwt", jwtProofTypeData);
        credentialConfig.setProofTypesSupported(proofTypesSupported);
        credentialConfig.setCryptographicBindingMethodsSupported(List.of("jwk"));

        CredentialRequest credentialRequest = new CredentialRequest();
        Proofs proofs = Proofs.create("jwt", compactJwt);
        credentialRequest.setProofs(proofs);

        VCIssuanceContext context = new VCIssuanceContext()
                .setCredentialConfig(credentialConfig)
                .setCredentialRequest(credentialRequest);

        JwtProofValidator validator = new JwtProofValidator(session, null);

        // The malformed proof should be rejected with INVALID_PROOF (not crash with ClassCastException)
        VCIssuerException exception = assertThrows(
                VCIssuerException.class,
                () -> validator.validateProof(context));

        // Verify the error is the expected INVALID_PROOF type
        assertEquals(ErrorType.INVALID_PROOF, exception.getErrorType());
    }

    private void assertBlocked(String alg, String kty, String claim, Object value) {
        JwtProofValidator validator = new JwtProofValidator(session, null);
        Map<String, Object> headerClaims = new HashMap<>();
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", kty);
        jwk.put(claim, value);
        headerClaims.put("jwk", jwk);

        assertThrows(VCIssuerException.class, () -> validator.validateNoPrivateKeyInHeaderClaims(alg, headerClaims));
    }

    private void assertAllowed(String alg, Map<String, Object> jwk) {
        JwtProofValidator validator = new JwtProofValidator(session, null);
        Map<String, Object> headerClaims = new HashMap<>();
        headerClaims.put("jwk", jwk);

        assertDoesNotThrow(() -> validator.validateNoPrivateKeyInHeaderClaims(alg, headerClaims));
    }
}

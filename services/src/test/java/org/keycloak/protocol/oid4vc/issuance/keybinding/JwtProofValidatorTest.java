package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.ErrorType;
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

    @Test
    public void testProofKeyAcceptedAtAnyPositionWhenNoIndexRequired() {
        JWK firstKey = newEcJwk();
        JWK secondKey = newEcJwk();

        assertDoesNotThrow(() -> new JwtProofValidator(session, null)
                .validateProofKeyAttestation(List.of(firstKey, secondKey), copyOf(secondKey), null));
    }

    @Test
    public void testProofKeyOutsideAttestationRejectedWhenNoIndexRequired() {
        VCIssuerException exception = assertThrows(VCIssuerException.class, () -> new JwtProofValidator(session, null)
                .validateProofKeyAttestation(List.of(newEcJwk()), copyOf(newEcJwk()), null));

        assertEquals(ErrorType.INVALID_PROOF, exception.getErrorType());
        assertEquals("JWT proof key is not included in attested_keys", exception.getMessage());
    }

    @Test
    public void testProofKeyAcceptedAtFirstPositionWhenIndexZeroRequired() {
        JWK firstKey = newEcJwk();
        JWK secondKey = newEcJwk();

        assertDoesNotThrow(() -> new JwtProofValidator(session, null)
                .validateProofKeyAttestation(List.of(firstKey, secondKey), copyOf(firstKey), 0));
    }

    @Test
    public void testProofKeyAtLaterPositionRejectedWhenIndexZeroRequired() {
        JWK firstKey = newEcJwk();
        JWK secondKey = newEcJwk();

        VCIssuerException exception = assertThrows(VCIssuerException.class, () -> new JwtProofValidator(session, null)
                .validateProofKeyAttestation(List.of(firstKey, secondKey), copyOf(secondKey), 0));

        assertEquals(ErrorType.INVALID_PROOF, exception.getErrorType());
        assertEquals("JWT proof key is not attested_keys[0], it is at index 1", exception.getMessage());
    }

    @Test
    public void testProofKeyOutsideAttestationRejectedWhenIndexZeroRequired() {
        VCIssuerException exception = assertThrows(VCIssuerException.class, () -> new JwtProofValidator(session, null)
                .validateProofKeyAttestation(List.of(newEcJwk()), copyOf(newEcJwk()), 0));

        assertEquals(ErrorType.INVALID_PROOF, exception.getErrorType());
        assertEquals("JWT proof key is not attested_keys[0]", exception.getMessage());
    }

    private static JWK newEcJwk() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return JWKBuilder.create().ec(generator.generateKeyPair().getPublic());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JWK copyOf(JWK jwk) {
        return JWKBuilder.create().ec(JWKParser.create(jwk).toPublicKey());
    }
}

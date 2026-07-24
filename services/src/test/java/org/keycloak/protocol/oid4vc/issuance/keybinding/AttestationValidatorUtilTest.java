package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.util.List;
import java.util.Map;

import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.model.KeyAttestationsRequired;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.SupportedProofTypeData;
import org.keycloak.utils.AbstractUtilSessionTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AttestationValidatorUtilTest extends AbstractUtilSessionTest {

    @Test
    public void testGetAttestationRequirements_ReadsFromJwtKeyWhenRequested() {
        KeyAttestationsRequired jwtAttestation = new KeyAttestationsRequired()
                .setKeyStorage(List.of("hardware"))
                .setUserAuthentication(List.of("pin"));

        VCIssuanceContext ctx = createContextWithAttestation(
                jwtAttestation,
                new KeyAttestationsRequired()
                        .setKeyStorage(List.of("tpm"))
                        .setUserAuthentication(List.of("fingerprint"))
        );

        KeyAttestationsRequired result = AttestationValidatorUtil.getAttestationRequirements(ctx, ProofType.JWT);

        Assertions.assertNotNull(result);
        assertEquals(List.of("hardware"), result.getKeyStorage(),
                "Should read key_storage from the jwt proof type entry");
        assertEquals(List.of("pin"), result.getUserAuthentication(),
                "Should read user_authentication from the jwt proof type entry");
    }

    @Test
    public void testGetAttestationRequirements_ReadsFromAttestationKeyWhenRequested() {
        KeyAttestationsRequired attestationAttestation = new KeyAttestationsRequired()
                .setKeyStorage(List.of("tpm"))
                .setUserAuthentication(List.of("fingerprint"));

        VCIssuanceContext ctx = createContextWithAttestation(
                new KeyAttestationsRequired()
                        .setKeyStorage(List.of("hardware"))
                        .setUserAuthentication(List.of("pin")),
                attestationAttestation
        );

        KeyAttestationsRequired result = AttestationValidatorUtil.getAttestationRequirements(ctx, ProofType.ATTESTATION);

        Assertions.assertNotNull(result);
        assertEquals(List.of("tpm"), result.getKeyStorage(),
                "Should read key_storage from the attestation proof type entry");
        assertEquals(List.of("fingerprint"), result.getUserAuthentication(),
                "Should read user_authentication from the attestation proof type entry");
    }

    @Test
    public void testGetAttestationRequirements_ReturnsNullWhenProofTypeKeyNotFound() {
        VCIssuanceContext ctx = createContextWithAttestation(
                new KeyAttestationsRequired().setKeyStorage(List.of("hardware")),
                null
        );

        KeyAttestationsRequired result = AttestationValidatorUtil.getAttestationRequirements(ctx, "unknown_proof_type");

        assertNull(result, "Should return null when proof type key does not exist");
    }

    @Test
    public void testGetAttestationRequirements_ReturnsNullWhenConfigIsNull() {
        VCIssuanceContext ctx = new VCIssuanceContext();

        KeyAttestationsRequired result = AttestationValidatorUtil.getAttestationRequirements(ctx, ProofType.JWT);

        assertNull(result, "Should return null when credentialConfig is null");
    }

    @Test
    public void testGetAttestationRequirements_ReturnsNullWhenProofTypesSupportedIsNull() {
        VCIssuanceContext ctx = new VCIssuanceContext();
        ctx.setCredentialConfig(new SupportedCredentialConfiguration());

        KeyAttestationsRequired result = AttestationValidatorUtil.getAttestationRequirements(ctx, ProofType.JWT);

        assertNull(result, "Should return null when proofTypesSupported is null");
    }

    @Test
    public void testGetAttestationRequirements_ReturnsNullWhenSupportedProofTypesIsEmpty() {
        // supportedProofTypes is always an initialized HashMap (never null), so this covers the empty-map case.
        VCIssuanceContext ctx = new VCIssuanceContext();
        SupportedCredentialConfiguration config = new SupportedCredentialConfiguration();
        config.setProofTypesSupported(new ProofTypesSupported());
        ctx.setCredentialConfig(config);

        KeyAttestationsRequired result = AttestationValidatorUtil.getAttestationRequirements(ctx, ProofType.JWT);

        assertNull(result, "Should return null when supportedProofTypes map is empty");
    }

    @Test
    public void testGetAttestationRequirements_ReturnsJwtFallbackWhenProofTypeKeyIsNull() {
        // null proofTypeKey falls back to "jwt" for backward compatibility.
        KeyAttestationsRequired jwtReq = new KeyAttestationsRequired()
                .setKeyStorage(List.of("hardware"))
                .setUserAuthentication(List.of("pin"));

        VCIssuanceContext ctx = createContextWithAttestation(jwtReq, null);

        KeyAttestationsRequired result = AttestationValidatorUtil.getAttestationRequirements(ctx, null);

        Assertions.assertNotNull(result, "Should return attestation requirements using JWT fallback when proofTypeKey is null");
        assertEquals(List.of("hardware"), result.getKeyStorage());
        assertEquals(List.of("pin"), result.getUserAuthentication());
    }

    @Test
    public void testGetAttestationRequirements_JwtAndAttestationProduceDifferentResults() {
        KeyAttestationsRequired jwtReq = new KeyAttestationsRequired()
                .setKeyStorage(List.of("hardware"));
        KeyAttestationsRequired attestationReq = new KeyAttestationsRequired()
                .setKeyStorage(List.of("tpm", "secure_enclave"));

        VCIssuanceContext ctx = createContextWithAttestation(jwtReq, attestationReq);

        KeyAttestationsRequired jwtResult = AttestationValidatorUtil.getAttestationRequirements(ctx, ProofType.JWT);
        KeyAttestationsRequired attestationResult = AttestationValidatorUtil.getAttestationRequirements(ctx, ProofType.ATTESTATION);

        Assertions.assertNotNull(jwtResult);
        assertEquals(List.of("hardware"), jwtResult.getKeyStorage());
        Assertions.assertNotNull(attestationResult);
        assertEquals(List.of("tpm", "secure_enclave"), attestationResult.getKeyStorage());
    }

    @Test
    public void testGetAttestationRequirements_ReturnsNullWhenProofTypeEntryHasNoAttestationRequired() {
        VCIssuanceContext ctx = new VCIssuanceContext();
        SupportedCredentialConfiguration config = new SupportedCredentialConfiguration();
        ProofTypesSupported proofTypesSupported = new ProofTypesSupported();
        proofTypesSupported.getSupportedProofTypes().put(ProofType.JWT,
                new SupportedProofTypeData(List.of("ES256"), null));
        config.setProofTypesSupported(proofTypesSupported);
        ctx.setCredentialConfig(config);

        KeyAttestationsRequired result = AttestationValidatorUtil.getAttestationRequirements(ctx, ProofType.JWT);

        assertNull(result, "Should return null when the proof type entry has null key_attestations_required");
    }

    private static VCIssuanceContext createContextWithAttestation(
            KeyAttestationsRequired jwtAttestation,
            KeyAttestationsRequired attestationAttestation) {
        VCIssuanceContext ctx = new VCIssuanceContext();
        SupportedCredentialConfiguration config = new SupportedCredentialConfiguration();
        ProofTypesSupported proofTypesSupported = new ProofTypesSupported();
        Map<String, SupportedProofTypeData> proofTypes = proofTypesSupported.getSupportedProofTypes();

        if (jwtAttestation != null) {
            proofTypes.put(ProofType.JWT,
                    new SupportedProofTypeData(List.of("ES256"), jwtAttestation));
        }
        if (attestationAttestation != null) {
            proofTypes.put(ProofType.ATTESTATION,
                    new SupportedProofTypeData(List.of("ES256"), attestationAttestation));
        }

        config.setProofTypesSupported(proofTypesSupported);
        ctx.setCredentialConfig(config);
        return ctx;
    }
}

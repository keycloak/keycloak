package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.keycloak.VCFormat;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_EXP;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_IAT;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_ISSUER;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_JTI;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUB;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;

public class SdJwtCredentialBuilder implements CredentialBuilder {

    public SdJwtCredentialBuilder() {
    }

    @Override
    public String getSupportedFormat() {
        return VCFormat.SD_JWT_VC;
    }

    @Override
    public SdJwtCredentialBody buildCredentialBody(
            VerifiableCredential verifiableCredential,
            CredentialBuildConfig credentialBuildConfig
    ) throws CredentialBuilderException {

        URI vcId = verifiableCredential.getId();
        Instant issuanceDate = verifiableCredential.getIssuanceDate();
        Instant expirationDate = verifiableCredential.getExpirationDate();

        // Retrieve subject claims
        CredentialSubject credentialSubject = verifiableCredential.getCredentialSubject();
        Map<String, Object> claims = new LinkedHashMap<>(credentialSubject.getClaims());

        // Map subject id => sub
        Optional.ofNullable(claims.remove(CLAIM_NAME_SUBJECT_ID)).ifPresent(it ->
                claims.put(CLAIM_NAME_SUB, it)
        );

        // Always add a jti (the credential id)
        claims.put(CLAIM_NAME_JTI, vcId != null ? vcId : UUID.randomUUID().toString());

        // Put all claims into the disclosure spec, except the one to be kept visible
        DisclosureSpec.Builder disclosureSpecBuilder = DisclosureSpec.builder();
        claims.entrySet()
                .stream()
                .filter(entry -> !credentialBuildConfig.getSdJwtVisibleClaims().contains(entry.getKey()))
                .forEach(entry -> {
                    // Determine array-ness from the serialized JSON value so that non-List
                    // Java values (e.g. HashSet from OID4VCTargetRoleMapper, Java arrays)
                    // are also disclosed per-element.
                    JsonNode valueNode = JsonSerialization.mapper.valueToTree(entry.getValue());
                    if (valueNode != null && valueNode.isArray()) {
                        // Disclose elements one by one, the claim name itself stays visible
                        int size = valueNode.size();
                        IntStream.range(0, size)
                                .forEach(i -> disclosureSpecBuilder
                                        .withUndisclosedArrayElt(entry.getKey(), i, SdJwtUtils.randomSalt()));
                    } else {
                        disclosureSpecBuilder.withUndisclosedClaim(entry.getKey(), SdJwtUtils.randomSalt());
                    }
                });

        // Populate configured fields (necessarily visible)
        claims.put(CLAIM_NAME_ISSUER, credentialBuildConfig.getCredentialIssuer());
        claims.put(CLAIM_NAME_VCT, credentialBuildConfig.getCredentialType());

        // iat is issuer-controlled: it must always reflect the issuer-computed issuance time
        // and must not be overridable by a mapped attribute value (see keycloak/keycloak#52667).
        if (issuanceDate != null) {
            claims.put(CLAIM_NAME_IAT, issuanceDate.getEpochSecond());
        }

        // Set exp claim from verifiable credential expiration date
        // expiry is optional, but should be set if available to comply with HAIP
        // see: https://openid.github.io/OpenID4VC-HAIP/openid4vc-high-assurance-interoperability-profile-wg-draft.html#section-6.1
        // exp is issuer-controlled: it must always reflect the issuer-configured credential lifetime
        // and must not be overridable by a mapped attribute value (see keycloak/keycloak#52667).
        if (expirationDate != null) {
            claims.put(CLAIM_NAME_EXP, expirationDate.getEpochSecond());
        }

        // jti, nbf, and iat are all optional. So need to be set by a protocol mapper if needed.
        // see: https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-03.html#name-registered-jwt-claims

        // Add the configured number of decoys
        if (credentialBuildConfig.getNumberOfDecoys() > 0) {
            IntStream.range(0, credentialBuildConfig.getNumberOfDecoys())
                    .forEach(i -> disclosureSpecBuilder.withDecoyClaim(SdJwtUtils.randomSalt()));
        }

        ObjectNode claimsNode = JsonSerialization.mapper.convertValue(claims, ObjectNode.class);
        IssuerSignedJWT issuerSignedJWT = IssuerSignedJWT.builder()
                                                         .withClaims(claimsNode,
                                                                     disclosureSpecBuilder.build())
                                                         .withHashAlg(credentialBuildConfig.getHashAlgorithm())
                                                         .withJwsType(credentialBuildConfig.getTokenJwsType())
                                                         .build();
        SdJwt.Builder sdJwtBuilder = SdJwt.builder();

        return new SdJwtCredentialBody(sdJwtBuilder, issuerSignedJWT);
    }

    @Override
    public void contributeToMetadata(SupportedCredentialConfiguration credentialConfig, CredentialScopeModel credentialScope) {
        String vct = Optional.ofNullable(credentialScope.getVct()).orElse(credentialScope.getName());
        credentialConfig.setVct(vct);
    }
}

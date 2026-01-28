/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.util.JsonSerialization;

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
        return Format.SD_JWT_VC;
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

        Optional.ofNullable(issuanceDate).ifPresent(it ->
                claims.put(CLAIM_NAME_IAT, it.getEpochSecond())
        );

        // Put all claims into the disclosure spec, except the one to be kept visible
        DisclosureSpec.Builder disclosureSpecBuilder = DisclosureSpec.builder();
        claims.entrySet()
                .stream()
                .filter(entry -> !credentialBuildConfig.getSdJwtVisibleClaims().contains(entry.getKey()))
                .forEach(entry -> {
                    if (entry instanceof List<?> listValue) {
                        // FIXME: Unreachable branch. The intent was probably to check `entry.getValue()`,
                        //  but changing just that will expose the array field name and break many tests.
                        //  Needs further discussion on the wanted behavior.

                        IntStream.range(0, listValue.size())
                                .forEach(i -> disclosureSpecBuilder
                                        .withUndisclosedArrayElt(entry.getKey(), i, SdJwtUtils.randomSalt())
                                );
                    } else {
                        disclosureSpecBuilder.withUndisclosedClaim(entry.getKey(), SdJwtUtils.randomSalt());
                    }
                });

        // Populate configured fields (necessarily visible)
        claims.put(CLAIM_NAME_ISSUER, credentialBuildConfig.getCredentialIssuer());
        claims.put(CLAIM_NAME_VCT, credentialBuildConfig.getCredentialType());

        // Set exp claim from verifiable credential expiration date
        // expiry is optional, but should be set if available to comply with HAIP
        // see: https://openid.github.io/OpenID4VC-HAIP/openid4vc-high-assurance-interoperability-profile-wg-draft.html#section-6.1
        // Only set if not already set by a protocol mapper
        if (!claims.containsKey(CLAIM_NAME_EXP) && expirationDate != null) {
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
}

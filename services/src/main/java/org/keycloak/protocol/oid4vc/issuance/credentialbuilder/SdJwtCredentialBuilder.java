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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        Instant issuanceDate = verifiableCredential.getIssuanceDate();
        Integer expirySeconds = credentialBuildConfig.getExpiryInSeconds();

        // Retrieve subject claims
        CredentialSubject credentialSubject = verifiableCredential.getCredentialSubject();
        Map<String, Object> claims = new LinkedHashMap<>(credentialSubject.getClaims());

        // Add inner (disclosed) claims iat, sub - the latter being derived from Subject.id
        Optional.ofNullable(issuanceDate).ifPresent(it -> {
            claims.put(CLAIM_NAME_IAT, it.getEpochSecond());
        });
        Optional.ofNullable(claims.get(CLAIM_NAME_SUBJECT_ID)).ifPresent(it -> {
            claims.put(CLAIM_NAME_SUB, claims.remove(CLAIM_NAME_SUBJECT_ID));
        });

        // Put inner claims into the disclosure spec, except the one to be kept visible
        DisclosureSpec.Builder disclosureSpecBuilder = DisclosureSpec.builder();
        List<String> outerClaims = credentialBuildConfig.getSdJwtVisibleClaims();
        claims.keySet().stream()
                .filter(it -> !outerClaims.contains(it))
                .forEach(it -> {
                    disclosureSpecBuilder.withUndisclosedClaim(it, SdJwtUtils.randomSalt());
                });

        // Add outer (always visible) claims: iss, vct, exp
        // https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-11.html#section-3.2.2.2
        claims.put(CLAIM_NAME_ISSUER, credentialBuildConfig.getCredentialIssuer());
        claims.put(CLAIM_NAME_VCT, credentialBuildConfig.getCredentialType());

        // Set exp claim from verifiable credential expiration date
        // expiry is optional, but should be set if available to comply with HAIP
        // see: https://openid.github.io/OpenID4VC-HAIP/openid4vc-high-assurance-interoperability-profile-wg-draft.html#section-6.1
        // Only set if not already set by a protocol mapper
        if (!claims.containsKey(CLAIM_NAME_EXP)) {
            Optional.ofNullable(verifiableCredential.getExpirationDate())
                    .ifPresent(d -> claims.put(CLAIM_NAME_EXP, d.getEpochSecond()));
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

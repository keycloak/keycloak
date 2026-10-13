/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.broker.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.JwkParsingUtils;
import org.keycloak.sdjwt.consumer.StaticTrustedSdJwtIssuer;
import org.keycloak.sdjwt.consumer.TrustedSdJwtIssuer;
import org.keycloak.sdjwt.vp.TrustedSdJwtIssuerResolver;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Bridges a {@link TrustMaterialIdentityProvider} to a {@link TrustedSdJwtIssuerResolver}. When the
 * provider exposes X.509 trust anchors, chain validation is mandated as required by HAIP 6.1.1: the
 * x5c header of the credential's issuer signed JWT must validate against the anchors and the chain
 * leaf key verifies the signature, a credential without an x5c header is rejected. Only providers
 * without X.509 trust anchors fall back to looking up the trusted keys by the issuer key hints
 * (kid, alg, iss).
 *
 * TODO: Support looking up the JWKS dynamically from the .well-known/jwt-vc-issuer endpoint of the
 * issuer in the credential.
 */
public class TrustMaterialSdJwtIssuerResolver implements TrustedSdJwtIssuerResolver {

    private final TrustMaterialIdentityProvider<?> trustMaterial;

    public TrustMaterialSdJwtIssuerResolver(TrustMaterialIdentityProvider<?> trustMaterial) {
        this.trustMaterial = trustMaterial;
    }

    @Override
    public TrustedSdJwtIssuer resolve(IssuerSignedJWT issuerSignedJwt) throws VerificationException {
        JWSHeader header = issuerSignedJwt.getJwsHeader();
        JsonNode issuer = issuerSignedJwt.getPayload().get(OID4VCConstants.CLAIM_NAME_ISSUER);
        TrustMaterialRequest request = TrustMaterialRequest.builder()
                .kid(header.getKeyId())
                .algorithm(header.getRawAlgorithm())
                .issuer(issuer != null && issuer.isTextual() ? issuer.textValue() : null)
                .build();

        JWK leafKey = trustMaterial.validateX509Chain(request, header.getX5c(), header.getRawAlgorithm());
        if (leafKey != null) {
            return new StaticTrustedSdJwtIssuer(List.of(JwkParsingUtils.convertJwkToVerifierContext(leafKey)));
        }

        List<SignatureVerifierContext> verifiers = trustMaterial.resolveKeys(request)
                .map(JwkParsingUtils::convertJwkToVerifierContext)
                .collect(Collectors.toList());
        if (verifiers.isEmpty()) {
            throw new VerificationException("No trusted issuer key found for the presented SD-JWT credential");
        }
        return new StaticTrustedSdJwtIssuer(verifiers);
    }
}

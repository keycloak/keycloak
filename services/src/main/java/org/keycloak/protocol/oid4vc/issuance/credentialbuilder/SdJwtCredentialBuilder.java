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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.IntStream;

import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;

public class SdJwtCredentialBuilder implements CredentialBuilder {

    public static final String JWT_ISSUER_CLAIM = "iss";
    public static final String JWT_ISSUANCE_DATE_CLAIM = "iat";
    public static final String JWT_EXPIRE_DATE_CLAIM = "exp";
    public static final String JWT_SUBJECT_CLAIM = "sub";
    public static final String JWT_CREDENTIAL_TYPE_CLAIM = "vct";
    public static final String SUBJECT_ID_CLAIM = "id";

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

        var maybeIssuanceDate = verifiableCredential.getIssuanceDate();
        var maybeExpirySeconds = credentialBuildConfig.getExpiryInSeconds();

        // Retrieve subject claims
        var credentialSubject = verifiableCredential.getCredentialSubject();
        var claims = new LinkedHashMap<>(credentialSubject.getClaims());

        // Add inner (disclosed) claims iat, sub - the latter being derived from Subject.id
        Optional.ofNullable(maybeIssuanceDate).ifPresent(it -> {
            claims.put(JWT_ISSUANCE_DATE_CLAIM, it.getEpochSecond());
        });
        Optional.ofNullable(claims.get(SUBJECT_ID_CLAIM)).ifPresent(it -> {
            claims.put(JWT_SUBJECT_CLAIM, claims.remove(SUBJECT_ID_CLAIM));
        });

        // Put inner claims into the disclosure spec, except the one to be kept visible
        var disclosureSpecBuilder = DisclosureSpec.builder();
        var outerClaims = credentialBuildConfig.getSdJwtVisibleClaims();
        claims.keySet().stream()
                .filter(it -> !outerClaims.contains(it))
                .forEach(it -> {
                    disclosureSpecBuilder.withUndisclosedClaim(it, SdJwtUtils.randomSalt());
                });

        // Add outer (always visible) claims: iss, vct, exp
        // https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-11.html#section-3.2.2.2

        claims.put(JWT_ISSUER_CLAIM, credentialBuildConfig.getCredentialIssuer());
        claims.put(JWT_CREDENTIAL_TYPE_CLAIM, credentialBuildConfig.getCredentialType());
        if (maybeIssuanceDate != null && maybeExpirySeconds != null) {
            var exp = maybeIssuanceDate.plus(Duration.ofSeconds(maybeExpirySeconds));
            claims.put(JWT_EXPIRE_DATE_CLAIM, exp.getEpochSecond());
        }

        // Add the configured number of decoys
        if (credentialBuildConfig.getNumberOfDecoys() > 0) {
            IntStream.range(0, credentialBuildConfig.getNumberOfDecoys())
                    .forEach(i -> disclosureSpecBuilder.withDecoyClaim(SdJwtUtils.randomSalt()));
        }

        var disclosureSpec = disclosureSpecBuilder.build();
        var sdJwtBuilder = SdJwt.builder()
                .withDisclosureSpec(disclosureSpec)
                .withHashAlgorithm(credentialBuildConfig.getHashAlgorithm())
                .withJwsType(credentialBuildConfig.getTokenJwsType());

        return new SdJwtCredentialBody(sdJwtBuilder, claims);
    }
}

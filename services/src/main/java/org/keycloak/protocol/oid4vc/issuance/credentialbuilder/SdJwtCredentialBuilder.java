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

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;

public class SdJwtCredentialBuilder implements CredentialBuilder {

    public static final String ISSUER_CLAIM = "iss";
    public static final String VERIFIABLE_CREDENTIAL_TYPE_CLAIM = "vct";

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
        // Retrieve claims
        CredentialSubject credentialSubject = verifiableCredential.getCredentialSubject();
        Map<String, Object> claimSet = credentialSubject.getClaims();

        // Put all claims into the disclosure spec, except the one to be kept visible
        DisclosureSpec.Builder disclosureSpecBuilder = DisclosureSpec.builder();
        claimSet.entrySet()
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
        claimSet.put(ISSUER_CLAIM, credentialBuildConfig.getCredentialIssuer());
        claimSet.put(VERIFIABLE_CREDENTIAL_TYPE_CLAIM, credentialBuildConfig.getCredentialType());

        // jti, nbf, iat and exp are all optional. So need to be set by a protocol mapper if needed.
        // see: https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-03.html#name-registered-jwt-claims

        // Add the configured number of decoys
        if (credentialBuildConfig.getNumberOfDecoys() > 0) {
            IntStream.range(0, credentialBuildConfig.getNumberOfDecoys())
                    .forEach(i -> disclosureSpecBuilder.withDecoyClaim(SdJwtUtils.randomSalt()));
        }

        var sdJwtBuilder = SdJwt.builder()
                .withDisclosureSpec(disclosureSpecBuilder.build())
                .withHashAlgorithm(credentialBuildConfig.getHashAlgorithm())
                .withJwsType(credentialBuildConfig.getTokenJwsType());

        return new SdJwtCredentialBody(sdJwtBuilder, claimSet);
    }
}

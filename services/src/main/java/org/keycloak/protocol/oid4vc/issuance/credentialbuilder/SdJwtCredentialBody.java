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

import java.util.Map;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_CNF;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_JWK;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class SdJwtCredentialBody implements CredentialBody {

    private final SdJwt.Builder sdJwtBuilder;
    private final Map<String, Object> claimSet;

    public SdJwtCredentialBody(SdJwt.Builder sdJwtBuilder, Map<String, Object> claimSet) {
        this.sdJwtBuilder = sdJwtBuilder;
        this.claimSet = claimSet;
    }

    public void addKeyBinding(JWK jwk) throws CredentialBuilderException {
        claimSet.put(CLAIM_NAME_CNF, Map.of(CLAIM_NAME_JWK, jwk));
    }

    public Map<String, Object> getClaimSet() {
        return claimSet;
    }

    public String sign(SignatureSignerContext signatureSignerContext) {
        JsonNode claimSet = JsonSerialization.mapper.valueToTree(this.claimSet);
        SdJwt sdJwt = sdJwtBuilder
                .withClaimSet(claimSet)
                .withSigner(signatureSignerContext)
                .build();

        return sdJwt.toSdJwtString();
    }
}

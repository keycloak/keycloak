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

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_CNF;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_JWK;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class SdJwtCredentialBody implements CredentialBody {

    private final SdJwt.Builder sdJwtBuilder;
    private final IssuerSignedJWT issuerSignedJWT;

    public SdJwtCredentialBody(SdJwt.Builder sdJwtBuilder, IssuerSignedJWT issuerSignedJWT) {
        this.sdJwtBuilder = sdJwtBuilder;
        this.issuerSignedJWT = issuerSignedJWT;
    }

    public void addKeyBinding(JWK jwk) throws CredentialBuilderException {
        ObjectNode jwkNode = JsonSerialization.mapper.convertValue(jwk, ObjectNode.class);
        ObjectNode keyBindingNode = JsonSerialization.mapper.createObjectNode();
        keyBindingNode.set(CLAIM_NAME_JWK, jwkNode);
        issuerSignedJWT.getPayload().set(CLAIM_NAME_CNF, keyBindingNode);
    }

    public IssuerSignedJWT getIssuerSignedJWT() {
        return issuerSignedJWT;
    }

    public String sign(SignatureSignerContext signatureSignerContext) {
        SdJwt sdJwt = sdJwtBuilder.withIssuerSignedJwt(issuerSignedJWT)
                .withIssuerSigningContext(signatureSignerContext)
                .build();

        return sdJwt.toSdJwtString();
    }
}

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

import static org.keycloak.OID4VCConstants.CLAIM_NAME_CNF;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_JWK;

import java.util.Map;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class JwtCredentialBody implements CredentialBody {

    private final JWSBuilder jwsBuilder;
    private final JsonWebToken jsonWebToken;

    public JwtCredentialBody(JWSBuilder jwsBuilder, JsonWebToken jsonWebToken) {
        this.jwsBuilder = jwsBuilder;
        this.jsonWebToken = jsonWebToken;
    }

    @Override
    public void addKeyBinding(JWK jwk) throws CredentialBuilderException {
        Map<String, Object> jwkMap = JsonSerialization
                .mapper
                .convertValue(jwk, JsonSerialization.mapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, Object.class));
        jsonWebToken.setOtherClaims(CLAIM_NAME_CNF, Map.of(CLAIM_NAME_JWK, jwkMap));
    }

    public String sign(SignatureSignerContext signatureSignerContext) {
        return jwsBuilder.jsonContent(jsonWebToken).sign(signatureSignerContext);
    }
}

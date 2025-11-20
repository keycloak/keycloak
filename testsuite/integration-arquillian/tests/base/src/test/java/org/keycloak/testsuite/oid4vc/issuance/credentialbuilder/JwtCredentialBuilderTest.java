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

package org.keycloak.testsuite.oid4vc.issuance.credentialbuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CREDENTIAL_SUBJECT;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class JwtCredentialBuilderTest extends CredentialBuilderTest {

    TimeProvider timeProvider = new StaticTimeProvider(1000);
    JwtCredentialBuilder builder = new JwtCredentialBuilder(timeProvider);

    @Test
    public void shouldBuildJwtCredentialSuccessfully() throws Exception {
        VerifiableCredential verifiableCredential = getTestCredential(exampleCredentialClaims());
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig().setTokenJwsType("JWT");

        // Build
        JwtCredentialBody jwtCredentialBody = builder
                .buildCredentialBody(verifiableCredential, credentialBuildConfig);

        // Sign and parse JWS string
        String jws = jwtCredentialBody.sign(exampleSigner());
        JWSInput jwsInput = new JWSInput(jws);
        JsonNode credentialSubject = parseCredentialSubject(jwsInput);

        // Assert
        assertEquals("JWT", jwsInput.getHeader().getType());
        assertEquals(10, credentialSubject.get("issuanceDate").asInt());
        assertEquals("randomValue", credentialSubject.get("randomKey").asText());
        assertEquals("[\"a\",\"b\",\"c\"]", credentialSubject.get("arrayClaim").toString());
    }

    @Test
    public void buildJwtCredential_SetNbfAsCurrentTimeIfIssuanceDateNotSupplied() throws Exception {
        VerifiableCredential verifiableCredential = getTestCredential(exampleCredentialClaimsWithoutIssuanceDate());
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig().setTokenJwsType("JWT");

        // Build
        JwtCredentialBody jwtCredentialBody = builder
                .buildCredentialBody(verifiableCredential, credentialBuildConfig);

        // Sign and parse JWS string
        String jws = jwtCredentialBody.sign(exampleSigner());
        JWSInput jwsInput = new JWSInput(jws);
        JsonNode payload = jwsInput.readJsonContent(JsonNode.class);

        // Assert that nbf is set and comes from the static time provider
        assertEquals(timeProvider.currentTimeSeconds(), payload.get("nbf").asLong());
    }

    private JsonNode parseCredentialSubject(JWSInput jwsInput) throws JWSInputException {
        JsonNode payload = jwsInput.readJsonContent(JsonNode.class);
        return payload.get("vc").get(CREDENTIAL_SUBJECT);
    }

    private Map<String, Object> exampleCredentialClaims() {
        return new HashMap<>(Map.of(
                "id", String.format("uri:uuid:%s", UUID.randomUUID()),
                "randomKey", "randomValue",
                "arrayClaim", List.of("a", "b", "c"),
                "issuanceDate", Instant.ofEpochSecond(10)
        ));
    }

    private Map<String, Object> exampleCredentialClaimsWithoutIssuanceDate() {
        return new HashMap<>(Map.of(
                "id", String.format("uri:uuid:%s", UUID.randomUUID()),
                "randomKey", "randomValue",
                "arrayClaim", List.of("a", "b", "c")
        ));
    }
}

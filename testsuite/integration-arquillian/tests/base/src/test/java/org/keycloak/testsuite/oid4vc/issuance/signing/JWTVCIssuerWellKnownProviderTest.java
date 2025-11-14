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
package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.protocol.oid4vc.issuance.JWTVCIssuerWellKnownProviderFactory;
import org.keycloak.protocol.oid4vc.model.JWTVCIssuerMetadata;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */public class JWTVCIssuerWellKnownProviderTest extends OID4VCTest {

    @Test
    public void getConfig() throws IOException {
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;

        try (Client client = AdminClientUtil.createResteasyClient()) {
            UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
            URI jwtIssuerUri = RealmsResource.wellKnownProviderUrl(builder).build("test", JWTVCIssuerWellKnownProviderFactory.PROVIDER_ID);
            WebTarget jwtIssuerTarget = client.target(jwtIssuerUri);

            try (Response jwtIssuerResponse = jwtIssuerTarget.request().get()) {
                JWTVCIssuerMetadata jwtvcIssuerMetadata = JsonSerialization.readValue(jwtIssuerResponse.readEntity(String.class), JWTVCIssuerMetadata.class);
                assertEquals("The correct issuer should be included.", expectedIssuer, jwtvcIssuerMetadata.getIssuer());
                JSONWebKeySet jwks = jwtvcIssuerMetadata.getJwks();
                assertNotNull("The key set shall not be null", jwks.getKeys());
                assertTrue("The key set shall not be empty", jwks.getKeys().length > 0);
            }
        }
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() != null) {
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getRsaKeyProvider(RSA_KEY));
        } else {
            testRealm.setComponents(new MultivaluedHashMap<>(
                    Map.of("org.keycloak.keys.KeyProvider", List.of(getRsaKeyProvider(RSA_KEY))
                    )));
        }
    }
}

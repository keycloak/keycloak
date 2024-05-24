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

import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.protocol.oid4vc.issuance.JWTVCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.JWTVCIssuerMetadata;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */public class JWTVCIssuerWellKnownProviderTest extends OID4VCTest {

     @Test
    public void getConfig() {
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    JWTVCIssuerWellKnownProvider jwtvcIssuerWellKnownProvider = new JWTVCIssuerWellKnownProvider(session);
                    Object issuerConfig = jwtvcIssuerWellKnownProvider.getConfig();
                    assertTrue("Valid jwt-vc-issuer metadata should be returned.", issuerConfig instanceof JWTVCIssuerMetadata);
                    JWTVCIssuerMetadata jwtvcIssuerMetadata = (JWTVCIssuerMetadata) issuerConfig;
                    assertEquals("The correct issuer should be included.", expectedIssuer, jwtvcIssuerMetadata.getIssuer());
                    JSONWebKeySet jwks = jwtvcIssuerMetadata.getJwks();
                    assertNotNull("The key set shall not be null", jwks.getKeys());
                    assertTrue("The key set shall not be empty", jwks.getKeys().length>0);
                }));
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() != null) {
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getRsaKeyProvider(RSA_KEY));
            testRealm.getComponents().add("org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService", getJwtSigningProvider(RSA_KEY));
        } else {
            testRealm.setComponents(new MultivaluedHashMap<>(
                    Map.of("org.keycloak.keys.KeyProvider", List.of(getRsaKeyProvider(RSA_KEY)),
                            "org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService", List.of(getJwtSigningProvider(RSA_KEY))
                    )));
        }
    }
}

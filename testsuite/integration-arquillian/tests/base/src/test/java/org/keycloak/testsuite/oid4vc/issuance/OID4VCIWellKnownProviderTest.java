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
package org.keycloak.testsuite.oid4vc.issuance;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCTest;

import org.junit.Test;

import static org.keycloak.common.crypto.CryptoConstants.A128KW;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP_256;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OID4VCIWellKnownProviderTest extends OID4VCTest {

    @Test
    public void assertOnlyAsymmetricIncluded() throws IOException {

        getTestingClient()
                .server(TEST_REALM_NAME)
                .run(session -> {
                    OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
                    CredentialIssuer credentialIssuer = oid4VCIssuerWellKnownProvider.getIssuerMetadata();
                    assertEquals("Only one asymmetric encryption key is present in the realm.",
                            1,
                            credentialIssuer.getCredentialResponseEncryption()
                                    .getAlgValuesSupported()
                                    .size());
                    assertTrue("The algorithm of the configured asymmetric encryption key should be provided.",
                            credentialIssuer.getCredentialResponseEncryption().getAlgValuesSupported().contains(RSA_OAEP_256));
                });
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() != null) {
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider",
                    getRsaEncKeyProvider(RSA_OAEP_256, "enc-key-oaep256", 100));
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getRsaKeyProvider(RSA_KEY));
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getAesKeyProvider(A128KW, "aes-enc", "ENC", "aes-generated"));
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getAesKeyProvider(Algorithm.HS256, "aes-sig", "SIG", "hmac-generated"));
        } else {
            testRealm.setComponents(new MultivaluedHashMap<>(
                    Map.of("org.keycloak.keys.KeyProvider",
                            List.of(
                                    getRsaEncKeyProvider(RSA_OAEP_256, "enc-key-oaep256", 100),
                                    getRsaKeyProvider(RSA_KEY),
                                    getAesKeyProvider(A128KW, "aes-enc", "ENC", "aes-generated"),
                                    getAesKeyProvider(Algorithm.HS256, "aes-sig", "SIG", "hmac-generated"))
                    )));
        }
    }

    public static ComponentExportRepresentation getAesKeyProvider(String algorithm, String keyName, String keyUse, String providerId) {
        // Generate a random AES key (default length: 256 bits)
        byte[] secret = SecretGenerator.getInstance().randomBytes(32); // 32 bytes = 256 bits

        String secretBase64 = Base64.getEncoder().encodeToString(secret);

        ComponentExportRepresentation component = new ComponentExportRepresentation();
        component.setName(keyName);
        component.setId(UUID.randomUUID().toString());
        component.setProviderId(providerId);

        component.setConfig(new MultivaluedHashMap<>(
                Map.of(
                        "secret", List.of(secretBase64),
                        "active", List.of("true"),
                        "priority", List.of(String.valueOf(100)),
                        "enabled", List.of("true"),
                        "algorithm", List.of(algorithm),
                        "keyUse", List.of(keyUse) // encryption usage
                )
        ));
        return component;
    }
}

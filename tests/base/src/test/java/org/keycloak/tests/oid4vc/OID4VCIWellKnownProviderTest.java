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
package org.keycloak.tests.oid4vc;

import java.util.List;

import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryptionMetadata;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.keycloak.common.crypto.CryptoConstants.A128KW;
import static org.keycloak.common.crypto.CryptoConstants.RSA_OAEP;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP_256;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCIWellKnownProviderTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    boolean configuredAlready;

    @TestSetup
    public void configureTestRealm() {

        // When named differently this method is called before OID4VCIssuerTestBase.configureTestRealm()
        // When named the same name, it is called twice (i.e. does not override)
        //
        // [TODO] IntegrationTest sub class @TestSetup called before super class
        // https://github.com/keycloak/keycloak/issues/46667

        if (!configuredAlready) {
            super.configureTestRealm();

            ComponentsResource components = testRealm.admin().components();
            components.add(getRsaKeyProvider(getRsaKey_Default())).close();
            components.add(getRsaEncKeyProvider(RSA_OAEP_256, "enc-key-oaep256", 100)).close();
            components.add(getAesKeyProvider(A128KW, "aes-enc", "ENC", "aes-generated")).close();
            components.add(getAesKeyProvider(Algorithm.HS256, "aes-sig", "SIG", "hmac-generated")).close();

            configuredAlready = true;
        }
    }

    @Test
    public void assertOnlyAsymmetricIncluded() {

        // Server-side debugging: KC_TEST_SERVER = embedded
        //
        runOnServer.run(session -> {
            CredentialIssuer credentialIssuer = new OID4VCIssuerWellKnownProvider(session).getIssuerMetadata();
            CredentialResponseEncryptionMetadata credentialResponseEncryption = credentialIssuer.getCredentialResponseEncryption();
            List<String> algValuesSupported = credentialResponseEncryption.getAlgValuesSupported();
            assertEquals(2, algValuesSupported.size(), "Two asymmetric encryption are present.");
            assertTrue(algValuesSupported.contains(RSA_OAEP), "The default algorithm for asymmetric encryption should be available as well.");
            assertTrue(algValuesSupported.contains(RSA_OAEP_256), "The algorithm of the configured asymmetric encryption key should be provided.");
        });
    }
}

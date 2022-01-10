/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.vault;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.vault.VaultStringSecret;
import org.keycloak.vault.VaultTranscriber;

import java.util.List;
import java.util.Optional;

import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * Tests the usage of the {@link VaultTranscriber} on the server side. The tests attempt to obtain the transcriber from
 * the session and then use it to obtain secrets from the configured provider.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@EnableVault
@AuthServerContainerExclude(REMOTE)
public class KeycloakVaultTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/testrealm.json"));
    }


    @Test
    public void testKeycloakVault() throws Exception {
        // run the test in two different realms to test the provider's ability to retrieve secrets with the same key in different realms.
        testingClient.server().run(new KeycloakVaultServerTest("${vault.smtp_key}", "secure_master_smtp_secret"));
        testingClient.server("test").run(new KeycloakVaultServerTest("${vault.smtp_key}", "secure_test_smtp_secret"));
    }

    static class KeycloakVaultServerTest implements RunOnServer {

        private String testKey;
        private String expectedSecret;

        public KeycloakVaultServerTest(final String key, final String expectedSecret) {
            this.testKey = key;
            this.expectedSecret = expectedSecret;
        }

        @Override
        public void run(KeycloakSession session) {
            VaultTranscriber transcriber = session.vault();
            Assert.assertNotNull(transcriber);

            // use the transcriber to obtain a secret from the vault.
            try (VaultStringSecret secret = transcriber.getStringSecret(testKey)){
                Optional<String> optional = secret.get();
                Assert.assertTrue(optional.isPresent());
                String secretString = optional.get();
                Assert.assertEquals(expectedSecret, secretString);
            }

            // try obtaining a secret using a key that does not exist in the vault.
            String invalidEntry = "${vault.invalid_entry}";
            try (VaultStringSecret secret = transcriber.getStringSecret(invalidEntry)) {
                Optional<String> optional = secret.get();
                Assert.assertFalse(optional.isPresent());
            }

            // invoke the transcriber using a string that is not a vault expression.
            try (VaultStringSecret secret = transcriber.getStringSecret("mysecret")) {
                Optional<String> optional = secret.get();
                Assert.assertTrue(optional.isPresent());
                String secretString = optional.get();
                Assert.assertEquals("mysecret", secretString);
            }
        }
    }
}
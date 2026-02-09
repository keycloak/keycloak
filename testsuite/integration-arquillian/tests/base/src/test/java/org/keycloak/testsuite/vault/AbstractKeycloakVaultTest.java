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

import java.util.List;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.vault.VaultStringSecret;
import org.keycloak.vault.VaultTranscriber;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

/**
 * Tests the usage of the {@link VaultTranscriber} on the server side. The tests attempt to obtain the transcriber from
 * the session and then use it to obtain secrets from the configured provider.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */

public abstract class AbstractKeycloakVaultTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/testrealm.json"));
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
            VaultTranscriber transcriber = getVaultTranscriber(session);
            // obtain an existing secret from the vault.
            Optional<String> optional = getSecret(transcriber, testKey);
            Assert.assertTrue(optional.isPresent());
            Assert.assertEquals(expectedSecret, optional.get());

            // try obtaining a secret using a key that does not exist in the vault.
            optional = getSecret(transcriber, "${vault.invalid_entry}");
            Assert.assertFalse(optional.isPresent());

            // invoke the transcriber using a string that is not a vault expression.
            optional = getSecret(transcriber, "mysecret");
            Assert.assertTrue(optional.isPresent());
            Assert.assertEquals("mysecret", optional.get());
        }

        private Optional<String> getSecret(VaultTranscriber transcriber, String testKey) {
            VaultStringSecret secret = transcriber.getStringSecret(testKey);
            return secret.get();
        }
    }

    @NotNull
    private static VaultTranscriber getVaultTranscriber(KeycloakSession session) throws RuntimeException {
        return session.vault();
    }
}
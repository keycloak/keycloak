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

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.vault.PlainTextVaultProvider;
import org.keycloak.vault.VaultProvider;
import org.keycloak.vault.VaultUtils;

/**
 * Tests the usage of the {@link VaultProvider} on the server side. The tests attempt to obtain the configured vault provider
 * from the session and then use the {@link VaultUtils} to obtain secrets from the configured provider.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class KeycloakVaultTest extends AbstractKeycloakTest {

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create();
    }

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
            // the default provider as defined in keycloak-server.json is the PlainTextVaultProvider.
            VaultProvider provider = session.getProvider(VaultProvider.class);
            Assert.assertNotNull(provider);
            Assert.assertEquals(PlainTextVaultProvider.class, provider.getClass());

            // use the vault utils to obtain a secret from the provider.
            Assert.assertTrue(VaultUtils.isVaultExpression(testKey));
            Optional<WeakReference<String>> optional = VaultUtils.getStringSecret(provider, testKey);
            Assert.assertTrue(optional.isPresent());
            WeakReference<String> stringSecret = optional.get();
            Assert.assertEquals(expectedSecret, stringSecret.get());

            // try obtaining a secret using a key that does not exist in the vault.
            String invaildEntry = "${vault.invalid_entry}";
            Assert.assertTrue(VaultUtils.isVaultExpression(invaildEntry));
            optional = VaultUtils.getStringSecret(provider, invaildEntry);
            Assert.assertFalse(optional.isPresent());
        }
    }
}

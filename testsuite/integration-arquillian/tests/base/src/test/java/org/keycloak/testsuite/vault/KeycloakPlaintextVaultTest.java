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

import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.keycloak.vault.VaultTranscriber;

import org.junit.Test;

/**
 * Tests the usage of the {@link VaultTranscriber} on the server side. The tests attempt to obtain the transcriber from
 * the session and then use it to obtain secrets from the configured provider.
 * <p/>
 * This test differs from the abstract class in that it uses the {@code files-plaintext} provider to obtain secrets.
 *
 * @author <a href="mailto:pzaoral@redhat.com">Peter Zaoral</a>
 */
@EnableVault(providerId = EnableVault.PROVIDER_ID.PLAINTEXT)
public class KeycloakPlaintextVaultTest extends AbstractKeycloakVaultTest {

    @Test
    public void testKeycloakPlaintextVault() {
        // run the test in two different realms to test the provider's ability to retrieve secrets with the same key in different realms.
        testingClient.server().run(new KeycloakVaultServerTest("${vault.smtp_key}", "secure_master_smtp_secret"));
        testingClient.server("test").run(new KeycloakVaultServerTest("${vault.smtp_key}", "secure_test_smtp_secret"));
    }

}

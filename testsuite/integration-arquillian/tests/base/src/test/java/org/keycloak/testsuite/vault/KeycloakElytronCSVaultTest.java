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

/**
 * Tests the usage of the {@link VaultTranscriber} on the server side. The tests attempt to obtain the transcriber from
 * the session and then use it to obtain secrets from the configured provider.
 * <p/>
 * This test differs from the superclass in that it uses the {@code elytron-cs-keystore} provider to obtain secrets.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@EnableVault(providerId = EnableVault.PROVIDER_ID.ELYTRON_CS_KEYSTORE)
public class KeycloakElytronCSVaultTest extends KeycloakVaultTest {
    // run the same tests of the superclass using the elytron credential store provider.
}

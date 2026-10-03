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

package org.keycloak.saml.processing.core.util;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;

/**
 * Default factory for creating {@link DefaultSamlDecryptionProvider} instances.
 *
 * <p>This factory retrieves decryption keys from session attributes. It supports
 * two modes:</p>
 * <ul>
 *   <li>{@code "saml.decryption.keys"} (List&lt;PrivateKey&gt;) — multiple candidate keys
 *       for key rotation scenarios</li>
 *   <li>{@code "saml.decryption.key"} (PrivateKey) — single key fallback for
 *       backward compatibility</li>
 * </ul>
 *
 * <p>The caller (adapter or broker) sets these attributes before invoking decryption.</p>
 */
public class DefaultSamlDecryptionProviderFactory implements SamlDecryptionProviderFactory {

    public static final String PROVIDER_ID = "default";

    @Override
    @SuppressWarnings("unchecked")
    public SamlDecryptionProvider create(KeycloakSession session) {
        // Try multi-key attribute first
        List<PrivateKey> keys = (List<PrivateKey>) session.getAttribute("saml.decryption.keys");
        if (keys != null && !keys.isEmpty()) {
            return new DefaultSamlDecryptionProvider(keys);
        }

        // Fall back to single-key attribute for backward compatibility
        PrivateKey singleKey = (PrivateKey) session.getAttribute("saml.decryption.key");
        if (singleKey != null) {
            return new DefaultSamlDecryptionProvider(Collections.singletonList(singleKey));
        }

        throw new RuntimeException(
            "No decryption keys available in session. Ensure SAML deployment " +
            "has valid encryption key(s) configured.");
    }

    @Override
    public void init(Config.Scope config) {
        // No factory-level configuration needed for default provider
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // No resources to release
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

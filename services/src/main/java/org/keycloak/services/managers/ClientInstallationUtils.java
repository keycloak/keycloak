/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.managers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;

public final class ClientInstallationUtils {

    private ClientInstallationUtils() {
    }

    public static Map<String, Object> getClientCredentialsAdapterConfig(KeycloakSession session, ClientModel client,
            boolean includeSecrets) {
        String clientAuthenticator = client.getClientAuthenticatorType();
        ClientAuthenticatorFactory authenticator = (ClientAuthenticatorFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(ClientAuthenticator.class, clientAuthenticator);
        Map<String, Object> adapterConfig = authenticator.getAdapterConfiguration(session, client);
        return includeSecrets ? adapterConfig : maskSecrets(adapterConfig);
    }

    private static <K> Map<K, Object> maskSecrets(Map<K, ?> adapterConfig) {
        Map<K, Object> maskedAdapterConfig = new LinkedHashMap<>();
        adapterConfig.forEach((key, value) -> {
            if (CredentialRepresentation.SECRET.equals(key)) {
                maskedAdapterConfig.put(key, ComponentRepresentation.SECRET_VALUE);
            } else if (value instanceof Map<?, ?> nestedConfig) {
                maskedAdapterConfig.put(key, maskSecrets(nestedConfig));
            } else {
                maskedAdapterConfig.put(key, value);
            }
        });
        return maskedAdapterConfig;
    }
}

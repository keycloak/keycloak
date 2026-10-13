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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ComponentRepresentation;

public final class ClientInstallationUtils {

    private ClientInstallationUtils() {
    }

    public static Map<String, Object> getClientCredentialsAdapterConfig(KeycloakSession session, ClientModel client,
            boolean includeSecrets) {
        String clientAuthenticator = client.getClientAuthenticatorType();
        ClientAuthenticatorFactory authenticator = (ClientAuthenticatorFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(ClientAuthenticator.class, clientAuthenticator);
        Map<String, Object> adapterConfig = authenticator.getAdapterConfiguration(session, client);
        if (includeSecrets) {
            return adapterConfig;
        }

        Set<String> secretKeys = Objects.requireNonNull(authenticator.getAdapterConfigurationSecretKeys(),
                "Adapter configuration secret paths must not be null for " + clientAuthenticator);
        Set<List<String>> secretPaths = secretKeys.stream().map(path -> {
            if (path == null || !path.matches("/[^/]+(?:/[^/]+)*")) {
                throw new IllegalArgumentException("Invalid adapter configuration secret path for " + clientAuthenticator + ": " + path);
            }
            return Arrays.asList(path.substring(1).split("/"));
        }).collect(Collectors.toSet());
        return maskSecrets(adapterConfig, secretPaths, clientAuthenticator);
    }

    private static <K> Map<K, Object> maskSecrets(Map<K, ?> adapterConfig, Set<List<String>> secretPaths, String providerId) {
        Map<K, Object> maskedAdapterConfig = new LinkedHashMap<>();
        adapterConfig.forEach((key, value) -> {
            Set<List<String>> matchingPaths = secretPaths.stream()
                    .filter(path -> path.get(0).equals(key))
                    .collect(Collectors.toSet());
            if (matchingPaths.stream().anyMatch(path -> path.size() == 1)) {
                maskedAdapterConfig.put(key, ComponentRepresentation.SECRET_VALUE);
            } else if (!matchingPaths.isEmpty()) {
                if (!(value instanceof Map<?, ?> nestedConfig)) {
                    throw new IllegalArgumentException("Adapter configuration secret paths for " + providerId
                            + " traverse a non-map entry: " + matchingPaths);
                }
                Set<List<String>> nestedPaths = matchingPaths.stream().map(path -> path.subList(1, path.size())).collect(Collectors.toSet());
                maskedAdapterConfig.put(key, maskSecrets(nestedConfig, nestedPaths, providerId));
            } else {
                maskedAdapterConfig.put(key, value);
            }
        });
        return maskedAdapterConfig;
    }
}

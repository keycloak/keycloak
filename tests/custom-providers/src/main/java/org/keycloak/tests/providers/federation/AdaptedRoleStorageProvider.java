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

package org.keycloak.tests.providers.federation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.role.RoleStorageProvider;
import org.keycloak.storage.role.RoleStorageProviderModel;

import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * Small read-only role store used to verify the current generic role lookup SPI from an external provider JAR.
 */
public final class AdaptedRoleStorageProvider implements RoleStorageProvider {

    public static final String CLIENT_ID = "spi-client-with-roles";
    public static final String REALM_ROLE_NAME = "external-realm-role";
    public static final String CLIENT_ROLE_NAME = "external-client-role";
    public static final String REALM_ROLE_EXTERNAL_ID = "realm-role";
    public static final String CLIENT_ROLE_EXTERNAL_ID = "client-role";

    private final KeycloakSession session;
    private final RoleStorageProviderModel component;

    AdaptedRoleStorageProvider(KeycloakSession session, RoleStorageProviderModel component) {
        this.session = session;
        this.component = component;
    }

    @Override
    public RoleModel getRole(RoleContainerModel container, String name) {
        if (container instanceof RealmModel realm) {
            return getRealmRole(realm, name);
        } else if (container instanceof ClientModel client) {
            return getClientRole(client, name);
        }
        return null;
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        return REALM_ROLE_NAME.equals(name) ? realmRole(realm) : null;
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        if (!component.getId().equals(storageId.getProviderId())) {
            return null;
        }
        return switch (storageId.getExternalId()) {
            case REALM_ROLE_EXTERNAL_ID -> realmRole(realm);
            case CLIENT_ROLE_EXTERNAL_ID -> clientRole(realm);
            default -> null;
        };
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        return filterAndPage(realmRole(realm), search, first, max, REALM_ROLE_NAME, "External realm role");
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        return isConfiguredClient(client) && CLIENT_ROLE_NAME.equals(name) ? clientRole(client) : null;
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        RoleModel role = isConfiguredClient(client) ? clientRole(client) : null;
        return filterAndPage(role, search, first, max, CLIENT_ROLE_NAME, "External client role");
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> ids, String search,
            Integer first, Integer max) {
        Set<String> includedIds = ids == null ? null : ids.collect(Collectors.toSet());
        ClientModel client = configuredClient(realm);
        RoleModel role = client == null ? null : clientRole(client);
        return paginatedStream(filterAndPage(role, search, null, null, CLIENT_ROLE_NAME, CLIENT_ID)
                .filter(candidate -> includedIds == null || includedIds.contains(candidate.getId())), first, max);
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, String search, Stream<String> excludedIds,
            Integer first, Integer max) {
        Set<String> excluded = excludedIds == null ? Set.of() : excludedIds.collect(Collectors.toSet());
        ClientModel client = configuredClient(realm);
        RoleModel role = client == null ? null : clientRole(client);
        return paginatedStream(filterAndPage(role, search, null, null, CLIENT_ROLE_NAME, CLIENT_ID)
                .filter(candidate -> !excluded.contains(candidate.getId())), first, max);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RoleContainerModel container, String search, Integer first, Integer max) {
        if (container instanceof RealmModel realm) {
            return searchForRolesStream(realm, search, first, max);
        } else if (container instanceof ClientModel client) {
            return searchForClientRolesStream(client, search, first, max);
        }
        return Stream.empty();
    }

    private ClientModel configuredClient(RealmModel realm) {
        return session.clients().getClientByClientId(realm, CLIENT_ID);
    }

    private boolean isConfiguredClient(ClientModel client) {
        return CLIENT_ID.equals(client.getClientId());
    }

    private RoleModel realmRole(RealmModel realm) {
        return new ExternalRoleModel(realm, REALM_ROLE_EXTERNAL_ID, REALM_ROLE_NAME, "External realm role");
    }

    private RoleModel clientRole(RealmModel realm) {
        ClientModel client = configuredClient(realm);
        return client == null ? null : clientRole(client);
    }

    private RoleModel clientRole(ClientModel client) {
        return new ExternalRoleModel(client, CLIENT_ROLE_EXTERNAL_ID, CLIENT_ROLE_NAME, "External client role");
    }

    private Stream<RoleModel> filterAndPage(RoleModel role, String search, Integer first, Integer max, String... values) {
        if (role == null || !matches(search, values)) {
            return Stream.empty();
        }
        return paginatedStream(Stream.of(role), first, max);
    }

    private boolean matches(String search, String... values) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return Stream.of(values).anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(normalized));
    }

    @Override
    public void close() {
    }

    private final class ExternalRoleModel implements RoleModel {

        private final RoleContainerModel container;
        private final String externalId;
        private final String name;
        private final String description;

        private ExternalRoleModel(RoleContainerModel container, String externalId, String name, String description) {
            this.container = container;
            this.externalId = externalId;
            this.name = name;
            this.description = description;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getId() {
            return new StorageId(component.getId(), externalId).getId();
        }

        @Override
        public boolean isComposite() {
            return false;
        }

        @Override
        public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) {
            return Stream.empty();
        }

        @Override
        public String getContainerId() {
            return container.getId();
        }

        @Override
        public RoleContainerModel getContainer() {
            return container;
        }

        @Override
        public boolean hasRole(RoleModel role) {
            return getId().equals(role.getId());
        }

        @Override
        public String getFirstAttribute(String name) {
            return null;
        }

        @Override
        public Stream<String> getAttributeStream(String name) {
            return Stream.empty();
        }

        @Override
        public Map<String, List<String>> getAttributes() {
            return Map.of();
        }

        @Override
        public void setDescription(String description) {
            throw readOnly();
        }

        @Override
        public void setName(String name) {
            throw readOnly();
        }

        @Override
        public void addCompositeRole(RoleModel role) {
            throw readOnly();
        }

        @Override
        public void removeCompositeRole(RoleModel role) {
            throw readOnly();
        }

        @Override
        public void setSingleAttribute(String name, String value) {
            throw readOnly();
        }

        @Override
        public void setAttribute(String name, List<String> values) {
            throw readOnly();
        }

        @Override
        public void removeAttribute(String name) {
            throw readOnly();
        }

        private ReadOnlyException readOnly() {
            return new ReadOnlyException("role is read only");
        }
    }
}

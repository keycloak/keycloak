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
package org.keycloak.organization.protocol.mappers;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RoleUtils;

public final class OrganizationRoleMapperUtils {

    public static final String ORGANIZATION_ROLES = "organization_roles";
    public static final String REALM_ACCESS = "realm_access";
    public static final String RESOURCE_ACCESS = "resource_access";
    public static final String ROLES = "roles";

    private OrganizationRoleMapperUtils() {
    }

    public static OrganizationRoleClaims resolveRoleClaims(OrganizationModel organization, UserModel user) {
        if (organization == null || user == null || !organization.isEnabled() || !organization.isMember(user)) {
            return OrganizationRoleClaims.empty();
        }

        Set<RoleModel> organizationRoles = user.getRoleMappingsStream()
                .filter(Objects::nonNull)
                .filter(RoleModel::isOrganizationRole)
                .filter(role -> Objects.equals(organization.getId(), role.getContainerId()))
                .collect(Collectors.toSet());

        if (organizationRoles.isEmpty()) {
            return OrganizationRoleClaims.empty();
        }

        return OrganizationRoleClaims.from(organization, RoleUtils.expandCompositeRoles(organizationRoles));
    }

    @SuppressWarnings("unchecked")
    public static void addToOrganizationClaim(Map<String, Object> organizationClaim, OrganizationRoleClaims claims) {
        if (organizationClaim == null || claims == null || claims.isEmpty()) {
            return;
        }

        if (!claims.getOrganizationRoles().isEmpty()) {
            organizationClaim.put(ORGANIZATION_ROLES, claims.getOrganizationRoles());
        }

        mergeRoles(organizationClaim, REALM_ACCESS, claims.getRealmRoles());

        if (!claims.getClientRoles().isEmpty()) {
            Map<String, Object> resourceAccess = mutableMap(organizationClaim.get(RESOURCE_ACCESS));

            claims.getClientRoles().forEach((clientId, roles) -> {
                Map<String, Object> clientAccess = mutableMap(resourceAccess.get(clientId));
                mergeRoles(clientAccess, ROLES, roles);
                resourceAccess.put(clientId, clientAccess);
            });

            organizationClaim.put(RESOURCE_ACCESS, resourceAccess);
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeRoles(Map<String, Object> parent, String claimName, Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        Object currentValue = parent.get(claimName);
        Map<String, Object> access = claimName.equals(ROLES) ? parent : mutableMap(currentValue);
        Set<String> mergedRoles = new TreeSet<>(roles);
        Object currentRoles = access.get(ROLES);

        if (currentRoles instanceof Collection<?> values) {
            values.stream().filter(Objects::nonNull).map(Object::toString).forEach(mergedRoles::add);
        } else if (currentRoles instanceof String value) {
            mergedRoles.add(value);
        }

        access.put(ROLES, List.copyOf(mergedRoles));

        if (!claimName.equals(ROLES)) {
            parent.put(claimName, access);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mutableMap(Object value) {
        return value instanceof Map<?, ?> ? new LinkedHashMap<>((Map<String, Object>) value) : new LinkedHashMap<>();
    }

    public static final class OrganizationRoleClaims {

        private static final OrganizationRoleClaims EMPTY = new OrganizationRoleClaims(List.of(), List.of(), Map.of());

        private final List<String> organizationRoles;
        private final List<String> realmRoles;
        private final Map<String, List<String>> clientRoles;

        private OrganizationRoleClaims(List<String> organizationRoles, List<String> realmRoles, Map<String, List<String>> clientRoles) {
            this.organizationRoles = organizationRoles;
            this.realmRoles = realmRoles;
            this.clientRoles = clientRoles;
        }

        public static OrganizationRoleClaims empty() {
            return EMPTY;
        }

        public static OrganizationRoleClaims from(OrganizationModel organization, Set<RoleModel> roles) {
            if (organization == null || roles == null || roles.isEmpty()) {
                return EMPTY;
            }

            Set<String> organizationRoles = new TreeSet<>();
            Set<String> realmRoles = new TreeSet<>();
            Map<String, Set<String>> clientRoles = new TreeMap<>();

            for (RoleModel role : roles) {
                if (role == null) {
                    continue;
                }

                if (role.isOrganizationRole()) {
                    if (Objects.equals(organization.getId(), role.getContainerId())) {
                        organizationRoles.add(role.getName());
                    }
                } else if (role.isRealmRole()) {
                    realmRoles.add(role.getName());
                } else if (role.getContainer() instanceof ClientModel client) {
                    clientRoles.computeIfAbsent(client.getClientId(), key -> new TreeSet<>()).add(role.getName());
                }
            }

            if (organizationRoles.isEmpty() && realmRoles.isEmpty() && clientRoles.isEmpty()) {
                return EMPTY;
            }

            Map<String, List<String>> clientRoleClaims = new LinkedHashMap<>();
            clientRoles.forEach((clientId, roleNames) -> clientRoleClaims.put(clientId, List.copyOf(roleNames)));

            return new OrganizationRoleClaims(List.copyOf(organizationRoles), List.copyOf(realmRoles), clientRoleClaims);
        }

        public List<String> getOrganizationRoles() {
            return organizationRoles;
        }

        public List<String> getRealmRoles() {
            return realmRoles;
        }

        public Map<String, List<String>> getClientRoles() {
            return clientRoles;
        }

        public boolean isEmpty() {
            return organizationRoles.isEmpty() && realmRoles.isEmpty() && clientRoles.isEmpty();
        }
    }
}

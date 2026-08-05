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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.protocol.mappers.OrganizationRoleMapperUtils.OrganizationRoleClaims;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OrganizationRoleMapperUtilsTest {

    @Test
    public void resolvesOrganizationRoleClaimsFromDirectMappingsAndComposites() {
        TestContext context = new TestContext();
        TestRole direct = context.addOrganizationRole("direct", "admin", context.organization);
        TestRole sameOrganizationComposite = context.addOrganizationRole("member", "member", context.organization);
        TestRole otherOrganizationComposite = context.addOrganizationRole("other", "other", context.otherOrganization);
        TestRole realmComposite = context.addRealmRole("realm", "realm-admin");
        TestRole clientComposite = context.addClientRole("client-role", "client-admin", context.client);
        direct.composites.addAll(List.of(sameOrganizationComposite.model, otherOrganizationComposite.model, realmComposite.model, clientComposite.model));
        context.user.roleMappings.add(direct.model);
        context.user.roleMappings.add(context.addOrganizationRole("ignored", "ignored", context.otherOrganization).model);

        OrganizationRoleClaims claims = OrganizationRoleMapperUtils.resolveRoleClaims(context.organization, context.user.model);

        assertEquals(List.of("admin", "member"), claims.getOrganizationRoles());
        assertEquals(List.of("realm-admin"), claims.getRealmRoles());
        assertEquals(Map.of("client", List.of("client-admin")), claims.getClientRoles());
    }

    @Test
    public void returnsEmptyClaimsWhenOrganizationOrUserCannotContributeRoles() {
        TestContext context = new TestContext();

        assertSame(OrganizationRoleClaims.empty(), OrganizationRoleMapperUtils.resolveRoleClaims(null, context.user.model));
        assertSame(OrganizationRoleClaims.empty(), OrganizationRoleMapperUtils.resolveRoleClaims(context.organization, null));
        context.organizationEnabled = false;
        assertSame(OrganizationRoleClaims.empty(), OrganizationRoleMapperUtils.resolveRoleClaims(context.organization, context.user.model));
        context.organizationEnabled = true;
        context.member = false;
        assertSame(OrganizationRoleClaims.empty(), OrganizationRoleMapperUtils.resolveRoleClaims(context.organization, context.user.model));
        context.member = true;
        assertSame(OrganizationRoleClaims.empty(), OrganizationRoleMapperUtils.resolveRoleClaims(context.organization, context.user.model));
        assertSame(OrganizationRoleClaims.empty(), OrganizationRoleClaims.from(null, Set.of(context.addRealmRole("realm", "realm").model)));
        assertSame(OrganizationRoleClaims.empty(), OrganizationRoleClaims.from(context.organization, Set.of()));
        assertSame(OrganizationRoleClaims.empty(), OrganizationRoleClaims.from(context.organization,
                Set.of(context.addOrganizationRole("foreign", "foreign", context.otherOrganization).model)));
    }

    @Test
    public void buildsClaimsFromRoleSetAndIgnoresNullsAndForeignOrganizationRoles() {
        TestContext context = new TestContext();
        Set<RoleModel> roles = new HashSet<>();
        roles.add(context.addOrganizationRole("admin", "admin", context.organization).model);
        roles.add(context.addOrganizationRole("foreign", "foreign", context.otherOrganization).model);
        roles.add(context.addRealmRole("realm", "realm").model);
        roles.add(context.addClientRole("client", "client-role", context.client).model);
        roles.add(null);

        OrganizationRoleClaims claims = OrganizationRoleClaims.from(context.organization, roles);

        assertEquals(List.of("admin"), claims.getOrganizationRoles());
        assertEquals(List.of("realm"), claims.getRealmRoles());
        assertEquals(Map.of("client", List.of("client-role")), claims.getClientRoles());
    }

    @Test
    public void mergesClaimsIntoExistingOrganizationClaim() {
        TestContext context = new TestContext();
        Set<RoleModel> roles = Set.of(
                context.addOrganizationRole("admin", "admin", context.organization).model,
                context.addRealmRole("realm", "realm").model,
                context.addClientRole("client", "client-role", context.client).model);
        OrganizationRoleClaims claims = OrganizationRoleClaims.from(context.organization, roles);
        Map<String, Object> organizationClaim = new LinkedHashMap<>();
        organizationClaim.put("groups", List.of("/engineering"));
        organizationClaim.put(OrganizationRoleMapperUtils.REALM_ACCESS, Map.of(OrganizationRoleMapperUtils.ROLES, "existing-realm"));
        organizationClaim.put(OrganizationRoleMapperUtils.RESOURCE_ACCESS, Map.of("client", Map.of(OrganizationRoleMapperUtils.ROLES, List.of("existing-client"))));

        OrganizationRoleMapperUtils.addToOrganizationClaim(organizationClaim, claims);

        assertEquals(List.of("/engineering"), organizationClaim.get("groups"));
        assertEquals(List.of("admin"), organizationClaim.get(OrganizationRoleMapperUtils.ORGANIZATION_ROLES));
        assertEquals(Map.of(OrganizationRoleMapperUtils.ROLES, List.of("existing-realm", "realm")), organizationClaim.get(OrganizationRoleMapperUtils.REALM_ACCESS));
        assertEquals(Map.of("client", Map.of(OrganizationRoleMapperUtils.ROLES, List.of("client-role", "existing-client"))), organizationClaim.get(OrganizationRoleMapperUtils.RESOURCE_ACCESS));
    }

    @Test
    public void mergesOnlyOrganizationRolesWithoutCreatingEmptyAccessClaims() {
        TestContext context = new TestContext();
        OrganizationRoleClaims claims = OrganizationRoleClaims.from(context.organization,
                Set.of(context.addOrganizationRole("admin", "admin", context.organization).model));
        Map<String, Object> organizationClaim = new LinkedHashMap<>();

        OrganizationRoleMapperUtils.addToOrganizationClaim(organizationClaim, claims);

        assertEquals(List.of("admin"), organizationClaim.get(OrganizationRoleMapperUtils.ORGANIZATION_ROLES));
        assertTrue(!organizationClaim.containsKey(OrganizationRoleMapperUtils.REALM_ACCESS));
        assertTrue(!organizationClaim.containsKey(OrganizationRoleMapperUtils.RESOURCE_ACCESS));
    }

    @Test
    public void ignoresEmptyClaimsAndRebuildsUnexpectedAccessContainers() {
        TestContext context = new TestContext();
        OrganizationRoleClaims claims = OrganizationRoleClaims.from(context.organization, Set.of(
                context.addRealmRole("realm", "realm").model,
                context.addClientRole("client", "client-role", context.client).model));
        Map<String, Object> organizationClaim = new LinkedHashMap<>();
        organizationClaim.put(OrganizationRoleMapperUtils.REALM_ACCESS, "not-a-map");
        organizationClaim.put(OrganizationRoleMapperUtils.RESOURCE_ACCESS, "not-a-map");
        OrganizationRoleMapperUtils.addToOrganizationClaim(null, OrganizationRoleClaims.empty());
        OrganizationRoleMapperUtils.addToOrganizationClaim(organizationClaim, null);
        OrganizationRoleMapperUtils.addToOrganizationClaim(organizationClaim, OrganizationRoleClaims.empty());

        assertEquals("not-a-map", organizationClaim.get(OrganizationRoleMapperUtils.REALM_ACCESS));
        assertEquals("not-a-map", organizationClaim.get(OrganizationRoleMapperUtils.RESOURCE_ACCESS));
        assertTrue(OrganizationRoleClaims.empty().isEmpty());

        OrganizationRoleMapperUtils.addToOrganizationClaim(organizationClaim, claims);

        assertEquals(Map.of(OrganizationRoleMapperUtils.ROLES, List.of("realm")), organizationClaim.get(OrganizationRoleMapperUtils.REALM_ACCESS));
        assertEquals(Map.of("client", Map.of(OrganizationRoleMapperUtils.ROLES, List.of("client-role"))), organizationClaim.get(OrganizationRoleMapperUtils.RESOURCE_ACCESS));
    }

    private static class TestContext {
        private final RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm";
            default -> defaultValue(method.getReturnType());
        });
        private final ClientModel client = proxy(ClientModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "client-id";
            case "getClientId" -> "client";
            case "getRealm" -> realm;
            default -> defaultValue(method.getReturnType());
        });
        private boolean organizationEnabled = true;
        private boolean member = true;
        private final OrganizationModel organization = organization("org", "acme");
        private final OrganizationModel otherOrganization = organization("other-org", "other");
        private final TestUser user = new TestUser("user");

        private OrganizationModel organization(String id, String alias) {
            return proxy(OrganizationModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getAlias" -> alias;
                case "getRealm" -> realm;
                case "isEnabled" -> organizationEnabled;
                case "isMember" -> member && args[0] == user.model;
                default -> defaultValue(method.getReturnType());
            });
        }

        private TestRole addOrganizationRole(String id, String name, OrganizationModel organization) {
            return new TestRole(id, name, RoleModel.Type.ORGANIZATION, organization);
        }

        private TestRole addRealmRole(String id, String name) {
            return new TestRole(id, name, RoleModel.Type.REALM, realm);
        }

        private TestRole addClientRole(String id, String name, ClientModel client) {
            return new TestRole(id, name, RoleModel.Type.CLIENT, client);
        }
    }

    private static class TestUser {
        private final Set<RoleModel> roleMappings = new LinkedHashSet<>();
        private final UserModel model;

        TestUser(String id) {
            model = proxy(UserModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getRoleMappingsStream" -> roleMappings.stream();
                case "getGroupsStream" -> Stream.empty();
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    private static class TestRole {
        private final List<RoleModel> composites = new java.util.ArrayList<>();
        private final RoleModel model;

        TestRole(String id, String name, RoleModel.Type type, RoleContainerModel container) {
            model = proxy(RoleModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getName" -> name;
                case "getType" -> type;
                case "isClientRole" -> type == RoleModel.Type.CLIENT;
                case "isRealmRole" -> type == RoleModel.Type.REALM;
                case "isOrganizationRole" -> type == RoleModel.Type.ORGANIZATION;
                case "getContainer" -> container;
                case "getContainerId" -> container.getId();
                case "getCompositesStream" -> composites.stream();
                case "hasRole" -> Objects.equals(proxy, args[0]) || composites.contains(args[0]);
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.getDeclaringClass().equals(Object.class)) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> null;
                };
            }
            return handler.invoke(proxy, method, args);
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        return 0;
    }
}

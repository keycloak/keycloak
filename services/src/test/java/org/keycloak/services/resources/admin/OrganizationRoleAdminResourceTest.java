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
package org.keycloak.services.resources.admin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RolePermissionEvaluator;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class OrganizationRoleAdminResourceTest {

    @Test
    public void realmScopeMappingsRejectOrganizationRolesById() {
        RoleModel organizationRole = mockOrganizationRole("role-1", mockOrganization("org-1"));
        RealmModel realm = mockRealm(organizationRole);
        ScopeMappedResource resource = new ScopeMappedResource(realm, null, mockScopeContainer(), mockSession(), adminEventBuilder(realm), () -> {}, () -> {});
        RoleRepresentation representation = new RoleRepresentation();
        representation.setId(organizationRole.getId());

        assertThrows(NotFoundException.class, () -> resource.addRealmScopeMappings(List.of(representation)));
        assertThrows(NotFoundException.class, () -> resource.deleteRealmScopeMappings(List.of(representation)));
    }

    @Test
    public void realmScopeMappingsAcceptRealmRolesById() {
        RealmModel realm = mockRealm();
        RoleModel realmRole = mockRealmRole("realm-role", realm);
        realm = mockRealm(realmRole);
        AtomicInteger additions = new AtomicInteger();
        AtomicInteger removals = new AtomicInteger();
        ScopeContainerModel scopeContainer = mockScopeContainer(additions, removals);
        ScopeMappedResource resource = new ScopeMappedResource(realm, null, scopeContainer, mockSession(), adminEventBuilder(realm), () -> {}, () -> {});
        RoleRepresentation representation = new RoleRepresentation();
        representation.setId(realmRole.getId());

        resource.addRealmScopeMappings(List.of(representation));
        resource.deleteRealmScopeMappings(List.of(representation));

        assertEquals(1, additions.get());
        assertEquals(1, removals.get());
    }

    @Test
    public void roleCompositesRejectOrganizationRolesUnderRealmRoles() {
        OrganizationModel organization = mockOrganization("org-1");
        RoleModel organizationRole = mockOrganizationRole("role-1", organization);
        RealmModel realm = mockRealm(organizationRole);
        RoleModel realmRole = mockRealmRole("realm-role", realm);
        RoleRepresentation representation = new RoleRepresentation();
        representation.setId(organizationRole.getId());

        TestRoleResource resource = new TestRoleResource(realm);

        assertThrows(BadRequestException.class, () -> resource.addComposites(
                mockAdminPermissionEvaluator(), null, null, List.of(representation), realmRole));
    }

    @Test
    public void rolesByIdRejectOrganizationRoles() {
        OrganizationModel organization = mockOrganization("org-1");
        RoleModel organizationRole = mockOrganizationRole("organization-role", organization);
        RealmModel initialRealm = mockRealm();
        RoleModel realmRole = mockRealmRole("realm-role", initialRealm);
        RealmModel realm = mockRealm(organizationRole, realmRole);
        RoleByIdResource resource = new RoleByIdResource(mockSession(realm), mockAdminPermissionEvaluator(), adminEventBuilder(realm));

        assertThrows(NotFoundException.class, () -> resource.getRoleModel(organizationRole.getId()));
        assertEquals(realmRole, resource.getRoleModel(realmRole.getId()));
    }

    @Test
    public void roleCompositesValidateAllRolesBeforeMutation() {
        OrganizationModel organization = mockOrganization("org-1");
        RoleModel organizationRole = mockOrganizationRole("organization-role", organization);
        RealmModel initialRealm = mockRealm();
        RoleModel composite = mockRealmRole("composite-role", initialRealm);
        RealmModel realm = mockRealm(composite, organizationRole);
        AtomicInteger mutations = new AtomicInteger();
        RoleModel parent = mockRealmRole("parent-role", realm, mutations);
        TestRoleResource resource = new TestRoleResource(realm);
        RoleRepresentation valid = new RoleRepresentation();
        valid.setId(composite.getId());
        RoleRepresentation invalid = new RoleRepresentation();
        invalid.setId(organizationRole.getId());

        assertThrows(BadRequestException.class, () -> resource.addComposites(
                mockAdminPermissionEvaluator(), adminEventBuilder(realm), uriInfo(), List.of(valid, invalid), parent));
        assertEquals(0, mutations.get());

        resource.addComposites(mockAdminPermissionEvaluator(), adminEventBuilder(realm), uriInfo(), List.of(valid), parent);
        assertEquals(1, mutations.get());
    }

    private static class TestRoleResource extends RoleResource {
        TestRoleResource(RealmModel realm) {
            super(realm);
        }
    }

    private static KeycloakSession mockSession() {
        return mockSession(null);
    }

    private static KeycloakSession mockSession(RealmModel realm) {
        KeycloakSessionFactory sessionFactory = proxy(KeycloakSessionFactory.class, (factoryProxy, method, args) -> {
            if ("getProviderFactoriesStream".equals(method.getName()) && args[0].equals(EventListenerProvider.class)) {
                return Stream.empty();
            }
            return defaultValue(method.getReturnType());
        });
        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> {
            if ("getKeycloakSessionFactory".equals(method.getName())) {
                return sessionFactory;
            }
            if ("getContext".equals(method.getName())) {
                return proxy(KeycloakContext.class, (contextProxy, contextMethod, contextArgs) -> switch (contextMethod.getName()) {
                    case "getRealm" -> realm;
                    case "getUri" -> uriInfo();
                    default -> defaultValue(contextMethod.getReturnType());
                });
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static RealmModel mockRealm(RoleModel... roles) {
        Map<String, RoleModel> rolesById = Arrays.stream(roles).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(RoleModel::getId, role -> role));
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm-1";
            case "getName" -> "realm";
            case "getRoleById" -> rolesById.get(args[0]);
            case "getEventsListenersStream" -> Stream.empty();
            case "isAdminEventsEnabled" -> false;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static AdminEventBuilder adminEventBuilder(RealmModel realm) {
        ClientModel client = proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "client-1";
            default -> defaultValue(method.getReturnType());
        });
        UserModel user = proxy(UserModel.class, (userProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "user-1";
            default -> defaultValue(method.getReturnType());
        });
        ClientConnection connection = proxy(ClientConnection.class, (connectionProxy, method, args) -> switch (method.getName()) {
            case "getRemoteAddr", "getRemoteHost", "getLocalAddr" -> "remote-host";
            default -> defaultValue(method.getReturnType());
        });

        return new AdminEventBuilder(realm, new AdminAuth(realm, null, user, client), mockSession(), connection);
    }

    private static OrganizationModel mockOrganization(String id) {
        return proxy(OrganizationModel.class, (organizationProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel mockOrganizationRole(String id, OrganizationModel organization) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> "organization-role";
            case "getContainer" -> organization;
            case "getContainerId" -> organization.getId();
            case "getType" -> RoleModel.Type.ORGANIZATION;
            case "isOrganizationRole" -> true;
            case "isRealmRole", "isClientRole" -> false;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel mockRealmRole(String id, RealmModel realm) {
        return mockRealmRole(id, realm, null);
    }

    private static RoleModel mockRealmRole(String id, RealmModel realm, AtomicInteger mutations) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> "realm-role";
            case "getContainer" -> realm;
            case "getContainerId" -> realm.getId();
            case "getType" -> RoleModel.Type.REALM;
            case "isRealmRole" -> true;
            case "isOrganizationRole", "isClientRole" -> false;
            case "addCompositeRole" -> {
                if (mutations != null) mutations.incrementAndGet();
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ScopeContainerModel mockScopeContainer() {
        return proxy(ScopeContainerModel.class, (scopeProxy, method, args) -> {
            if ("addScopeMapping".equals(method.getName()) || "deleteScopeMapping".equals(method.getName())) {
                throw new AssertionError("Organization role should be rejected before scope mutation");
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static ScopeContainerModel mockScopeContainer(AtomicInteger additions, AtomicInteger removals) {
        return proxy(ScopeContainerModel.class, (scopeProxy, method, args) -> switch (method.getName()) {
            case "addScopeMapping" -> {
                additions.incrementAndGet();
                yield null;
            }
            case "deleteScopeMapping" -> {
                removals.incrementAndGet();
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });
    }

    private static KeycloakUriInfo uriInfo() {
        UriInfo delegate = proxy(UriInfo.class, (uriProxy, method, args) -> "getPath".equals(method.getName())
                ? "admin/realms/realm/test" : defaultValue(method.getReturnType()));
        HostnameProvider hostnameProvider = proxy(HostnameProvider.class, (providerProxy, method, args) ->
                "getBaseUri".equals(method.getName()) ? URI.create("http://localhost") : defaultValue(method.getReturnType()));
        KeycloakSession session = proxy(KeycloakSession.class, (sessionProxy, method, args) ->
                "getProvider".equals(method.getName()) && args[0].equals(HostnameProvider.class)
                        ? hostnameProvider : defaultValue(method.getReturnType()));
        return new KeycloakUriInfo(session, UrlType.FRONTEND, delegate);
    }

    private static AdminPermissionEvaluator mockAdminPermissionEvaluator() {
        RolePermissionEvaluator rolePermissions = proxy(RolePermissionEvaluator.class,
                (rolesProxy, method, args) -> defaultValue(method.getReturnType()));
        return proxy(AdminPermissionEvaluator.class, (authProxy, method, args) -> {
            if ("roles".equals(method.getName())) {
                return rolePermissions;
            }
            return defaultValue(method.getReturnType());
        });
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

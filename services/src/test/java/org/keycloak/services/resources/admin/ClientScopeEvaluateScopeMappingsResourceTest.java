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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RolePermissionEvaluator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ClientScopeEvaluateScopeMappingsResourceTest {

    @Test
    public void filtersRestrictedScopeMappingsByRoleVisibility() {
        Fixture fixture = new Fixture(false);

        List<RoleRepresentation> granted = fixture.resource.getGrantedScopeMappings().toList();
        List<RoleRepresentation> notGranted = fixture.resource.getNotGrantedScopeMappings().toList();

        assertEquals(Set.of("visible-granted"), names(granted));
        assertEquals(Set.of("visible-not-granted"), names(notGranted));
        assertBrief(granted);
        assertBrief(notGranted);
    }

    @Test
    public void filtersFullScopeMappingsByRoleVisibility() {
        Fixture fixture = new Fixture(true);

        List<RoleRepresentation> granted = fixture.resource.getGrantedScopeMappings().toList();
        List<RoleRepresentation> notGranted = fixture.resource.getNotGrantedScopeMappings().toList();

        assertEquals(Set.of("visible-granted", "visible-not-granted"), names(granted));
        assertEquals(Set.of(), names(notGranted));
        assertBrief(granted);
    }

    @Test
    public void skipsRoleEvaluationForFullScopeNotGrantedMappings() {
        Fixture fixture = new Fixture(true);

        assertEquals(List.of(), fixture.resource.getNotGrantedScopeMappings().toList());
        assertEquals(0, fixture.scopeMappingChecks.get());
    }

    @Test
    public void evaluatesVisibilityOncePerRoleForRestrictedNotGrantedMappings() {
        Fixture fixture = new Fixture(false);

        assertEquals(Set.of("visible-not-granted"), names(fixture.resource.getNotGrantedScopeMappings().toList()));
        assertEquals(4, fixture.scopeMappingChecks.get());
    }

    private static Set<String> names(List<RoleRepresentation> roles) {
        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    private static void assertBrief(List<RoleRepresentation> roles) {
        roles.forEach(role -> assertNull(role.getAttributes()));
    }

    private static final class Fixture {
        private final ClientScopeEvaluateScopeMappingsResource resource;
        private final AtomicInteger scopeMappingChecks = new AtomicInteger();

        private Fixture(boolean fullScopeAllowed) {
            RoleModel visibleGranted = role("visible-granted");
            RoleModel hiddenGranted = role("hidden-granted");
            RoleModel visibleNotGranted = role("visible-not-granted");
            RoleModel hiddenNotGranted = role("hidden-not-granted");
            List<RoleModel> roles = List.of(visibleGranted, hiddenGranted, visibleNotGranted, hiddenNotGranted);
            Set<RoleModel> grantedRoles = Set.of(visibleGranted, hiddenGranted);
            Set<RoleModel> mapOnlyRoles = Set.of(visibleGranted, visibleNotGranted);

            ClientModel client = proxy(ClientModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "isFullScopeAllowed" -> fullScopeAllowed;
                case "getClientScopes" -> Map.of();
                case "hasScope" -> grantedRoles.contains(args[0]);
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "scope-client";
                default -> throw new UnsupportedOperationException(method.toString());
            });

            RolePermissionEvaluator rolePermissions = proxy(RolePermissionEvaluator.class, (proxy, method, args) -> switch (method.getName()) {
                case "canView" -> false;
                case "canMapClientScope" -> mapOnlyRoles.contains(args[0]);
                case "canViewScopeMapping" -> {
                    scopeMappingChecks.incrementAndGet();
                    yield mapOnlyRoles.contains(args[0]);
                }
                case "toString" -> "map-only-role-permissions";
                default -> throw new UnsupportedOperationException(method.toString());
            });
            AdminPermissionEvaluator permissions = proxy(AdminPermissionEvaluator.class, (proxy, method, args) -> {
                if (method.getName().equals("roles")) {
                    return rolePermissions;
                }
                throw new UnsupportedOperationException(method.toString());
            });
            RoleContainerModel container = proxy(RoleContainerModel.class, (proxy, method, args) -> {
                if (method.getName().equals("getRolesStream")) {
                    return roles.stream();
                }
                throw new UnsupportedOperationException(method.toString());
            });

            resource = new ClientScopeEvaluateScopeMappingsResource(null, container, permissions, client, null);
        }
    }

    private static RoleModel role(String name) {
        return proxy(RoleModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId", "getName" -> name;
            case "getDescription" -> "description-" + name;
            case "isComposite", "isClientRole" -> false;
            case "getContainerId" -> "role-container";
            case "getAttributes" -> Map.of("classification", List.of("sensitive"));
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> name;
            default -> throw new UnsupportedOperationException(method.toString());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }
}

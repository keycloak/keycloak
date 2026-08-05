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

package org.keycloak.storage;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;

import org.junit.Assert;
import org.junit.Test;

public class RoleStorageManagerTest {

    @Test
    public void shouldDelegateOrganizationRolesToLocalStorage() {
        OrganizationModel organization = proxy(OrganizationModel.class);
        RoleModel role = proxy(RoleModel.class);
        LocalRoleProvider localStorage = new LocalRoleProvider(role);
        RoleStorageManager manager = new RoleStorageManager(session(localStorage), 3000);

        Assert.assertSame(role, manager.addOrganizationRole(organization, "member"));
        Assert.assertSame(organization, localStorage.organization);
        Assert.assertNull(localStorage.id);
        Assert.assertEquals("member", localStorage.name);

        Assert.assertSame(role, manager.addOrganizationRole(organization, "role-id", "admin"));
        Assert.assertEquals("role-id", localStorage.id);
        Assert.assertEquals("admin", localStorage.name);

        Assert.assertSame(role, manager.getOrganizationRole(organization, "admin"));
        Assert.assertEquals("admin", localStorage.name);

        Assert.assertSame(role, manager.getRoleById(organization, "role-id"));
        Assert.assertEquals("role-id", localStorage.id);

        Assert.assertEquals(List.of(role), manager.getOrganizationRolesStream(organization, 1, 2).toList());
        Assert.assertEquals(Integer.valueOf(1), localStorage.first);
        Assert.assertEquals(Integer.valueOf(2), localStorage.max);

        Assert.assertEquals(List.of(role), manager.searchForOrganizationRolesStream(organization, "adm", 3, 4).toList());
        Assert.assertEquals("adm", localStorage.search);
        Assert.assertEquals(Integer.valueOf(3), localStorage.first);
        Assert.assertEquals(Integer.valueOf(4), localStorage.max);

        Assert.assertEquals(11, manager.getOrganizationRolesCount(organization, "adm"));
        Assert.assertEquals("adm", localStorage.search);

        manager.removeRoles(organization);
        Assert.assertTrue(localStorage.removedOrganizationRoles);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> { throw new UnsupportedOperationException(); });
    }

    private static KeycloakSession session(RoleProvider localStorage) {
        return (KeycloakSession) Proxy.newProxyInstance(RoleStorageManagerTest.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    if ("getProvider".equals(method.getName()) && args.length == 1 && RoleProvider.class.equals(args[0])) {
                        return localStorage;
                    }
                    throw new UnsupportedOperationException();
                });
    }

    private static final class LocalRoleProvider implements RoleProvider {

        private final RoleModel role;
        private OrganizationModel organization;
        private String id;
        private String name;
        private String search;
        private Integer first;
        private Integer max;
        private boolean removedOrganizationRoles;

        private LocalRoleProvider(RoleModel role) {
            this.role = role;
        }

        @Override
        public RoleModel addOrganizationRole(OrganizationModel organization, String id, String name) {
            this.organization = organization;
            this.id = id;
            this.name = name;
            return role;
        }

        @Override
        public RoleModel getOrganizationRole(OrganizationModel organization, String name) {
            this.organization = organization;
            this.name = name;
            return role;
        }

        @Override
        public RoleModel getRoleById(OrganizationModel organization, String id) {
            this.organization = organization;
            this.id = id;
            return role;
        }

        @Override
        public Stream<RoleModel> getOrganizationRolesStream(OrganizationModel organization, Integer first, Integer max) {
            this.organization = organization;
            this.first = first;
            this.max = max;
            return Stream.of(role);
        }

        @Override
        public Stream<RoleModel> searchForOrganizationRolesStream(OrganizationModel organization, String search, Integer first, Integer max) {
            this.organization = organization;
            this.search = search;
            this.first = first;
            this.max = max;
            return Stream.of(role);
        }

        @Override
        public long getOrganizationRolesCount(OrganizationModel organization, String search) {
            this.organization = organization;
            this.search = search;
            return 11;
        }

        @Override
        public void removeRoles(OrganizationModel organization) {
            this.organization = organization;
            this.removedOrganizationRoles = true;
        }

        @Override public RoleModel addRealmRole(RealmModel realm, String id, String name) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public boolean removeRole(RoleModel role) { throw new UnsupportedOperationException(); }
        @Override public void removeRoles(RealmModel realm) { throw new UnsupportedOperationException(); }
        @Override public RoleModel addClientRole(ClientModel client, String id, String name) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public void removeRoles(ClientModel client) { throw new UnsupportedOperationException(); }
        @Override public RoleModel getRealmRole(RealmModel realm, String name) { throw new UnsupportedOperationException(); }
        @Override public RoleModel getRoleById(RealmModel realm, String id) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public RoleModel getClientRole(ClientModel client, String name) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, String search, Stream<String> excludedIds, Integer first, Integer max) { throw new UnsupportedOperationException(); }
        @Override public void close() { }
    }
}

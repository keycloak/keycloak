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

package org.keycloak.models;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.utils.RoleUtils;
import org.keycloak.utils.KeycloakSessionUtil;

import org.junit.Assert;
import org.junit.Test;

public class OrganizationRoleSpiTest {

    @Test
    public void shouldClassifyAllRoleTypes() {
        RoleModel realmRole = new TestRole("realm", proxy(RealmModel.class));
        RoleModel clientRole = new TestRole("client", proxy(ClientModel.class));
        RoleModel organizationRole = new TestRole("organization", new TestOrganization());

        Assert.assertEquals(RoleModel.Type.REALM, realmRole.getType());
        Assert.assertTrue(realmRole.isRealmRole());
        Assert.assertFalse(realmRole.isOrganizationRole());
        Assert.assertTrue(RoleUtils.isRealmRole(realmRole));

        Assert.assertEquals(RoleModel.Type.CLIENT, clientRole.getType());
        Assert.assertFalse(clientRole.isRealmRole());
        Assert.assertFalse(clientRole.isOrganizationRole());

        Assert.assertEquals(RoleModel.Type.ORGANIZATION, organizationRole.getType());
        Assert.assertFalse(organizationRole.isRealmRole());
        Assert.assertTrue(organizationRole.isOrganizationRole());
        Assert.assertFalse(RoleUtils.isRealmRole(organizationRole));
    }

    @Test
    public void shouldExposeStableTypeIntegerValues() {
        Assert.assertEquals(0, RoleModel.Type.REALM.intValue());
        Assert.assertEquals(1, RoleModel.Type.CLIENT.intValue());
        Assert.assertEquals(2, RoleModel.Type.ORGANIZATION.intValue());

        for (RoleModel.Type type : RoleModel.Type.values()) {
            Assert.assertSame(type, RoleModel.Type.valueOf(type.intValue()));
        }

        IllegalArgumentException cause = Assert.assertThrows(IllegalArgumentException.class, () -> RoleModel.Type.valueOf(99));
        Assert.assertTrue(cause.getMessage().contains("99"));
    }

    @Test
    public void shouldDetectRolesFromClientByTypeAndContainerId() {
        ClientModel client = client("client");
        ClientModel otherClient = client("other-client");
        RoleModel clientRole = new TestRole("client-role", client);
        RoleModel realmRole = new TestRole("realm-role", proxy(RealmModel.class));
        RoleModel organizationRole = new TestRole("organization-role", new TestOrganization());

        Assert.assertTrue(RoleUtils.isRoleFromClient(clientRole, client));
        Assert.assertFalse(RoleUtils.isRoleFromClient(clientRole, otherClient));
        Assert.assertFalse(RoleUtils.isRoleFromClient(realmRole, client));
        Assert.assertFalse(RoleUtils.isRoleFromClient(organizationRole, client));
    }

    @Test
    public void shouldResolveOrganizationRealmWhenExpandingComposites() {
        RealmModel realm = proxy(RealmModel.class);
        TestOrganization organization = new TestOrganization() {
            @Override
            public RealmModel getRealm() {
                return realm;
            }
        };
        TestRole role = new TestRole("organization-role", organization);
        RealmModel[] resolvedRealm = new RealmModel[1];
        TestRoleProvider roleProvider = new TestRoleProvider() {
            @Override
            public Stream<RoleModel> getCompositeRolesStream(RealmModel realm, Set<String> parentRoleIds) {
                resolvedRealm[0] = realm;
                return Stream.empty();
            }
        };
        KeycloakSession session = (KeycloakSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    if ("roles".equals(method.getName())) {
                        return roleProvider;
                    }
                    throw new UnsupportedOperationException();
                });
        KeycloakSession previous = KeycloakSessionUtil.setKeycloakSession(session);

        try {
            Assert.assertEquals(Set.of(role), RoleUtils.expandCompositeRoles(Set.of(role)));
            Assert.assertSame(realm, resolvedRealm[0]);
        } finally {
            KeycloakSessionUtil.setKeycloakSession(previous);
        }
    }

    @Test
    public void shouldExposeOrganizationAsRoleContainerWithoutBreakingExistingProviders() {
        TestOrganization organization = new TestOrganization();
        RoleModel role = new TestRole("role", organization);

        Assert.assertTrue(RoleContainerModel.class.isAssignableFrom(OrganizationModel.class));
        Assert.assertThrows(UnsupportedOperationException.class, organization::getRealm);
        Assert.assertThrows(UnsupportedOperationException.class, organization::getDefaultRole);
        Assert.assertThrows(UnsupportedOperationException.class, () -> organization.setDefaultRole(role));
        Assert.assertThrows(UnsupportedOperationException.class, () -> organization.getRole("role"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> organization.addRole("role"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> organization.addRole("id", "role"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> organization.removeRole(role));
        Assert.assertThrows(UnsupportedOperationException.class, organization::getRolesStream);
        Assert.assertThrows(UnsupportedOperationException.class, () -> organization.getRolesStream(0, 10));
        Assert.assertThrows(UnsupportedOperationException.class, () -> organization.searchForRolesStream("role", 0, 10));
    }

    @Test
    public void shouldManageDefaultRoleCompositesThroughRoleModel() {
        TestRole defaultRole = new TestRole("default", new TestOrganization());
        TestRole composite = new TestRole("composite", proxy(RealmModel.class));
        TestOrganization organization = new TestOrganization() {
            @Override
            public RoleModel getDefaultRole() {
                return defaultRole;
            }
        };

        organization.addToDefaultRoles(composite);

        Assert.assertEquals(List.of(composite), defaultRole.composites);
    }

    @Test
    public void shouldDelegateOrganizationAddRoleConvenienceMethod() {
        RoleModel role = new TestRole("member", proxy(RealmModel.class));
        String[] arguments = new String[2];
        TestOrganization organization = new TestOrganization() {
            @Override
            public RoleModel addRole(String id, String name) {
                arguments[0] = id;
                arguments[1] = name;
                return role;
            }
        };

        Assert.assertSame(role, organization.addRole("member"));
        Assert.assertNull(arguments[0]);
        Assert.assertEquals("member", arguments[1]);
    }

    @Test
    public void shouldDelegateRoleProviderConvenienceMethods() {
        TestOrganization organization = new TestOrganization();
        TestRole role = new TestRole("member", organization);
        DelegatingRoleProvider provider = new DelegatingRoleProvider(role);

        Assert.assertSame(role, provider.addOrganizationRole(organization, "member"));
        Assert.assertSame(organization, provider.organization);
        Assert.assertNull(provider.id);
        Assert.assertEquals("member", provider.name);

        Assert.assertEquals(List.of(role), provider.getOrganizationRolesStream(organization).toList());
        Assert.assertNull(provider.first);
        Assert.assertNull(provider.max);

        Assert.assertEquals(7, provider.getOrganizationRolesCount(organization));
        Assert.assertNull(provider.search);
    }

    @Test
    public void shouldFailExplicitlyWhenProviderDoesNotSupportOrganizationRoles() {
        TestOrganization organization = new TestOrganization();
        TestRole role = new TestRole("member", organization);
        RoleProvider provider = new TestRoleProvider();

        Assert.assertThrows(UnsupportedOperationException.class, () -> provider.addOrganizationRole(organization, "id", "member"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> provider.getOrganizationRole(organization, "member"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> provider.getRoleById(organization, "id"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> provider.searchForOrganizationRolesStream(organization, "member", 0, 10));
        Assert.assertThrows(UnsupportedOperationException.class, () -> provider.getOrganizationRolesStream(organization, 0, 10));
        Assert.assertThrows(UnsupportedOperationException.class, () -> provider.getOrganizationRolesCount(organization, "member"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> provider.removeRoles(organization));
        Assert.assertEquals(RoleModel.Type.ORGANIZATION, role.getType());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> { throw new UnsupportedOperationException(); });
    }

    private static ClientModel client(String id) {
        return (ClientModel) Proxy.newProxyInstance(ClientModel.class.getClassLoader(), new Class<?>[] { ClientModel.class },
                (proxy, method, args) -> {
                    if ("getId".equals(method.getName())) {
                        return id;
                    }
                    throw new UnsupportedOperationException();
                });
    }

    private static class TestOrganization implements OrganizationModel {

        @Override public String getId() { return "organization"; }
        @Override public void setName(String name) { }
        @Override public String getName() { return "organization"; }
        @Override public String getAlias() { return "organization"; }
        @Override public void setAlias(String alias) { }
        @Override public boolean isEnabled() { return true; }
        @Override public void setEnabled(boolean enabled) { }
        @Override public String getDescription() { return null; }
        @Override public void setDescription(String description) { }
        @Override public String getRedirectUrl() { return null; }
        @Override public void setRedirectUrl(String redirectUrl) { }
        @Override public Map<String, List<String>> getAttributes() { return Map.of(); }
        @Override public void setAttributes(Map<String, List<String>> attributes) { }
        @Override public Stream<OrganizationDomainModel> getDomains() { return Stream.empty(); }
        @Override public void setDomains(Set<OrganizationDomainModel> domains) { }
        @Override public Stream<IdentityProviderModel> getIdentityProviders() { return Stream.empty(); }
        @Override public boolean isManaged(UserModel user) { return false; }
        @Override public boolean isMember(UserModel user) { return false; }
    }

    private static class TestRole implements RoleModel {

        private final String id;
        private final RoleContainerModel container;
        private final List<RoleModel> composites = new java.util.ArrayList<>();

        private TestRole(String id, RoleContainerModel container) {
            this.id = id;
            this.container = container;
        }

        @Override public String getName() { return id; }
        @Override public String getDescription() { return null; }
        @Override public void setDescription(String description) { }
        @Override public String getId() { return id; }
        @Override public void setName(String name) { }
        @Override public boolean isComposite() { return !composites.isEmpty(); }
        @Override public void addCompositeRole(RoleModel role) { composites.add(role); }
        @Override public void removeCompositeRole(RoleModel role) { composites.remove(role); }
        @Override public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) { return composites.stream(); }
        @Override public String getContainerId() { return container.getId(); }
        @Override public RoleContainerModel getContainer() { return container; }
        @Override public boolean hasRole(RoleModel role) { return composites.contains(role); }
        @Override public void setSingleAttribute(String name, String value) { }
        @Override public void setAttribute(String name, List<String> values) { }
        @Override public void removeAttribute(String name) { }
        @Override public Stream<String> getAttributeStream(String name) { return Stream.empty(); }
        @Override public Map<String, List<String>> getAttributes() { return Map.of(); }
    }

    private static class TestRoleProvider implements RoleProvider {

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

    private static final class DelegatingRoleProvider extends TestRoleProvider {

        private final RoleModel role;
        private OrganizationModel organization;
        private String id;
        private String name;
        private Integer first;
        private Integer max;
        private String search;

        private DelegatingRoleProvider(RoleModel role) {
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
        public Stream<RoleModel> getOrganizationRolesStream(OrganizationModel organization, Integer first, Integer max) {
            this.organization = organization;
            this.first = first;
            this.max = max;
            return Stream.of(role);
        }

        @Override
        public long getOrganizationRolesCount(OrganizationModel organization, String search) {
            this.organization = organization;
            this.search = search;
            return 7;
        }
    }
}

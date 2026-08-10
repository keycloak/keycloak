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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.utils.RoleUtils;
import org.keycloak.organization.OrganizationProvider;

import org.junit.Assert;
import org.junit.Test;

public class OrganizationRoleSpiTest {

    @Test
    public void shouldPreserveRoleTypeAndContainerContracts() {
        RoleModel realmRole = new TestRole("realm", proxy(RealmModel.class));
        ClientModel client = client("client");
        RoleModel clientRole = new TestRole("client", client);
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

        Assert.assertEquals(0, RoleModel.Type.REALM.intValue());
        Assert.assertEquals(1, RoleModel.Type.CLIENT.intValue());
        Assert.assertEquals(2, RoleModel.Type.ORGANIZATION.intValue());

        for (RoleModel.Type type : RoleModel.Type.values()) {
            Assert.assertSame(type, RoleModel.Type.valueOf(type.intValue()));
        }

        IllegalArgumentException cause = Assert.assertThrows(IllegalArgumentException.class, () -> RoleModel.Type.valueOf(99));
        Assert.assertTrue(cause.getMessage().contains("99"));

        ClientModel otherClient = client("other-client");
        Assert.assertTrue(RoleUtils.isRoleFromClient(clientRole, client));
        Assert.assertFalse(RoleUtils.isRoleFromClient(clientRole, otherClient));
        Assert.assertFalse(RoleUtils.isRoleFromClient(realmRole, client));
        Assert.assertFalse(RoleUtils.isRoleFromClient(organizationRole, client));

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
    public void shouldPreserveOrganizationProviderFallbackContracts() {
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

        TestOrganization providerOrganization = new TestOrganization();
        TestRole providerRole = new TestRole("member", providerOrganization);
        DelegatingRoleProvider provider = new DelegatingRoleProvider(providerRole);

        Assert.assertSame(providerRole, provider.addOrganizationRole(providerOrganization, "member"));
        Assert.assertSame(providerOrganization, provider.organization);
        Assert.assertNull(provider.id);
        Assert.assertEquals("member", provider.name);

        Assert.assertEquals(List.of(providerRole), provider.getOrganizationRolesStream(providerOrganization).toList());
        Assert.assertNull(provider.first);
        Assert.assertNull(provider.max);

        Assert.assertEquals(7, provider.getOrganizationRolesCount(providerOrganization));
        Assert.assertNull(provider.search);

        OrganizationProvider memberProvider = organizationProvider(List.of(
                user("outsider"),
                user("member-a", providerRole),
                user("member-b", providerRole)));

        Assert.assertEquals(List.of("member-a", "member-b"),
                memberProvider.getRoleMembersStream(providerOrganization, providerRole, "member", 0, 2).map(UserModel::getUsername).toList());
        Assert.assertEquals(List.of("member-a"),
                memberProvider.getRoleMembersStream(providerOrganization, providerRole, "member", 0, 1).map(UserModel::getUsername).toList());
        Assert.assertEquals(List.of("member-b"),
                memberProvider.getRoleMembersStream(providerOrganization, providerRole, "member", 1, 1).map(UserModel::getUsername).toList());
        Assert.assertEquals(List.of("member-b"),
                memberProvider.getRoleMembersStream(providerOrganization, providerRole, "member-b", 0, 1).map(UserModel::getUsername).toList());
        Assert.assertEquals(List.of("member-a", "member-b"),
                memberProvider.getRoleMembersStream(providerOrganization, providerRole, null, null, null).map(UserModel::getUsername).toList());
        Assert.assertEquals(List.of("member-a", "member-b"),
                memberProvider.getRoleMembersStream(providerOrganization, providerRole, null, 0, -1).map(UserModel::getUsername).toList());

        RoleProvider unsupportedProvider = new TestRoleProvider();

        Assert.assertThrows(UnsupportedOperationException.class, () -> unsupportedProvider.addOrganizationRole(providerOrganization, "id", "member"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> unsupportedProvider.getOrganizationRole(providerOrganization, "member"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> unsupportedProvider.getRoleById(providerOrganization, "id"));
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> unsupportedProvider.searchForOrganizationRolesStream(providerOrganization, "member", 0, 10));
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> unsupportedProvider.getOrganizationRolesStream(providerOrganization, 0, 10));
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> unsupportedProvider.getOrganizationRolesCount(providerOrganization, "member"));
        Assert.assertThrows(UnsupportedOperationException.class, () -> unsupportedProvider.removeRoles(providerOrganization));
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

    private static OrganizationProvider organizationProvider(List<UserModel> members) {
        return (OrganizationProvider) Proxy.newProxyInstance(OrganizationProvider.class.getClassLoader(), new Class<?>[] { OrganizationProvider.class },
                (proxy, method, args) -> {
                    if ("getMembersStream".equals(method.getName()) && method.getParameterTypes()[1] == String.class) {
                        String search = (String) args[1];
                        return members.stream().filter(user -> search == null || user.getUsername().contains(search));
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static UserModel user(String username, RoleModel... roles) {
        Set<RoleModel> roleSet = Set.of(roles);
        return (UserModel) Proxy.newProxyInstance(UserModel.class.getClassLoader(), new Class<?>[] { UserModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId", "getUsername" -> username;
                    case "hasRole" -> roleSet.contains(args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
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

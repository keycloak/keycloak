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
package org.keycloak.exportimport.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.exportimport.ExportOptions;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ParConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyManager;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class OrganizationExportImportUtilsTest {

    @BeforeClass
    public static void configureProfile() {
        Profile.configure();
    }

    @Test
    public void exportOrganizationRoleOwnsOrganizationComposites() {
        RealmModel realm = realm("realm-1");
        ClientModel client = client("client-1", "client-a");
        OrganizationModel organization = organization("org-1", "acme");
        RoleModel parent = organization.addRole("parent-id", "parent");
        RoleModel organizationRole = organization.addRole("org-role-id", "organization-child");
        RoleModel realmRole = role("realm-role-id", "realm-child", realm);
        RoleModel clientRole = role("client-role-id", "client-child", client);

        parent.addCompositeRole(organizationRole);
        parent.addCompositeRole(realmRole);
        parent.addCompositeRole(clientRole);
        RoleModel organizationOnlyParent = organization.addRole("organization-only-parent-id", "organization-only-parent");
        organizationOnlyParent.addCompositeRole(organizationRole);

        RoleRepresentation genericRepresentation = ExportUtils.exportRole(parent);
        RoleRepresentation genericOrganizationOnlyRepresentation = ExportUtils.exportRole(organizationOnlyParent);
        OrganizationRepresentation organizationRepresentation = new OrganizationRepresentation();
        OrganizationExportImportUtils.exportOrganizationRoles(organization, organizationRepresentation);
        RoleRepresentation organizationRoleRepresentation = organizationRepresentation.getRoles().stream()
                .filter(role -> "parent".equals(role.getName()))
                .findFirst()
                .orElseThrow();
        RoleRepresentation organizationOnlyRoleRepresentation = organizationRepresentation.getRoles().stream()
                .filter(role -> "organization-only-parent".equals(role.getName()))
                .findFirst()
                .orElseThrow();

        assertNull(genericRepresentation.getComposites().getOrganization());
        assertNull(genericOrganizationOnlyRepresentation.getComposites());
        assertThat(genericRepresentation.getComposites().getRealm(), contains("realm-child"));
        assertThat(genericRepresentation.getComposites().getClient().get("client-a"), contains("client-child"));
        assertThat(organizationRoleRepresentation.getComposites().getOrganization(), contains("organization-child"));
        assertThat(organizationRoleRepresentation.getComposites().getRealm(), contains("realm-child"));
        assertThat(organizationRoleRepresentation.getComposites().getClient().get("client-a"), contains("client-child"));
        assertThat(organizationOnlyRoleRepresentation.getComposites().getOrganization(), contains("organization-child"));
    }

    @Test
    public void exportOrganizationMemberRoleMappingsReturnsOnlySameOrganizationRoles() {
        OrganizationModel organization = organization("org-1", "acme");
        OrganizationModel other = organization("org-2", "other");
        RealmModel realm = realm("realm-1");
        ClientModel client = client("client-1", "client-a");
        UserModel user = user("user-1", "member");

        user.grantRole(organization.getDefaultRole());
        user.grantRole(role("role-2", "same-b", organization));
        user.grantRole(role("role-1", "same-a", organization));
        user.grantRole(role("role-3", "other", other));
        user.grantRole(role("role-4", "realm", realm));
        user.grantRole(role("role-5", "client", client));

        assertThat(OrganizationExportImportUtils.exportOrganizationMemberRoleMappings(organization, user), contains("same-a", "same-b"));
    }

    @Test
    public void exportOrganizationRolesIncludesRolesAndDefaultRole() {
        OrganizationModel organization = organization("org-1", "acme");
        organization.addRole("admin");
        OrganizationRepresentation representation = new OrganizationRepresentation();

        OrganizationExportImportUtils.exportOrganizationRoles(organization, representation);

        assertThat(representation.getRoles().stream().map(RoleRepresentation::getName).toList(),
                containsInAnyOrder("default-roles-acme", "admin"));
        assertThat(representation.getDefaultRole().getName(), is("default-roles-acme"));
    }

    @Test
    public void exportOrganizationsIncludesRolesMembersIdentityProvidersAndGroups() {
        OrganizationModel organization = organization("org-1", "acme");
        RoleModel admin = organization.addRole("admin");
        UserModel managedMember = user("user-1", "alice");
        UserModel unmanagedMember = user("user-2", "bob");
        unmanagedMember.grantRole(admin);
        GroupModel group = group("group-1", "engineering");
        IdentityProviderModel identityProvider = new IdentityProviderModel();
        identityProvider.setAlias("github");
        OrganizationProvider provider = organizationProvider(organization);
        OrganizationProviderHandler providerHandler = organizationProviderHandler(provider);
        providerHandler.members.add(managedMember);
        providerHandler.members.add(unmanagedMember);
        providerHandler.managedMembers.add(managedMember);
        providerHandler.memberGroups.put(unmanagedMember, List.of(group));
        providerHandler.identityProviders.add(identityProvider);
        RealmRepresentation realm = new RealmRepresentation();

        OrganizationExportImportUtils.exportOrganizations(session(provider), realm);

        OrganizationRepresentation exported = realm.getOrganizations().get(0);
        assertThat(exported.getAlias(), is("acme"));
        assertThat(exported.getRoles().stream().map(RoleRepresentation::getName).toList(),
                containsInAnyOrder("default-roles-acme", "admin"));
        assertThat(exported.getDefaultRole().getName(), is("default-roles-acme"));
        assertThat(exported.getIdentityProviders().stream().map(identity -> identity.getAlias()).toList(), contains("github"));
        assertThat(exported.getMembers(), hasSize(2));
        assertThat(exported.getMembers().get(0).getUsername(), is("alice"));
        assertThat(exported.getMembers().get(0).getMembershipType(), is(MembershipType.MANAGED));
        assertNull(exported.getMembers().get(0).getOrganizationRoles());
        assertThat(exported.getMembers().get(1).getUsername(), is("bob"));
        assertThat(exported.getMembers().get(1).getMembershipType(), is(MembershipType.UNMANAGED));
        assertThat(exported.getMembers().get(1).getOrganizationRoles(), contains("admin"));
        assertThat(exported.getMembers().get(1).getGroups(), contains("group-1"));
    }

    @Test
    public void exportRealmDelegatesFullOrganizationExport() {
        RealmModel realm = realm("realm-1");
        OrganizationModel organization = organization("org-1", "acme");
        RealmRepresentation representation = ExportUtils.exportRealm(session(organizationProvider(organization), userProvider()),
                realm, new ExportOptions(false, false, false, false, false), false);

        assertThat(representation.getOrganizations(), hasSize(1));
        assertThat(representation.getOrganizations().get(0).getAlias(), is("acme"));
    }

    @Test
    public void exportUserRoleMappingsSkipsOrganizationRoles() {
        RealmModel realm = realm("realm-1");
        ClientModel client = client("client-1", "client-a");
        OrganizationModel organization = organization("org-1", "acme");
        UserRepresentation representation = new UserRepresentation();

        ExportUtils.exportUserRoleMappings(representation, Set.of(
                role("realm-role-id", "realm-role", realm),
                role("client-role-id", "client-role", client),
                role("org-role-id", "organization-role", organization)));

        assertThat(representation.getRealmRoles(), contains("realm-role"));
        assertThat(representation.getClientRoles().get("client-a"), contains("client-role"));
    }

    @Test
    public void exportUserDelegatesRoleMappingsSkippingOrganizationRoles() {
        RealmModel realm = realm("realm-1");
        ClientModel client = client("client-1", "client-a");
        OrganizationModel organization = organization("org-1", "acme");
        UserModel user = user("user-1", "alice");
        user.grantRole(role("realm-role-id", "realm-role", realm));
        user.grantRole(role("client-role-id", "client-role", client));
        user.grantRole(role("org-role-id", "organization-role", organization));

        UserRepresentation representation = ExportUtils.exportUser(session(null, userProvider(user)), realm, user,
                new ExportOptions(true, false, true, false, false), false);

        assertThat(representation.getRealmRoles(), contains("realm-role"));
        assertThat(representation.getClientRoles().get("client-a"), contains("client-role"));
    }

    @Test
    public void exportFederatedUserDelegatesRoleMappingsSkippingOrganizationRoles() {
        RealmModel realm = realm("realm-1");
        ClientModel client = client("client-1", "client-a");
        OrganizationModel organization = organization("org-1", "acme");
        Set<RoleModel> roles = Set.of(
                role("realm-role-id", "realm-role", realm),
                role("client-role-id", "client-role", client),
                role("org-role-id", "organization-role", organization));

        UserRepresentation representation = ExportUtils.exportFederatedUser(session(null, userProvider(), userFederatedStorage(roles)),
                realm, "federated-user", new ExportOptions(true, false, true, false, false));

        assertThat(representation.getRealmRoles(), contains("realm-role"));
        assertThat(representation.getClientRoles().get("client-a"), contains("client-role"));
    }

    @Test
    public void importOrganizationRolesReusesDefaultRoleAndImportsComposites() {
        RealmModel realm = realm("realm-1");
        ClientModel client = client("client-1", "client-a");
        RoleModel realmRole = role("realm-role-id", "realm-child", realm);
        RoleModel clientRole = role("client-role-id", "client-child", client);
        realmHandler(realm).rolesByName.put(realmRole.getName(), realmRole);
        realmHandler(realm).clientsByClientId.put(client.getClientId(), client);
        clientHandler(client).rolesByName.put(clientRole.getName(), clientRole);

        OrganizationModel organization = organization("org-1", "acme");
        RoleModel generatedDefault = organization.getDefaultRole();
        generatedDefault.setAttribute("stale", List.of("value"));

        RoleRepresentation viewer = roleRepresentation("viewer-id", "viewer", "Viewer");
        RoleRepresentation member = roleRepresentation("source-default-id", "member", "Member");
        member.setAttributes(Map.of("tier", List.of("base")));
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        composites.setOrganization(Set.of("viewer"));
        composites.setRealm(Set.of("realm-child"));
        composites.setClient(Map.of("client-a", List.of("client-child")));
        member.setComposites(composites);

        OrganizationRepresentation representation = new OrganizationRepresentation();
        representation.setDefaultRole(member);
        representation.setRoles(Arrays.asList(member, viewer));

        OrganizationExportImportUtils.importOrganizationRoles(session(), realm, organization, representation);

        assertThat(organization.getDefaultRole().getName(), is("member"));
        assertThat(organization.getRolesStream().map(RoleModel::getName).toList(), containsInAnyOrder("member", "viewer"));
        assertThat(organization.getRolesStream().toList(), hasSize(2));
        assertThat(organization.getDefaultRole().getAttributes().containsKey("stale"), is(false));
        assertThat(organization.getDefaultRole().getAttributes().get("tier"), contains("base"));
        assertThat(organization.getDefaultRole().getCompositesStream().map(RoleModel::getName).toList(),
                containsInAnyOrder("viewer", "realm-child", "client-child"));
    }

    @Test
    public void importOrganizationRolesPreservesGeneratedRoleWhenCustomRoleIsDefault() {
        OrganizationModel organization = organization("org-1", "acme");
        RoleRepresentation generated = roleRepresentation("default-id-acme", "default-roles-acme", "Generated default");
        RoleRepresentation customDefault = roleRepresentation("custom-default-id", "member", "Member");
        OrganizationRepresentation representation = new OrganizationRepresentation();
        representation.setDefaultRole(customDefault);
        representation.setRoles(Arrays.asList(generated, customDefault));

        OrganizationExportImportUtils.importOrganizationRoles(session(), realm("realm-1"), organization, representation);

        assertThat(organization.getDefaultRole().getName(), is("member"));
        assertThat(organization.getRolesStream().map(RoleModel::getName).toList(), containsInAnyOrder("default-roles-acme", "member"));
        assertThat(organization.getRolesStream().toList(), hasSize(2));
    }

    @Test
    public void importOrganizationRolesSupportsDefaultRoleOnlyPayloads() {
        OrganizationModel organization = organization("org-1", "acme");
        RoleRepresentation viewer = roleRepresentation("viewer-id", "viewer", "Viewer");
        RoleRepresentation defaultRole = roleRepresentation(null, "member", "Member");
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        composites.setOrganization(Set.of("viewer"));
        defaultRole.setComposites(composites);
        OrganizationRepresentation representation = new OrganizationRepresentation();
        representation.setDefaultRole(defaultRole);
        representation.setRoles(List.of(viewer));

        OrganizationExportImportUtils.importOrganizationRoles(session(), realm("realm-1"), organization, representation);

        assertThat(organization.getDefaultRole().getName(), is("member"));
        assertThat(organization.getRolesStream().toList(), hasSize(2));
        assertThat(organization.getDefaultRole().getCompositesStream().map(RoleModel::getName).toList(), contains("viewer"));
    }

    @Test
    public void importOrganizationRolesSupportsRolesWithoutAttributes() {
        OrganizationModel organization = organization("org-1", "acme");
        RoleRepresentation role = new RoleRepresentation();
        role.setName("no-attrs");
        OrganizationRepresentation representation = new OrganizationRepresentation();
        representation.setRoles(List.of(role));

        OrganizationExportImportUtils.importOrganizationRoles(session(), realm("realm-1"), organization, representation);

        assertThat(organization.getRole("no-attrs").getName(), is("no-attrs"));
    }

    @Test
    public void importOrganizationRolesPreservesRepresentationIds() {
        OrganizationModel organization = organization("org-1", "acme");
        RoleRepresentation role = roleRepresentation("stable-role-id", "admin", "Admin");
        OrganizationRepresentation representation = new OrganizationRepresentation();
        representation.setRoles(List.of(role));
        KeycloakSession session = session();

        OrganizationExportImportUtils.importOrganizationRoles(session, realm("realm-1"), organization, representation);

        RoleModel imported = organization.getRole("admin");
        assertThat(imported.getId(), is("stable-role-id"));
        assertThat(session.roles().getRoleById(organization, "stable-role-id"), is(imported));
    }

    @Test
    public void importOrganizationRolesRejectsInvalidReferences() {
        OrganizationRepresentation unnamedRole = new OrganizationRepresentation();
        unnamedRole.setRoles(List.of(new RoleRepresentation()));

        assertThrows(ModelException.class,
                () -> OrganizationExportImportUtils.importOrganizationRoles(session(), realm("realm-1"), organization("org-1", "acme"), unnamedRole));

        OrganizationRepresentation missingComposite = new OrganizationRepresentation();
        RoleRepresentation role = roleRepresentation("role-id", "member", "Member");
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        composites.setOrganization(Set.of("missing"));
        role.setComposites(composites);
        missingComposite.setRoles(List.of(role));

        assertThrows(ModelException.class,
                () -> OrganizationExportImportUtils.importOrganizationRoles(session(), realm("realm-1"), organization("org-1", "acme"), missingComposite));
    }

    @Test
    public void importOrganizationRolesRejectsInvalidCompositeReferences() {
        assertThrows(ModelException.class, () -> importRoleWithComposites(realm("realm-1"), compositesWithRealmRole("missing")));
        assertThrows(ModelException.class, () -> importRoleWithComposites(realm("realm-1"), compositesWithClientRole("missing-client", "role")));

        RealmModel realm = realm("realm-1");
        ClientModel client = client("client-1", "client-a");
        realmHandler(realm).clientsByClientId.put(client.getClientId(), client);
        assertThrows(ModelException.class, () -> importRoleWithComposites(realm, compositesWithClientRole("client-a", "missing")));

        Set<String> nullRoleName = new HashSet<>();
        nullRoleName.add(null);
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        composites.setRealm(nullRoleName);
        assertThrows(ModelException.class, () -> importRoleWithComposites(realm("realm-1"), composites));
    }

    @Test
    public void resolveOrganizationRoleRejectsMissingRole() throws Exception {
        Method method = OrganizationExportImportUtils.class.getDeclaredMethod("resolveOrganizationRole",
                KeycloakSession.class, OrganizationModel.class, RoleRepresentation.class);
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, session(), organization("org-1", "acme"), roleRepresentation("missing-id", "missing", "Missing")));

        assertThat(exception.getCause(), instanceOf(ModelException.class));
    }

    @Test
    public void roleReferenceMatchingRejectsNullReferences() throws Exception {
        Method representationMatcher = OrganizationExportImportUtils.class.getDeclaredMethod("matchesRoleReference",
                RoleRepresentation.class, RoleRepresentation.class);
        representationMatcher.setAccessible(true);
        Method modelMatcher = OrganizationExportImportUtils.class.getDeclaredMethod("matchesRoleReference",
                RoleModel.class, RoleRepresentation.class);
        modelMatcher.setAccessible(true);

        assertThat((Boolean) representationMatcher.invoke(null, new RoleRepresentation(), null), is(false));
        assertThat((Boolean) modelMatcher.invoke(null, null, new RoleRepresentation()), is(false));
    }

    @Test
    public void importOrganizationMemberRoleMappingsRequiresMembersAndExistingRoles() {
        OrganizationModel organization = organization("org-1", "acme");
        RoleModel role = organization.addRole("admin");
        MemberRepresentation member = new MemberRepresentation();
        member.setUsername("member");
        member.setOrganizationRoles(List.of("admin"));
        MemberRepresentation noRoles = new MemberRepresentation();
        UserModel user = user("user-1", "member");

        OrganizationExportImportUtils.importOrganizationMemberRoleMappings(organization, noRoles, null);
        assertThrows(ModelException.class, () -> OrganizationExportImportUtils.importOrganizationMemberRoleMappings(organization, member, null));

        MemberRepresentation missingRole = new MemberRepresentation();
        missingRole.setOrganizationRoles(List.of("missing"));
        ModelException exception = assertThrows(ModelException.class,
                () -> OrganizationExportImportUtils.importOrganizationMemberRoleMappings(organization, missingRole, user));
        assertThat(exception.getMessage(), containsString("organization role mapping"));
        assertThat(exception.getMessage(), containsString("missing"));

        organizationHandler(organization).members.add(user);
        OrganizationExportImportUtils.importOrganizationMemberRoleMappings(organization, member, user);

        assertThat(user.hasDirectRole(role), is(true));
    }

    private static RoleRepresentation roleRepresentation(String id, String name, String description) {
        RoleRepresentation representation = new RoleRepresentation();
        representation.setId(id);
        representation.setName(name);
        representation.setDescription(description);
        representation.setAttributes(new HashMap<>());
        return representation;
    }

    private static void importRoleWithComposites(RealmModel realm, RoleRepresentation.Composites composites) {
        OrganizationModel organization = organization("org-1", "acme");
        RoleRepresentation role = roleRepresentation("role-id", "member", "Member");
        role.setComposites(composites);
        OrganizationRepresentation representation = new OrganizationRepresentation();
        representation.setRoles(List.of(role));

        OrganizationExportImportUtils.importOrganizationRoles(session(), realm, organization, representation);
    }

    private static RoleRepresentation.Composites compositesWithRealmRole(String roleName) {
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        composites.setRealm(Set.of(roleName));
        return composites;
    }

    private static RoleRepresentation.Composites compositesWithClientRole(String clientId, String roleName) {
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        composites.setClient(Map.of(clientId, List.of(roleName)));
        return composites;
    }

    private static KeycloakSession session() {
        return session(null, null);
    }

    private static KeycloakSession session(OrganizationProvider organizationProvider) {
        return session(organizationProvider, null);
    }

    private static KeycloakSession session(OrganizationProvider organizationProvider, UserProvider userProvider) {
        return session(organizationProvider, userProvider, null);
    }

    private static KeycloakSession session(OrganizationProvider organizationProvider, UserProvider userProvider,
            UserFederatedStorageProvider userFederatedStorageProvider) {
        RoleProvider roles = proxy(RoleProvider.class, (proxy, method, args) -> {
            if ("getRoleById".equals(method.getName()) && args[0] instanceof OrganizationModel) {
                return organizationHandler((OrganizationModel) args[0]).rolesById.get(args[1]);
            }
            return defaultValue(method.getReturnType());
        });
        return proxy(KeycloakSession.class, (proxy, method, args) -> {
            if ("roles".equals(method.getName())) {
                return roles;
            }
            if ("users".equals(method.getName())) {
                return userProvider;
            }
            if ("identityProviders".equals(method.getName())) {
                return identityProviderStorage();
            }
            if ("clientPolicy".equals(method.getName())) {
                return clientPolicyManager();
            }
            if ("getAllProviders".equals(method.getName())) {
                return Set.of();
            }
            if ("getProvider".equals(method.getName()) && OrganizationProvider.class.equals(args[0])) {
                return organizationProvider;
            }
            if ("getProvider".equals(method.getName()) && UserFederatedStorageProvider.class.equals(args[0])) {
                return userFederatedStorageProvider;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm(String id) {
        RealmHandler handler = new RealmHandler(id);
        RealmModel realm = proxy(RealmModel.class, handler);
        handler.realm = realm;
        handler.defaultRole = role("default-role-id-" + id, "default-roles-" + id, realm);
        return realm;
    }

    private static ClientModel client(String id, String clientId) {
        ClientHandler handler = new ClientHandler(id, clientId);
        return proxy(ClientModel.class, handler);
    }

    private static OrganizationModel organization(String id, String alias) {
        OrganizationHandler handler = new OrganizationHandler(id, alias);
        OrganizationModel organization = proxy(OrganizationModel.class, handler);
        handler.organization = organization;
        handler.addRoleInternal("default-id-" + alias, "default-roles-" + alias);
        handler.defaultRole = handler.rolesByName.get("default-roles-" + alias);
        return organization;
    }

    private static UserModel user(String id, String username) {
        return proxy(UserModel.class, new UserHandler(id, username));
    }

    private static GroupModel group(String id, String name) {
        return proxy(GroupModel.class, new GroupHandler(id, name));
    }

    private static OrganizationProvider organizationProvider(OrganizationModel organization) {
        return proxy(OrganizationProvider.class, new OrganizationProviderHandler(organization));
    }

    private static UserProvider userProvider(UserModel... users) {
        Map<String, UserModel> usersByUsername = Arrays.stream(users).collect(Collectors.toMap(UserModel::getUsername, user -> user));
        return proxy(UserProvider.class, (proxy, method, args) -> {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            if ("getUserByUsername".equals(method.getName())) {
                return usersByUsername.get(args[1]);
            }
            switch (method.getName()) {
                case "getFederatedIdentitiesStream":
                case "getConsentsStream":
                case "getVerifiableCredentialsByUser":
                case "getIssuedVerifiableCredentialsStreamByUser":
                    return Stream.empty();
                case "getNotBeforeOfUser":
                    return 0;
                default:
                    return defaultValue(method.getReturnType());
            }
        });
    }

    private static UserFederatedStorageProvider userFederatedStorage(Set<RoleModel> roles) {
        return proxy(UserFederatedStorageProvider.class, (proxy, method, args) -> {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            switch (method.getName()) {
                case "getAttributes":
                    return new MultivaluedHashMap<String, String>();
                case "getRequiredActionsStream":
                case "getFederatedIdentitiesStream":
                case "getStoredCredentialsStream":
                case "getGroupsStream":
                    return Stream.empty();
                case "getRoleMappingsStream":
                    return roles.stream();
                case "getNotBeforeOfUser":
                    return 0;
                default:
                    return defaultValue(method.getReturnType());
            }
        });
    }

    private static SubjectCredentialManager credentialManager() {
        return proxy(SubjectCredentialManager.class, (proxy, method, args) -> {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            switch (method.getName()) {
                case "getDisableableCredentialTypesStream":
                case "getStoredCredentialsStream":
                    return Stream.empty();
                case "isConfiguredFor":
                    return false;
                default:
                    return defaultValue(method.getReturnType());
            }
        });
    }

    private static IdentityProviderStorageProvider identityProviderStorage() {
        return proxy(IdentityProviderStorageProvider.class, (proxy, method, args) -> {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            if ("getAllStream".equals(method.getName()) || "getMappersStream".equals(method.getName())) {
                return Stream.empty();
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static ClientPolicyManager clientPolicyManager() {
        return proxy(ClientPolicyManager.class, (proxy, method, args) -> {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            return defaultValue(method.getReturnType());
        });
    }

    private static RoleModel role(String id, String name, RoleContainerModel container) {
        return role(new RoleHandler(id, name, container));
    }

    private static RoleModel role(RoleHandler handler) {
        RoleModel role = proxy(RoleModel.class, handler);
        handler.role = role;
        return role;
    }

    private static OrganizationHandler organizationHandler(OrganizationModel organization) {
        return (OrganizationHandler) Proxy.getInvocationHandler(organization);
    }

    private static RealmHandler realmHandler(RealmModel realm) {
        return (RealmHandler) Proxy.getInvocationHandler(realm);
    }

    private static ClientHandler clientHandler(ClientModel client) {
        return (ClientHandler) Proxy.getInvocationHandler(client);
    }

    private static OrganizationProviderHandler organizationProviderHandler(OrganizationProvider provider) {
        return (OrganizationProviderHandler) Proxy.getInvocationHandler(provider);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        if (!Object.class.equals(method.getDeclaringClass())) {
            return Unhandled.INSTANCE;
        }
        if ("equals".equals(method.getName())) {
            return proxy == args[0];
        }
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(proxy);
        }
        return proxy.getClass().getInterfaces()[0].getSimpleName();
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
        if (byte.class.equals(type)) {
            return (byte) 0;
        }
        if (short.class.equals(type)) {
            return (short) 0;
        }
        if (long.class.equals(type)) {
            return 0L;
        }
        if (float.class.equals(type)) {
            return 0F;
        }
        if (double.class.equals(type)) {
            return 0D;
        }
        return 0;
    }

    private enum Unhandled {
        INSTANCE
    }

    private static final class RoleHandler implements InvocationHandler {
        private final String id;
        private final RoleContainerModel container;
        private String name;
        private String description;
        private RoleModel role;
        private final Set<RoleModel> composites = new LinkedHashSet<>();
        private final Map<String, List<String>> attributes = new HashMap<>();
        private RenameListener renameListener;

        private RoleHandler(String id, String name, RoleContainerModel container) {
            this.id = id;
            this.name = name;
            this.container = container;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            switch (method.getName()) {
                case "getId":
                    return id;
                case "getName":
                    return name;
                case "setName":
                    String previousName = name;
                    name = (String) args[0];
                    if (renameListener != null) {
                        renameListener.renamed(previousName, name, role);
                    }
                    return null;
                case "getDescription":
                    return description;
                case "setDescription":
                    description = (String) args[0];
                    return null;
                case "getContainer":
                    return container;
                case "getContainerId":
                    return container.getId();
                case "isClientRole":
                    return container instanceof ClientModel;
                case "isOrganizationRole":
                    return container instanceof OrganizationModel;
                case "isRealmRole":
                    return container instanceof RealmModel;
                case "getType":
                    if (container instanceof ClientModel) return RoleModel.Type.CLIENT;
                    if (container instanceof OrganizationModel) return RoleModel.Type.ORGANIZATION;
                    return RoleModel.Type.REALM;
                case "isComposite":
                    return !composites.isEmpty();
                case "addCompositeRole":
                    composites.add((RoleModel) args[0]);
                    return null;
                case "removeCompositeRole":
                    composites.remove(args[0]);
                    return null;
                case "getCompositesStream":
                    return composites.stream();
                case "getAttributes":
                    return attributes;
                case "setAttribute":
                    attributes.put((String) args[0], new ArrayList<>((Collection<String>) args[1]));
                    return null;
                case "setSingleAttribute":
                    attributes.put((String) args[0], List.of((String) args[1]));
                    return null;
                case "removeAttribute":
                    attributes.remove(args[0]);
                    return null;
                case "getAttributeStream":
                    return attributes.getOrDefault(args[0], List.of()).stream();
                case "hasRole":
                    return role == args[0] || composites.contains(args[0]);
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    private static final class OrganizationHandler implements InvocationHandler {
        private final String id;
        private String name;
        private String alias;
        private boolean enabled = true;
        private String description;
        private String redirectUrl;
        private Map<String, List<String>> attributes = Map.of();
        private OrganizationModel organization;
        private RoleModel defaultRole;
        private final Map<String, RoleModel> rolesByName = new LinkedHashMap<>();
        private final Map<String, RoleModel> rolesById = new LinkedHashMap<>();
        private final Set<UserModel> members = new HashSet<>();

        private OrganizationHandler(String id, String alias) {
            this.id = id;
            this.name = alias;
            this.alias = alias;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            switch (method.getName()) {
                case "getId":
                    return id;
                case "getAlias":
                    return alias;
                case "setAlias":
                    alias = (String) args[0];
                    return null;
                case "getName":
                    return name;
                case "setName":
                    name = (String) args[0];
                    return null;
                case "isEnabled":
                    return enabled;
                case "setEnabled":
                    enabled = (boolean) args[0];
                    return null;
                case "getDescription":
                    return description;
                case "setDescription":
                    description = (String) args[0];
                    return null;
                case "getRedirectUrl":
                    return redirectUrl;
                case "setRedirectUrl":
                    redirectUrl = (String) args[0];
                    return null;
                case "getAttributes":
                    return attributes;
                case "setAttributes":
                    attributes = (Map<String, List<String>>) args[0];
                    return null;
                case "getDomains":
                    return Stream.empty();
                case "setDomains":
                    return null;
                case "getDefaultRole":
                    return defaultRole;
                case "setDefaultRole":
                    defaultRole = (RoleModel) args[0];
                    return null;
                case "getRole":
                    return rolesByName.get(args[0]);
                case "addRole":
                    if (args.length == 1) {
                        return addRoleInternal("role-id-" + args[0], (String) args[0]);
                    }
                    return addRoleInternal((String) args[0], (String) args[1]);
                case "getRolesStream":
                    return rolesByName.values().stream();
                case "searchForRolesStream":
                    String search = (String) args[0];
                    return rolesByName.values().stream().filter(role -> role.getName().contains(search));
                case "isMember":
                    return members.contains(args[0]);
                case "hasRole":
                    return rolesByName.containsValue(args[0]);
                default:
                    return defaultValue(method.getReturnType());
            }
        }

        private RoleModel addRoleInternal(String id, String name) {
            RoleHandler handler = new RoleHandler(id, name, organization);
            RoleModel role = role(handler);
            handler.renameListener = (previousName, newName, renamedRole) -> {
                rolesByName.remove(previousName);
                rolesByName.put(newName, renamedRole);
            };
            rolesByName.put(name, role);
            rolesById.put(id, role);
            return role;
        }
    }

    private static final class OrganizationProviderHandler implements InvocationHandler {
        private final OrganizationModel organization;
        private final List<UserModel> members = new ArrayList<>();
        private final Set<UserModel> managedMembers = new HashSet<>();
        private final Map<UserModel, List<GroupModel>> memberGroups = new HashMap<>();
        private final List<IdentityProviderModel> identityProviders = new ArrayList<>();
        private final List<GroupModel> groups = new ArrayList<>();

        private OrganizationProviderHandler(OrganizationModel organization) {
            this.organization = organization;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            switch (method.getName()) {
                case "create":
                    return organization;
                case "getAllStream":
                    return Stream.of(organization);
                case "getMembersStream":
                    return members.stream();
                case "addManagedMember":
                    managedMembers.add((UserModel) args[1]);
                    organizationHandler((OrganizationModel) args[0]).members.add((UserModel) args[1]);
                    return true;
                case "addMember":
                    organizationHandler((OrganizationModel) args[0]).members.add((UserModel) args[1]);
                    return true;
                case "isManagedMember":
                    return managedMembers.contains(args[1]);
                case "getOrganizationGroupsByMember":
                    return memberGroups.getOrDefault(args[1], List.of()).stream();
                case "getIdentityProviders":
                    return identityProviders.stream();
                case "getTopLevelGroups":
                    return groups.stream();
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    private static final class RealmHandler implements InvocationHandler {
        private final String id;
        private RealmModel realm;
        private RoleModel defaultRole;
        private final Map<String, RoleModel> rolesByName = new HashMap<>();
        private final Map<String, ClientModel> clientsByClientId = new HashMap<>();

        private RealmHandler(String id) {
            this.id = id;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            switch (method.getName()) {
                case "getId":
                    return id;
                case "getName":
                    return id;
                case "getDisplayName":
                case "getDisplayNameHtml":
                case "getDefaultSignatureAlgorithm":
                case "getAccountTheme":
                case "getLoginTheme":
                case "getAdminTheme":
                case "getEmailTheme":
                case "getDefaultLocale":
                case "getAttribute":
                    return args != null && args.length == 2 ? args[1] : null;
                case "isEnabled":
                    return true;
                case "getSslRequired":
                    return SslRequired.EXTERNAL;
                case "getBruteForceStrategy":
                    return RealmRepresentation.BruteForceStrategy.LINEAR;
                case "getOAuth2DeviceConfig":
                    return new OAuth2DeviceConfig(realm);
                case "getOTPPolicy":
                    return OTPPolicy.DEFAULT_POLICY;
                case "getWebAuthnPolicy":
                case "getWebAuthnPolicyPasswordless":
                    return WebAuthnPolicy.DEFAULT_POLICY;
                case "getCibaPolicy":
                    return CibaConfig.fromModel(realm);
                case "getParPolicy":
                    return ParConfig.fromModel(realm);
                case "getSmtpConfig":
                case "getBrowserSecurityHeaders":
                case "getAttributes":
                case "getRealmLocalizationTexts":
                    return Map.of();
                case "getDefaultRole":
                    return defaultRole;
                case "getRolesStream":
                    return rolesByName.values().stream();
                case "getRole":
                    return rolesByName.get(args[0]);
                case "getClientByClientId":
                    return clientsByClientId.get(args[0]);
                default:
                    if (method.getName().endsWith("Stream")) {
                        return Stream.empty();
                    }
                    return defaultValue(method.getReturnType());
            }
        }
    }

    private static final class ClientHandler implements InvocationHandler {
        private final String id;
        private final String clientId;
        private final Map<String, RoleModel> rolesByName = new HashMap<>();

        private ClientHandler(String id, String clientId) {
            this.id = id;
            this.clientId = clientId;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            switch (method.getName()) {
                case "getId":
                    return id;
                case "getClientId":
                    return clientId;
                case "getRole":
                    return rolesByName.get(args[0]);
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    private static final class GroupHandler implements InvocationHandler {
        private final String id;
        private final String name;

        private GroupHandler(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            switch (method.getName()) {
                case "getId":
                    return id;
                case "getName":
                    return name;
                case "getParentId":
                case "getParent":
                case "getDescription":
                    return null;
                case "getRoleMappingsStream":
                case "getSubGroupsStream":
                    return Stream.empty();
                case "getAttributes":
                    return Map.of();
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    private static final class UserHandler implements InvocationHandler {
        private final String id;
        private final String username;
        private final Set<RoleModel> roles = new LinkedHashSet<>();

        private UserHandler(String id, String username) {
            this.id = id;
            this.username = username;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object objectMethod = objectMethod(proxy, method, args);
            if (objectMethod != Unhandled.INSTANCE) {
                return objectMethod;
            }

            switch (method.getName()) {
                case "getId":
                    return id;
                case "getUsername":
                    return username;
                case "getCreatedTimestamp":
                case "getLastName":
                case "getFirstName":
                case "getEmail":
                case "getFederationLink":
                case "getServiceAccountClientLink":
                    return null;
                case "isEnabled":
                    return true;
                case "isEmailVerified":
                    return false;
                case "credentialManager":
                    return credentialManager();
                case "getRequiredActionsStream":
                case "getGroupsStream":
                    return Stream.empty();
                case "getAttributes":
                    return Map.of();
                case "grantRole":
                    roles.add((RoleModel) args[0]);
                    return null;
                case "hasDirectRole":
                    return roles.contains(args[0]);
                case "getRoleMappingsStream":
                    return roles.stream();
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    private interface RenameListener {
        void renamed(String previousName, String newName, RoleModel role);
    }
}

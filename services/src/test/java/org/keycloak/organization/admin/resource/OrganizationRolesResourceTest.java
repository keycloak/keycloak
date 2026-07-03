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
package org.keycloak.organization.admin.resource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.ProfileConfigResolver;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.OrganizationPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RolePermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OrganizationRolesResourceTest {

    @Before
    public void configureProfile() {
        Profile.reset();
        Profile.configure();
    }

    @After
    public void resetProfile() {
        Profile.reset();
    }

    @Test
    public void collectionCreatesListsCountsAndLocatesRolesById() {
        TestContext context = new TestContext();
        OrganizationRolesResource roles = new OrganizationRolesResource(context.session, context.organization, context.adminEvent, context.auth);

        RoleRepresentation representation = new RoleRepresentation();
        representation.setName("member");
        representation.setDescription("Member role");
        representation.setAttributes(Map.of("team", List.of("platform")));

        Response response = roles.createRole(representation);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(representation.getId());
        TestRole created = context.roles.get(representation.getId());
        assertEquals("Member role", created.description);
        assertEquals(List.of("platform"), created.attributes.get("team"));

        assertThrows(BadRequestException.class, () -> roles.createRole(null));
        assertStatus(Response.Status.CONFLICT, assertThrows(ErrorResponseException.class, () -> roles.createRole(representation)));

        assertEquals(List.of("member"), roles.getRoles("", null, null, true).map(RoleRepresentation::getName).toList());
        assertEquals(List.of("member"), roles.getRoles("mem", 0, 10, false).map(RoleRepresentation::getName).toList());
        assertEquals(1, roles.getRoleCount("mem"));

        context.defaultRole = created.model;
        assertEquals(created.id, roles.getDefaultRole().getRole().getId());
        assertEquals(created.id, roles.getRole(created.id).getRole().getId());
        assertThrows(NotFoundException.class, () -> roles.getRole("missing"));
        assertThrows(BadRequestException.class, () -> roles.getRole(" "));

        OrganizationResource organizationResource = new OrganizationResource(context.session, context.organization, context.adminEvent, context.auth);
        assertNotNull(organizationResource.roles());
    }

    @Test
    public void itemUpdatesDeletesAndRejectsDefaultRoleRemoval() {
        TestContext context = new TestContext();
        TestRole role = context.addOrganizationRole("role-1", "member");
        TestRole defaultRole = context.addOrganizationRole("role-2", "default-roles-acme");
        role.attributes.put("old", List.of("value"));
        context.defaultRole = defaultRole.model;
        OrganizationRoleResource resource = new OrganizationRoleResource(context.session, context.organization, role.model, context.adminEvent, context.auth);

        assertEquals(role.id, resource.getRole().getId());

        RoleRepresentation update = new RoleRepresentation();
        update.setName("renamed");
        update.setDescription("Renamed role");
        update.setAttributes(Map.of("new", List.of("value")));

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resource.updateRole(update).getStatus());
        assertEquals("renamed", role.name);
        assertEquals("Renamed role", role.description);
        assertFalse(role.attributes.containsKey("old"));
        assertEquals(List.of("value"), role.attributes.get("new"));
        assertEquals(1, context.providerEvents.size());
        RoleModel.RoleNameChangeEvent event = (RoleModel.RoleNameChangeEvent) context.providerEvents.get(0);
        assertSame(context.realm, event.getRealm());
        assertSame(role.model, event.getRole());
        assertSame(context.session, event.getKeycloakSession());
        assertEquals(RoleModel.Type.ORGANIZATION, event.getRole().getType());
        assertEquals(context.organization.getId(), event.getRole().getContainerId());
        assertSame(context.organization, event.getRole().getContainer());
        assertEquals("member", event.getPreviousName());
        assertEquals("renamed", event.getNewName());

        assertThrows(BadRequestException.class, () -> resource.updateRole(null));

        RoleRepresentation duplicate = new RoleRepresentation();
        duplicate.setName("duplicate");
        assertStatus(Response.Status.CONFLICT, assertThrows(ErrorResponseException.class, () -> resource.updateRole(duplicate)));

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resource.deleteRole().getStatus());
        assertFalse(context.roles.containsKey(role.id));

        OrganizationRoleResource defaultResource = new OrganizationRoleResource(context.session, context.organization, defaultRole.model, context.adminEvent, context.auth);
        assertStatus(Response.Status.BAD_REQUEST, assertThrows(ErrorResponseException.class, defaultResource::deleteRole));
    }

    @Test
    public void itemManagesCompositesAndRejectsCrossOrganizationComposites() {
        TestContext context = new TestContext();
        TestRole parent = context.addOrganizationRole("parent", "parent");
        TestRole sameOrganizationRole = context.addOrganizationRole("child", "child");
        TestRole otherOrganizationRole = context.addOrganizationRole("other-child", "other-child", context.otherOrganization);
        TestRole realmRole = context.addRealmRole("realm-role", "realm-role");
        ClientModel client = context.addClient("client-1", "client");
        TestRole clientRole = context.addClientRole("client-role", "client-role", client);
        OrganizationRoleResource resource = new OrganizationRoleResource(context.session, context.organization, parent.model, context.adminEvent, context.auth);

        resource.addComposites(List.of(role(repId(realmRole)), role(repId(clientRole)), role(repId(sameOrganizationRole))));
        resource.addComposites(List.of());

        assertEquals(Set.of("realm-role", "client-role", "child"), roleNames(parent.composites));
        assertEquals(List.of("realm-role", "client-role", "child"), resource.getRoleComposites(null, null, null).map(RoleRepresentation::getName).toList());
        assertEquals(List.of("realm-role"), resource.getRoleComposites("realm", 0, 1).map(RoleRepresentation::getName).toList());
        assertEquals(List.of("realm-role"), resource.getRealmRoleComposites().map(RoleRepresentation::getName).toList());
        assertEquals(List.of("client-role"), resource.getClientRoleComposites(client.getId()).map(RoleRepresentation::getName).toList());

        assertThrows(NotFoundException.class, () -> resource.getClientRoleComposites("missing-client"));
        assertThrows(BadRequestException.class, () -> resource.addComposites(null));
        assertThrows(NotFoundException.class, () -> resource.addComposites(Collections.singletonList(new RoleRepresentation())));
        RoleRepresentation missing = new RoleRepresentation();
        missing.setId("missing-role");
        assertThrows(NotFoundException.class, () -> resource.addComposites(List.of(missing)));
        assertThrows(BadRequestException.class, () -> resource.addComposites(List.of(role(repId(otherOrganizationRole)))));

        resource.deleteComposites(List.of(role(repId(sameOrganizationRole))));
        resource.deleteComposites(List.of());
        assertFalse(parent.composites.contains(sameOrganizationRole.model));
        assertThrows(BadRequestException.class, () -> resource.deleteComposites(null));
    }

    @Test
    public void itemListsAssignsAndRevokesUsers() {
        TestContext context = new TestContext();
        TestRole role = context.addOrganizationRole("role-1", "member");
        TestUser member = context.addUser("member-id", "member");
        TestUser outsider = context.addUser("outsider-id", "outsider");
        context.members.add(member.id);
        context.roleMembers.add(member.model);
        context.roleMembers.add(outsider.model);
        OrganizationRoleResource resource = new OrganizationRoleResource(context.session, context.organization, role.model, context.adminEvent, context.auth);

        assertEquals(List.of("member-id"), resource.getUsersInRole(true, null, null).map(UserRepresentation::getId).toList());
        resource.getUsersInRole(true, 1, 2).toList();
        assertEquals(Integer.valueOf(1), context.lastRoleMembersFirst);
        assertEquals(Integer.valueOf(2), context.lastRoleMembersMax);

        context.permissions.userView = false;
        assertTrue(resource.getUsersInRole(true, null, null).toList().isEmpty());
        context.adminPermissionsEnabled = true;
        withAdminFineGrainedAuthzV2(false, () -> assertTrue(resource.getUsersInRole(true, null, null).toList().isEmpty()));
        withAdminFineGrainedAuthzV2(true,
                () -> assertEquals(List.of("member-id"), resource.getUsersInRole(true, null, null).map(UserRepresentation::getId).toList()));
        context.adminPermissionsEnabled = false;
        context.permissions.userView = true;

        context.permissions.userQuery = false;
        assertThrows(ForbiddenException.class, () -> resource.getUsersInRole(true, null, null).toList());
        context.permissions.userQuery = true;

        resource.addUserRoleMappings(List.of(user(member.id)));
        assertEquals(1, member.grants);
        assertTrue(member.roleMappings.contains(role.model));

        resource.deleteUserRoleMappings(List.of(user(member.id)));
        assertEquals(1, member.revokes);
        assertFalse(member.roleMappings.contains(role.model));

        resource.addUserRoleMappings(List.of());
        assertThrows(BadRequestException.class, () -> resource.addUserRoleMappings(null));
        assertThrows(BadRequestException.class, () -> resource.addUserRoleMappings(Collections.singletonList(new UserRepresentation())));
        assertStatus(Response.Status.BAD_REQUEST, assertThrows(ErrorResponseException.class, () -> resource.addUserRoleMappings(List.of(user(outsider.id)))));
        assertThrows(NotFoundException.class, () -> resource.addUserRoleMappings(List.of(user("missing"))));
        context.permissions.userQuery = false;
        assertThrows(ForbiddenException.class, () -> resource.addUserRoleMappings(List.of(user("missing"))));
        context.permissions.userQuery = true;

        member.failIllegalStateOnGrant = true;
        assertStatus(Response.Status.INTERNAL_SERVER_ERROR, assertThrows(ErrorResponseException.class, () -> resource.addUserRoleMappings(List.of(user(member.id)))));
        member.failIllegalStateOnGrant = false;

        member.failReadOnlyOnDelete = true;
        assertStatus(Response.Status.BAD_REQUEST, assertThrows(ErrorResponseException.class, () -> resource.deleteUserRoleMappings(List.of(user(member.id)))));
    }

    @Test
    public void userRoleMappingBatchesValidateAllUsersBeforeGranting() {
        TestContext context = new TestContext();
        TestRole role = context.addOrganizationRole("role-1", "member");
        TestUser member = context.addUser("member-id", "member");
        TestUser outsider = context.addUser("outsider-id", "outsider");
        TestUser restricted = context.addUser("restricted-id", "restricted");
        context.members.add(member.id);
        context.members.add(restricted.id);
        OrganizationRoleResource resource = new OrganizationRoleResource(context.session, context.organization, role.model, context.adminEvent, context.auth);

        assertThrows(NotFoundException.class, () -> resource.addUserRoleMappings(List.of(user(member.id), user("missing"))));
        assertEquals(0, member.grants);
        assertFalse(member.roleMappings.contains(role.model));

        assertStatus(Response.Status.BAD_REQUEST, assertThrows(ErrorResponseException.class,
                () -> resource.addUserRoleMappings(List.of(user(member.id), user(outsider.id)))));
        assertEquals(0, member.grants);
        assertFalse(member.roleMappings.contains(role.model));

        context.permissions.userManageDenied.add(restricted.id);
        assertThrows(ForbiddenException.class, () -> resource.addUserRoleMappings(List.of(user(member.id), user(restricted.id))));
        assertEquals(0, member.grants);
        assertFalse(member.roleMappings.contains(role.model));
    }

    @Test
    public void userRoleMappingBatchesValidateAllUsersBeforeRevoking() {
        TestContext context = new TestContext();
        TestRole role = context.addOrganizationRole("role-1", "member");
        TestUser member = context.addUser("member-id", "member");
        TestUser restricted = context.addUser("restricted-id", "restricted");
        member.roleMappings.add(role.model);
        restricted.roleMappings.add(role.model);
        OrganizationRoleResource resource = new OrganizationRoleResource(context.session, context.organization, role.model, context.adminEvent, context.auth);

        assertThrows(NotFoundException.class, () -> resource.deleteUserRoleMappings(List.of(user(member.id), user("missing"))));
        assertEquals(0, member.revokes);
        assertTrue(member.roleMappings.contains(role.model));

        context.permissions.userManageDenied.add(restricted.id);
        assertThrows(ForbiddenException.class, () -> resource.deleteUserRoleMappings(List.of(user(member.id), user(restricted.id))));
        assertEquals(0, member.revokes);
        assertTrue(member.roleMappings.contains(role.model));
    }

    private static String repId(TestRole role) {
        return role.id;
    }

    private static RoleRepresentation role(String id) {
        RoleRepresentation representation = new RoleRepresentation();
        representation.setId(id);
        return representation;
    }

    private static UserRepresentation user(String id) {
        UserRepresentation representation = new UserRepresentation();
        representation.setId(id);
        return representation;
    }

    private static Set<String> roleNames(List<RoleModel> roles) {
        Set<String> names = new LinkedHashSet<>();
        roles.forEach(role -> names.add(role.getName()));
        return names;
    }

    private static void assertStatus(Response.Status status, WebApplicationException exception) {
        assertEquals(status.getStatusCode(), exception.getResponse().getStatus());
    }

    private static void withAdminFineGrainedAuthzV2(boolean enabled, Runnable runnable) {
        Profile.reset();
        Profile.configure(new AdminFineGrainedAuthzV2Resolver(enabled));
        try {
            runnable.run();
        } finally {
            Profile.reset();
            Profile.configure();
        }
    }

    private record AdminFineGrainedAuthzV2Resolver(boolean enabled) implements ProfileConfigResolver {
        @Override
        public Profile.ProfileName getProfileName() {
            return null;
        }

        @Override
        public FeatureConfig getFeatureConfig(String feature) {
            if (enabled && Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2.getVersionedKey().equals(feature)) {
                return FeatureConfig.ENABLED;
            }
            if (!enabled && Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2.getUnversionedKey().equals(feature)) {
                return FeatureConfig.DISABLED;
            }
            return FeatureConfig.UNCONFIGURED;
        }
    }

    private static class TestContext {
        private final Map<String, TestRole> roles = new LinkedHashMap<>();
        private final Map<String, TestUser> users = new LinkedHashMap<>();
        private final Map<String, ClientModel> clients = new LinkedHashMap<>();
        private final List<ProviderEvent> providerEvents = new ArrayList<>();
        private final Set<String> members = new LinkedHashSet<>();
        private final List<UserModel> roleMembers = new ArrayList<>();
        private final TestPermissions permissions = new TestPermissions();
        private RoleModel defaultRole;
        private boolean adminPermissionsEnabled;
        private Integer lastRoleMembersFirst;
        private Integer lastRoleMembersMax;
        private final RealmModel realm;
        private final OrganizationModel organization;
        private final OrganizationModel otherOrganization;
        private final RoleProvider roleProvider;
        private final UserProvider userProvider;
        private final KeycloakSession session;
        private final AdminPermissionEvaluator auth;
        private final AdminEventBuilder adminEvent;

        TestContext() {
            realm = realm();
            organization = organization("org-1", "acme");
            otherOrganization = organization("org-2", "other");
            roleProvider = roleProvider();
            userProvider = userProvider();
            session = session();
            auth = auth();
            adminEvent = adminEventBuilder();
        }

        TestRole addOrganizationRole(String id, String name) {
            return addOrganizationRole(id, name, organization);
        }

        TestRole addOrganizationRole(String id, String name, OrganizationModel container) {
            TestRole role = new TestRole(id, name, RoleModel.Type.ORGANIZATION, container);
            roles.put(id, role);
            return role;
        }

        TestRole addRealmRole(String id, String name) {
            TestRole role = new TestRole(id, name, RoleModel.Type.REALM, realm);
            roles.put(id, role);
            return role;
        }

        TestRole addClientRole(String id, String name, ClientModel client) {
            TestRole role = new TestRole(id, name, RoleModel.Type.CLIENT, client);
            roles.put(id, role);
            return role;
        }

        ClientModel addClient(String id, String clientId) {
            ClientModel client = proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getClientId" -> clientId;
                case "getRealm" -> realm;
                default -> defaultValue(method.getReturnType());
            });
            clients.put(id, client);
            return client;
        }

        TestUser addUser(String id, String username) {
            TestUser user = new TestUser(id, username);
            users.put(id, user);
            return user;
        }

        private RealmModel realm() {
            return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
                case "getId" -> "realm-1";
                case "getName" -> "realm";
                case "getRoleById" -> roles.containsKey(args[0]) ? roles.get(args[0]).model : null;
                case "getClientById" -> clients.get(args[0]);
                case "getDefaultRole" -> null;
                case "getEventsListenersStream" -> Stream.empty();
                case "isAdminEventsEnabled", "isAdminEventsDetailsEnabled" -> false;
                case "isAdminPermissionsEnabled" -> adminPermissionsEnabled;
                default -> defaultValue(method.getReturnType());
            });
        }

        private OrganizationModel organization(String id, String alias) {
            return proxy(OrganizationModel.class, (organizationProxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getRealm" -> realm;
                case "getName", "getAlias" -> alias;
                case "getDefaultRole" -> Objects.equals(id, "org-1") ? defaultRole : null;
                case "getRole" -> roles.values().stream()
                        .filter(role -> role.container == organizationProxy && Objects.equals(role.name, args[0]))
                        .map(role -> role.model)
                        .findFirst()
                        .orElse(null);
                case "addRole" -> addRole(organizationProxy, args);
                case "removeRole" -> removeRole((RoleModel) args[0]);
                case "getRolesStream" -> organizationRoles(organizationProxy, args);
                case "searchForRolesStream" -> searchOrganizationRoles(organizationProxy, args);
                case "isMember" -> members.contains(((UserModel) args[0]).getId());
                default -> defaultValue(method.getReturnType());
            });
        }

        private RoleModel addRole(Object organizationProxy, Object[] args) {
            String id = args.length == 2 ? (String) args[0] : "generated-" + (roles.size() + 1);
            String name = (String) args[args.length - 1];
            if (roles.values().stream().anyMatch(role -> role.container == organizationProxy && Objects.equals(role.name, name))) {
                throw new ModelDuplicateException();
            }
            return addOrganizationRole(id, name, (OrganizationModel) organizationProxy).model;
        }

        private boolean removeRole(RoleModel role) {
            return roles.remove(role.getId()) != null;
        }

        private Stream<RoleModel> organizationRoles(Object organizationProxy, Object[] args) {
            Stream<RoleModel> stream = roles.values().stream()
                    .filter(role -> role.container == organizationProxy)
                    .map(role -> role.model);
            if (args != null && args.length == 2) {
                stream = page(stream, (Integer) args[0], (Integer) args[1]);
            }
            return stream;
        }

        private Stream<RoleModel> searchOrganizationRoles(Object organizationProxy, Object[] args) {
            String search = ((String) args[0]).toLowerCase();
            return page(roles.values().stream()
                    .filter(role -> role.container == organizationProxy)
                    .filter(role -> role.name.toLowerCase().contains(search) || (role.description != null && role.description.toLowerCase().contains(search)))
                    .map(role -> role.model), (Integer) args[1], (Integer) args[2]);
        }

        private RoleProvider roleProvider() {
            return proxy(RoleProvider.class, (providerProxy, method, args) -> switch (method.getName()) {
                case "getRoleById" -> {
                    if (args[0] instanceof OrganizationModel organizationModel) {
                        TestRole role = roles.get(args[1]);
                        yield role != null && role.container == organizationModel ? role.model : null;
                    }
                    yield roles.containsKey(args[1]) ? roles.get(args[1]).model : null;
                }
                case "getOrganizationRolesCount" -> {
                    String search = (String) args[1];
                    yield roles.values().stream()
                            .filter(role -> role.container == args[0])
                            .filter(role -> search == null || role.name.contains(search) || (role.description != null && role.description.contains(search)))
                            .count();
                }
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            });
        }

        private UserProvider userProvider() {
            return proxy(UserProvider.class, (providerProxy, method, args) -> switch (method.getName()) {
                case "getUserById" -> users.containsKey(args[1]) ? users.get(args[1]).model : null;
                case "getRoleMembersStream" -> {
                    lastRoleMembersFirst = (Integer) args[2];
                    lastRoleMembersMax = (Integer) args[3];
                    yield roleMembers.stream();
                }
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            });
        }

        private KeycloakSession session() {
            KeycloakSessionFactory factory = proxy(KeycloakSessionFactory.class, (factoryProxy, method, args) -> {
                if ("getProviderFactoriesStream".equals(method.getName()) && args[0].equals(EventListenerProvider.class)) {
                    return Stream.empty();
                }
                if ("publish".equals(method.getName())) {
                    providerEvents.add((ProviderEvent) args[0]);
                    return null;
                }
                return defaultValue(method.getReturnType());
            });
            KeycloakContext context = proxy(KeycloakContext.class, (contextProxy, method, args) -> switch (method.getName()) {
                case "getRealm" -> realm;
                case "getUri" -> uriInfo();
                case "setOrganization" -> null;
                default -> defaultValue(method.getReturnType());
            });
            return proxy(KeycloakSession.class, (sessionProxy, method, args) -> {
                if ("getKeycloakSessionFactory".equals(method.getName())) {
                    return factory;
                }
                if ("getContext".equals(method.getName())) {
                    return context;
                }
                if ("roles".equals(method.getName())) {
                    return roleProvider;
                }
                if ("users".equals(method.getName())) {
                    return userProvider;
                }
                if ("getProvider".equals(method.getName())) {
                    if (args[0].equals(HostnameProvider.class)) {
                        return hostnameProvider();
                    }
                    if (args[0].equals(UserProfileProvider.class)) {
                        return userProfileProvider();
                    }
                    if (args[0].equals(OrganizationProvider.class)) {
                        return proxy(OrganizationProvider.class, (providerProxy, providerMethod, providerArgs) -> defaultValue(providerMethod.getReturnType()));
                    }
                }
                return defaultValue(method.getReturnType());
            });
        }

        private AdminPermissionEvaluator auth() {
            RolePermissionEvaluator roles = proxy(RolePermissionEvaluator.class, (rolesProxy, method, args) -> switch (method.getName()) {
                case "requireManage" -> require(permissions.roleManage);
                case "requireView" -> require(permissions.roleView);
                case "requireList" -> require(permissions.roleList);
                case "requireMapComposite" -> require(permissions.mapComposite);
                case "requireMapRole" -> require(permissions.mapRole);
                case "canManage" -> permissions.roleManage;
                case "canView" -> permissions.roleView;
                case "canList" -> permissions.roleList;
                case "canMapComposite" -> permissions.mapComposite;
                case "canMapRole" -> permissions.mapRole;
                default -> defaultValue(method.getReturnType());
            });
            UserPermissionEvaluator users = proxy(UserPermissionEvaluator.class, (usersProxy, method, args) -> switch (method.getName()) {
                case "requireQuery" -> require(permissions.userQuery);
                case "requireManage" -> require(permissions.canManageUser((UserModel) args[0]));
                case "requireView" -> require(permissions.userView);
                case "canQuery" -> permissions.userQuery;
                case "canManage" -> args == null || args.length == 0 ? permissions.userManage : permissions.canManageUser((UserModel) args[0]);
                case "canView" -> permissions.userView;
                default -> defaultValue(method.getReturnType());
            });
            OrganizationPermissionEvaluator orgs = proxy(OrganizationPermissionEvaluator.class, (orgsProxy, method, args) -> switch (method.getName()) {
                case "requireManage" -> require(permissions.organizationManage);
                case "requireView" -> require(permissions.organizationView);
                case "requireQuery" -> require(permissions.organizationQuery);
                case "canManage" -> permissions.organizationManage;
                case "canView" -> permissions.organizationView;
                case "canQuery" -> permissions.organizationQuery;
                default -> defaultValue(method.getReturnType());
            });
            return proxy(AdminPermissionEvaluator.class, (authProxy, method, args) -> switch (method.getName()) {
                case "roles" -> roles;
                case "users" -> users;
                case "orgs" -> orgs;
                default -> defaultValue(method.getReturnType());
            });
        }

        private Object require(boolean allowed) {
            if (!allowed) {
                throw new ForbiddenException();
            }
            return null;
        }

        private AdminEventBuilder adminEventBuilder() {
            ClientModel client = addClient("admin-client", "admin-client");
            UserModel user = addUser("admin-user", "admin").model;
            ClientConnection connection = proxy(ClientConnection.class, (connectionProxy, method, args) -> switch (method.getName()) {
                case "getRemoteAddr", "getRemoteHost", "getLocalAddr" -> "127.0.0.1";
                default -> defaultValue(method.getReturnType());
            });
            return new AdminEventBuilder(realm, new AdminAuth(realm, null, user, client), session, connection);
        }
    }

    private static class TestPermissions {
        private boolean roleManage = true;
        private boolean roleView = true;
        private boolean roleList = true;
        private boolean mapComposite = true;
        private boolean mapRole = true;
        private boolean userQuery = true;
        private boolean userManage = true;
        private final Set<String> userManageDenied = new LinkedHashSet<>();
        private boolean userView = true;
        private boolean organizationManage = true;
        private boolean organizationView = true;
        private boolean organizationQuery = true;

        private boolean canManageUser(UserModel user) {
            return userManage && !userManageDenied.contains(user.getId());
        }
    }

    private static class TestRole {
        private final String id;
        private final RoleModel.Type type;
        private final RoleContainerModel container;
        private final RoleModel model;
        private String name;
        private String description;
        private final Map<String, List<String>> attributes = new LinkedHashMap<>();
        private final List<RoleModel> composites = new ArrayList<>();

        TestRole(String id, String name, RoleModel.Type type, RoleContainerModel container) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.container = container;
            this.model = proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getName" -> this.name;
                case "setName" -> {
                    if (Objects.equals(args[0], "duplicate")) {
                        throw new ModelDuplicateException();
                    }
                    this.name = (String) args[0];
                    yield null;
                }
                case "getDescription" -> description;
                case "setDescription" -> {
                    description = (String) args[0];
                    yield null;
                }
                case "getType" -> type;
                case "isClientRole" -> type == RoleModel.Type.CLIENT;
                case "isRealmRole" -> type == RoleModel.Type.REALM;
                case "isOrganizationRole" -> type == RoleModel.Type.ORGANIZATION;
                case "getContainer" -> container;
                case "getContainerId" -> container.getId();
                case "isComposite" -> !composites.isEmpty();
                case "addCompositeRole" -> {
                    composites.add((RoleModel) args[0]);
                    yield null;
                }
                case "removeCompositeRole" -> {
                    composites.remove(args[0]);
                    yield null;
                }
                case "getCompositesStream" -> compositesStream(args);
                case "setAttribute" -> {
                    attributes.put((String) args[0], (List<String>) args[1]);
                    yield null;
                }
                case "removeAttribute" -> {
                    attributes.remove(args[0]);
                    yield null;
                }
                case "getAttributeStream" -> attributes.getOrDefault(args[0], List.of()).stream();
                case "getAttributes" -> attributes;
                case "hasRole" -> Objects.equals(roleProxy, args[0]) || composites.contains(args[0]);
                default -> defaultValue(method.getReturnType());
            });
        }

        private Stream<RoleModel> compositesStream(Object[] args) {
            Stream<RoleModel> stream = composites.stream();
            if (args != null && args.length == 3) {
                String search = (String) args[0];
                if (search != null) {
                    stream = stream.filter(role -> role.getName().contains(search));
                }
                stream = page(stream, (Integer) args[1], (Integer) args[2]);
            }
            return stream;
        }
    }

    private static class TestUser {
        private final String id;
        private final String username;
        private final UserModel model;
        private final Set<RoleModel> roleMappings = new LinkedHashSet<>();
        private int grants;
        private int revokes;
        private boolean failIllegalStateOnGrant;
        private boolean failReadOnlyOnDelete;

        TestUser(String id, String username) {
            this.id = id;
            this.username = username;
            this.model = proxy(UserModel.class, (userProxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getUsername" -> username;
                case "getEmail" -> username + "@example.test";
                case "getFirstName" -> username;
                case "getLastName" -> "User";
                case "getCreatedTimestamp" -> 1L;
                case "isEnabled", "isEmailVerified" -> true;
                case "getFederationLink", "getFirstAttribute" -> null;
                case "getAttributes" -> Map.of();
                case "getRoleMappingsStream", "getRealmRoleMappingsStream", "getClientRoleMappingsStream" -> roleMappings.stream();
                case "hasRole", "hasDirectRole" -> roleMappings.contains(args[0]);
                case "grantRole" -> {
                    if (failIllegalStateOnGrant) {
                        throw new ModelIllegalStateException("grant failed");
                    }
                    grants++;
                    roleMappings.add((RoleModel) args[0]);
                    yield null;
                }
                case "deleteRoleMapping" -> {
                    if (failReadOnlyOnDelete) {
                        throw new ReadOnlyException("delete failed");
                    }
                    revokes++;
                    roleMappings.remove(args[0]);
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    private static HostnameProvider hostnameProvider() {
        return proxy(HostnameProvider.class, (providerProxy, method, args) ->
                "getBaseUri".equals(method.getName()) ? URI.create("http://localhost") : defaultValue(method.getReturnType()));
    }

    private static UserProfileProvider userProfileProvider() {
        return proxy(UserProfileProvider.class, (providerProxy, method, args) -> {
            if ("create".equals(method.getName()) && args.length == 2 && args[1] instanceof UserModel user) {
                return userProfile(user);
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static UserProfile userProfile(UserModel user) {
        return proxy(UserProfile.class, (profileProxy, method, args) -> {
            if ("toRepresentation".equals(method.getName())) {
                UserRepresentation representation = new UserRepresentation();
                representation.setId(user.getId());
                representation.setUsername(user.getUsername());
                representation.setEnabled(true);
                return representation;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static KeycloakUriInfo uriInfo() {
        UriInfo delegate = proxy(UriInfo.class, (uriProxy, method, args) -> switch (method.getName()) {
            case "getPath" -> "admin/realms/realm/organizations/org-1/roles";
            case "getAbsolutePath" -> URI.create("http://localhost/admin/realms/realm/organizations/org-1/roles");
            case "getAbsolutePathBuilder" -> UriBuilder.fromUri("http://localhost/admin/realms/realm/organizations/org-1/roles");
            default -> defaultValue(method.getReturnType());
        });
        KeycloakSession session = proxy(KeycloakSession.class, (sessionProxy, method, args) ->
                "getProvider".equals(method.getName()) && args[0].equals(HostnameProvider.class)
                        ? hostnameProvider() : defaultValue(method.getReturnType()));
        return new KeycloakUriInfo(session, UrlType.FRONTEND, delegate);
    }

    private static <T> Stream<T> page(Stream<T> stream, Integer first, Integer max) {
        if (first != null && first > 0) {
            stream = stream.skip(first);
        }
        if (max != null && max >= 0) {
            stream = stream.limit(max);
        }
        return stream;
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

/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.services.resources.admin.AdminAuth.Resource;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.FederatedIdentityBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import org.hamcrest.Matchers;
import org.jgroups.util.UUID;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class PermissionsTest extends AbstractPermissionsTest {

    @InjectRealm(config = PermissionsTestRealmConfig1.class, ref = "realm1")
    ManagedRealm managedRealm1;

    @InjectRealm(config = PermissionsTestRealmConfig2.class, ref = "realm2")
    ManagedRealm managedRealm2;

    @Test
    public void realmLogoutAll() {
        Invocation invocation = RealmResource::logoutAll;

        invoke(invocation, clients.get("master-none"), false);
        invoke(invocation, clients.get("master-view-realm"), false);

        invoke(invocation, clients.get("REALM2"), false);

        invoke(invocation, clients.get("none"), false);
        invoke(invocation, clients.get("view-users"), false);
        invoke(invocation, clients.get("manage-realm"), false);
        invoke(invocation, clients.get("master-manage-realm"), false);

        invoke(invocation, clients.get("manage-users"), true);
        invoke(invocation, clients.get("master-manage-users"), true);
    }

    @Test
    public void events() {
        invoke(RealmResource::getRealmEventsConfig, Resource.EVENTS, false);
        invoke(realm -> realm.updateRealmEventsConfig(new RealmEventsConfigRepresentation()), Resource.EVENTS, true);
        invoke(RealmResource::getEvents, Resource.EVENTS, false);
        invoke(RealmResource::getAdminEvents, Resource.EVENTS, false);
        invoke(RealmResource::clearEvents, Resource.EVENTS, true);
        invoke(RealmResource::clearAdminEvents, Resource.EVENTS, true);
    }

    @Test
    public void attackDetection() {
        UserRepresentation newUser = UserConfigBuilder.create()
                .username("attacked")
                .enabled(true)
                .build();
        String userUuid = ApiUtil.getCreatedId(managedRealm1.admin().users().create(newUser));
        managedRealm1.cleanup().add(r -> r.users().delete(userUuid).close());

        UserRepresentation user = managedRealm1.admin().users().get(userUuid).toRepresentation();
        invoke(realm -> realm.attackDetection().bruteForceUserStatus(userUuid), Resource.USER, false);
        invoke(realm -> realm.attackDetection().clearBruteForceForUser(userUuid), Resource.USER, true);
        invoke(realm -> realm.attackDetection().clearAllBruteForce(), Resource.USER, true);
    }

    @Test
    public void clients() {
        invoke(realm -> realm.clients().findAll(), Resource.CLIENT, false, true);
        List<ClientRepresentation> l = clients.get(AdminRoles.QUERY_CLIENTS).realm(REALM_NAME).clients().findAll();
        assertThat(l, Matchers.empty());

        l = clients.get(AdminRoles.VIEW_CLIENTS).realm(REALM_NAME).clients().findAll();
        assertThat(l, Matchers.not(Matchers.empty()));

        ClientRepresentation client = l.get(0);
        invoke((realm, response) ->
                        response.set(clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clients().create(client)),
                clients.get(AdminRoles.QUERY_USERS), false);
        invoke(realm -> clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clients().get(client.getId()).toRepresentation(),
                clients.get(AdminRoles.QUERY_USERS), false);
        invoke(realm -> clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clients().get(client.getId()).update(client),
                clients.get(AdminRoles.QUERY_USERS), false);
        invoke(realm -> clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clients().get(client.getId()).remove(),
                clients.get(AdminRoles.QUERY_USERS), false);

        invoke(realm -> realm.convertClientDescription("blahblah"), Resource.CLIENT, true);
        invoke((realm, response) ->
                        response.set(realm.clients().create(ClientConfigBuilder.create().clientId("foo").build())),
                Resource.CLIENT, true);

        ClientRepresentation foo = managedRealm1.admin().clients().findByClientId("foo").get(0);

        invoke(realm -> realm.clients().get(foo.getId()).toRepresentation(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getInstallationProvider("nosuch"), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).update(foo), Resource.CLIENT, true);
        invoke(realm -> {
            realm.clients().get(foo.getId()).remove();
            realm.clients().create(foo);
            ClientRepresentation temp = realm.clients().findByClientId("foo").get(0);
            foo.setId(temp.getId());
        }, Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).generateNewSecret(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).regenerateRegistrationAccessToken(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getSecret(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getServiceAccountUser(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).pushRevocation(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getApplicationSessionCount(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getUserSessions(0, 100), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getOfflineSessionCount(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getOfflineUserSessions(0, 100), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).registerNode(Map.of()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).unregisterNode("nosuch"), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).testNodesAvailable(), Resource.CLIENT, true);

        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").generate(), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").generateAndGetKeystore(new KeyStoreConfig()),
                Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").getKeyInfo(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").getKeystore(new KeyStoreConfig()),
                Resource.CLIENT, false);

        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").uploadJks(null), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getCertficateResource("nosuch").uploadJksCertificate(null),
                Resource.CLIENT, true);

        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().createMapper(List.of()), Resource.CLIENT, true);
        invoke((realm, response) ->
                        response.set(realm.clients().get(foo.getId()).getProtocolMappers().createMapper(new ProtocolMapperRepresentation())),
                Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().getMapperById("nosuch"), Resource.CLIENT, false, true);
        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().getMappers(), Resource.CLIENT, false, true);
        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().getMappersPerProtocol("nosuch"),
                Resource.CLIENT, false, true);
        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().update("nosuch",
                        new ProtocolMapperRepresentation()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getProtocolMappers().delete("nosuch"), Resource.CLIENT, true);

        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().getAll(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().listAll(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().listEffective(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().listAvailable(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().add(List.of()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).getScopeMappings().realmLevel().remove(List.of()), Resource.CLIENT, true);

        invoke(realm -> realm.clients().get(UUID.randomUUID().toString()).roles().list(), Resource.CLIENT, false, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().create(new RoleRepresentation()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").toRepresentation(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).roles().deleteRole("nosuch"), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").update(new RoleRepresentation()),
                Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").addComposites(List.of()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").deleteComposites(List.of()), Resource.CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").getRoleComposites(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").getRealmRoleComposites(), Resource.CLIENT, false);
        invoke(realm -> realm.clients().get(foo.getId()).roles().get("nosuch").getClientRoleComposites("nosuch"), Resource.CLIENT, false);
        // users with query-client role should be able to query flows so the client detail page can be rendered successfully when fine-grained permissions are enabled.
        invoke(realm -> realm.flows().getFlows(), clients.get(AdminRoles.QUERY_CLIENTS), true);
        // the same for ClientAuthenticatorProviders and PerClientConfigDescription
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), clients.get(AdminRoles.QUERY_CLIENTS), true);
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), clients.get(AdminRoles.VIEW_CLIENTS), true);
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), clients.get(AdminRoles.MANAGE_CLIENTS), true);
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), clients.get(AdminRoles.QUERY_USERS), false);
        invoke(realm -> realm.flows().getPerClientConfigDescription(), clients.get(AdminRoles.QUERY_CLIENTS), true);
    }

    @Test
    public void clientScopes() {
        invoke((RealmResource realm) -> realm.clientScopes().findAll(), Resource.CLIENT, false, true);
        invoke((RealmResource realm, AtomicReference<Response> response) -> {
            ClientScopeRepresentation scope = new ClientScopeRepresentation();
            scope.setName("scope");
            response.set(realm.clientScopes().create(scope));
        }, Resource.CLIENT, true);

        ClientScopeRepresentation scope = managedRealm1.admin().clientScopes().findAll().get(0);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).toRepresentation(), Resource.CLIENT, false);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).update(scope), Resource.CLIENT, true);
        invoke((RealmResource realm) -> {
            realm.clientScopes().get(scope.getId()).remove();
            realm.clientScopes().create(scope);
        }, Resource.CLIENT, true);

        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getProtocolMappers().getMappers(),
                Resource.CLIENT, false, true);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scope.getId()).getProtocolMappers().getMappersPerProtocol("nosuch"),
                Resource.CLIENT, false, true);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getProtocolMappers().getMapperById("nosuch"),
                Resource.CLIENT, false, true);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scope.getId()).getProtocolMappers().update("nosuch", new ProtocolMapperRepresentation()),
                Resource.CLIENT, true);
        invoke((RealmResource realm, AtomicReference<Response> response) ->
                        response.set(realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(new ProtocolMapperRepresentation())),
                Resource.CLIENT, true);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getProtocolMappers().createMapper(List.of()),
                Resource.CLIENT, true);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getProtocolMappers().delete("nosuch"),
                Resource.CLIENT, true);

        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getScopeMappings().getAll(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getScopeMappings().realmLevel().listAll(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getScopeMappings().realmLevel().listAvailable(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getScopeMappings().realmLevel().listEffective(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getScopeMappings().realmLevel().add(List.of()),
                Resource.CLIENT, true);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getScopeMappings().realmLevel().remove(List.of()),
                Resource.CLIENT, true);
        ClientRepresentation realmAccessClient = adminClient.realms().realm(REALM_NAME) .clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        invoke((RealmResource realm) -> realm.clientScopes().get(scope.getId()).getScopeMappings().clientLevel(realmAccessClient.getId()).listAll(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scope.getId()).getScopeMappings().clientLevel(realmAccessClient.getId()).listAvailable(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scope.getId()).getScopeMappings().clientLevel(realmAccessClient.getId()).listEffective(),
                Resource.CLIENT, false);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scope.getId()).getScopeMappings().clientLevel(realmAccessClient.getId()).add(List.of()),
                Resource.CLIENT, true);
        invoke((RealmResource realm) ->
                        realm.clientScopes().get(scope.getId()).getScopeMappings().clientLevel(realmAccessClient.getId()).remove(List.of()),
                Resource.CLIENT, true);

        // this should throw forbidden as "query-users" role isn't enough
        invoke(realm -> clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clientScopes().findAll(),
                clients.get(AdminRoles.QUERY_USERS), false);
    }

    @Test
    public void clientInitialAccess() {
        invoke(realm -> realm.clientInitialAccess().list(), Resource.CLIENT, false);
        invoke(realm -> realm.clientInitialAccess().create(new ClientInitialAccessCreatePresentation()), Resource.CLIENT, true);
        invoke(realm -> realm.clientInitialAccess().delete("nosuch"), Resource.CLIENT, true);
    }

    @Test
    public void roles() {
        RoleRepresentation newRole = RoleConfigBuilder.create().name("sample-role").build();
        managedRealm1.admin().roles().create(newRole);
        managedRealm1.cleanup().add(r -> r.roles().deleteRole("sample-role"));

        invoke(realm -> realm.roles().list(), Resource.REALM, false, true);

        // this should throw forbidden as "create-client" role isn't enough
        invoke(realm -> clients.get(AdminRoles.CREATE_CLIENT).realm(REALM_NAME).roles().list(),
                clients.get(AdminRoles.CREATE_CLIENT), false);
        invoke(realm -> realm.roles().get("sample-role").toRepresentation(),
                Resource.REALM, false);
        invoke(realm -> realm.roles().get("sample-role").update(newRole),
                Resource.REALM, true);
        invoke(realm -> realm.roles().create(new RoleRepresentation()),
                Resource.REALM, true);
        invoke(realm -> {
            realm.roles().deleteRole("sample-role");
            // need to recreate for other tests
            realm.roles().create(newRole);
        }, Resource.REALM, true);
        invoke(realm -> realm.roles().get("sample-role").getRoleComposites(), Resource.REALM, false);
        invoke(realm -> realm.roles().get("sample-role").addComposites(List.of()), Resource.REALM, true);
        invoke(realm -> realm.roles().get("sample-role").deleteComposites(List.of()), Resource.REALM, true);
        invoke(realm -> realm.roles().get("sample-role").getRoleComposites(), Resource.REALM, false);
        invoke(realm -> realm.roles().get("sample-role").getRealmRoleComposites(), Resource.REALM, false);
        invoke(realm -> realm.roles().get("sample-role").getClientRoleComposites(KeycloakModelUtils.generateId()), Resource.REALM, false);
    }

    @Test
    public void rolesById() {
        RoleRepresentation newRole = RoleConfigBuilder.create().name("role-by-id").build();
        managedRealm1.admin().roles().create(newRole);
        RoleRepresentation role = managedRealm1.admin().roles().get("role-by-id").toRepresentation();
        managedRealm1.cleanup().add(r -> r.roles().deleteRole("role-by-id"));

        invoke(realm -> realm.rolesById().getRole(role.getId()), Resource.REALM, false, true);
        invoke(realm -> realm.rolesById().updateRole(role.getId(), role), Resource.REALM, true);
        invoke(realm -> {
            realm.rolesById().deleteRole(role.getId());
            // need to recreate for other tests
            realm.roles().create(newRole);
            RoleRepresentation temp = realm.roles().get("role-by-id").toRepresentation();
            role.setId(temp.getId());
        }, Resource.REALM, true);
        invoke(realm -> realm.rolesById().getRoleComposites(role.getId()), Resource.REALM, false, true);
        invoke(realm -> realm.rolesById().addComposites(role.getId(), List.of()), Resource.REALM, true);
        invoke(realm -> realm.rolesById().deleteComposites(role.getId(), List.of()), Resource.REALM, true);
        invoke(realm -> realm.rolesById().getRoleComposites(role.getId()), Resource.REALM, false, true);
        invoke(realm -> realm.rolesById().getRealmRoleComposites(role.getId()), Resource.REALM, false, true);
        invoke(realm -> realm.rolesById().getClientRoleComposites(role.getId(), KeycloakModelUtils.generateId()), Resource.REALM, false, true);
    }

    @Test
    public void groups() {
        invoke(realm -> realm.groups().groups(), Resource.USER, false);
        invoke(realm -> realm.groups().count(), Resource.USER, false);
        invoke((realm, response) -> {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("mygroup");
            response.set(realm.groups().add(group));
        }, Resource.USER, true);

        GroupRepresentation group = managedRealm1.admin().getGroupByPath("mygroup");
        ClientRepresentation realmAccessClient = managedRealm1.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);

        // this should throw forbidden as "create-client" role isn't enough
        invoke(realm -> clients.get(AdminRoles.CREATE_CLIENT).realm(REALM_NAME).groups().groups(),
                clients.get(AdminRoles.CREATE_CLIENT), false);

        invoke(realm -> realm.groups().group(group.getId()).toRepresentation(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).update(group), Resource.USER, true);
        invoke(realm -> realm.groups().group(group.getId()).members(0, 100), Resource.USER, false);
        invoke((realm, response) -> {
            GroupRepresentation subgroup = new GroupRepresentation();
            subgroup.setName("sub");
            response.set(realm.groups().group(group.getId()).subGroup(subgroup));
        }, Resource.USER, true);

        invoke(realm -> realm.groups().group(group.getId()).roles().getAll(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().listAll(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().listEffective(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().listAvailable(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().add(List.of()), Resource.USER, true);
        invoke(realm -> realm.groups().group(group.getId()).roles().realmLevel().remove(List.of()), Resource.USER, true);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).listAll(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).listEffective(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).listAvailable(), Resource.USER, false);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).add(List.of()), Resource.USER, true);
        invoke(realm -> realm.groups().group(group.getId()).roles().clientLevel(realmAccessClient.getId()).remove(List.of()), Resource.USER, true);
        invoke(realm -> {
            realm.groups().group(group.getId()).remove();
            group.setId(null);
            realm.groups().add(group);
            GroupRepresentation temp = realm.getGroupByPath("mygroup");
            group.setId(temp.getId());
        }, Resource.USER, true);
    }

    // Permissions for impersonation tested in ImpersonationTest
    @Test
    public void users() {
        invoke((realm, response) ->
                        response.set(realm.users().create(UserConfigBuilder.create().username("testuser").build())),
                Resource.USER, true);
        UserRepresentation user = managedRealm1.admin().users().search("testuser").get(0);
        invoke(realm -> {
            realm.users().get(user.getId()).remove();
            realm.users().create(user);
            UserRepresentation temp = realm.users().search("testuser").get(0);
            user.setId(temp.getId());
        }, Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).toRepresentation(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).getUnmanagedAttributes(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).update(user), Resource.USER, true);
        invoke(realm -> realm.users().count(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).getUserSessions(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).getOfflineSessions(KeycloakModelUtils.generateId()), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).getFederatedIdentity(), Resource.USER, false);
        invoke((realm, response) -> response.set(realm.users().get(user.getId()).addFederatedIdentity("nosuch",
                        FederatedIdentityBuilder.create().identityProvider("nosuch").userId("nosuch").userName("nosuch").build()
                )
        ), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).removeFederatedIdentity("nosuch"), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).getConsents(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).revokeConsent("testclient"), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).logout(), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).resetPassword(CredentialBuilder.create().password("password").build()),
                Resource.USER, true);
        invoke(realm -> {
            CredentialRepresentation totpCredential = realm.users().get(user.getId()).credentials().stream()
                    .filter(c -> OTPCredentialModel.TYPE.equals(c.getType())).findFirst().orElse(null);
            if (totpCredential != null) {
                realm.users().get(user.getId()).removeCredential(totpCredential.getId());
            } else {
                realm.users().get(user.getId()).removeCredential("123");
            }
        }, Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).executeActionsEmail(List.of(UserModel.RequiredAction.UPDATE_PASSWORD.name())),
                Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).executeActionsEmail(List.of()), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).sendVerifyEmail(), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).groups(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).leaveGroup("nosuch"), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).joinGroup("nosuch"), Resource.USER, true);

        invoke(realm -> realm.users().get(user.getId()).roles().getAll(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().listAll(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().listAvailable(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().listEffective(), Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().add(List.of()), Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).roles().realmLevel().remove(List.of()), Resource.USER, true);

        ClientRepresentation realmAccessClient = managedRealm1.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).listAll(),
                Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).listAvailable(),
                Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).listEffective(),
                Resource.USER, false);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).add(List.of()),
                Resource.USER, true);
        invoke(realm -> realm.users().get(user.getId()).roles().clientLevel(realmAccessClient.getId()).remove(List.of()),
                Resource.USER, true);
        invoke(realm -> realm.users().search("foo", 0, 1), Resource.USER, false);
        // this should throw forbidden as "query-client" role isn't enough
        invoke(realm -> clients.get(AdminRoles.QUERY_CLIENTS).realm(REALM_NAME).users().list(),
                clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke((realm, response) -> response.set(clients.get(AdminRoles.QUERY_CLIENTS).realm(REALM_NAME).users().create(user)),
                clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke(realm -> clients.get(AdminRoles.QUERY_CLIENTS).realm(REALM_NAME).users().search("test"),
                clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke(realm -> realm.users().get(user.getId()).toRepresentation(), clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke(realm -> realm.users().get(user.getId()).remove(), clients.get(AdminRoles.QUERY_CLIENTS), false);
        invoke(realm -> realm.users().get(user.getId()).update(user), clients.get(AdminRoles.QUERY_CLIENTS), false);
        // users with query-user role should be able to query required actions so the user detail page can be rendered successfully when fine-grained permissions are enabled.
        invoke(realm -> realm.flows().getRequiredActions(), clients.get(AdminRoles.QUERY_USERS), true);
        invoke(realm -> clients.get(AdminRoles.VIEW_USERS).realm(REALM_NAME).users().get(user.getId()).getConfiguredUserStorageCredentialTypes(),
                clients.get(AdminRoles.VIEW_USERS), true);
    }

    @Test
    public void identityProviders() {
        invoke(realm -> realm.identityProviders().findAll(), Resource.IDENTITY_PROVIDER, false);
        invoke((realm, response) -> response.set(realm.identityProviders().create(
                                IdentityProviderBuilder.create()
                                        .providerId("oidc")
                                        .displayName("nosuch-foo")
                                        .alias("foo")
                                        .setAttribute("clientId", "foo")
                                        .setAttribute("clientSecret", "foo")
                                        .build()
                        )
                ), Resource.IDENTITY_PROVIDER, true);
        invoke(realm -> realm.identityProviders().get("nosuch").toRepresentation(), Resource.IDENTITY_PROVIDER, false);
        invoke(realm -> realm.identityProviders().get("nosuch").update(new IdentityProviderRepresentation()), Resource.IDENTITY_PROVIDER, true);
        invoke((realm, response) -> response.set(realm.identityProviders().get("nosuch").export("saml")), Resource.IDENTITY_PROVIDER, false);
        invoke(realm -> realm.identityProviders().get("nosuch").remove(), Resource.IDENTITY_PROVIDER, true);

        invoke((realm, response) -> response.set(realm.identityProviders().get("nosuch").addMapper(new IdentityProviderMapperRepresentation())), Resource.IDENTITY_PROVIDER, true);
        invoke(realm -> realm.identityProviders().get("nosuch").delete("nosuch"), Resource.IDENTITY_PROVIDER, true);
        invoke(realm -> realm.identityProviders().get("nosuch").getMappers(), Resource.IDENTITY_PROVIDER, false);
        invoke(realm -> realm.identityProviders().get("nosuch").getMapperById("nosuch"), Resource.IDENTITY_PROVIDER, false);
        invoke(realm -> realm.identityProviders().get("nosuch").getMapperTypes(), Resource.IDENTITY_PROVIDER, false);

        invoke(realm -> realm.identityProviders().importFrom(Map.of()), Resource.IDENTITY_PROVIDER, true);
        invoke(realm -> realm.identityProviders().importFrom(null), Resource.IDENTITY_PROVIDER, true);
    }

    @Test
    public void components() {
        invoke(realm -> realm.components().query(), Resource.REALM, false);
        invoke(realm -> realm.components().query("nosuch"), Resource.REALM, false);
        invoke(realm -> realm.clientRegistrationPolicy().getProviders(), Resource.REALM, false);
        invoke((realm, response) -> response.set(realm.components().add(new ComponentRepresentation())), Resource.REALM, true);
        invoke(realm -> realm.components().component("nosuch").toRepresentation(), Resource.REALM, false);
        invoke(realm -> realm.components().component("nosuch").update(new ComponentRepresentation()), Resource.REALM, true);
        invoke(realm -> realm.components().component("nosuch").remove(), Resource.REALM, true);
    }

    @Test
    public void partialExport() {
        invoke(realm -> realm.partialExport(false, false), clients.get("view-realm"), false);
        invoke(realm -> realm.partialExport(false, false), clients.get("manage-realm"), true);
        invoke(realm -> realm.partialExport(true, false), clients.get("manage-realm"), false);
        invoke(realm -> realm.partialExport(false, true), clients.get("manage-realm"), false);
        invoke(realm -> realm.partialExport(true, true), clients.get("multi"), true);
        invoke(realm -> realm.partialExport(false, false), clients.get("none"), false);
    }

    @Test
    public void localizations() {
        verifyAnyAdminRoleReqired(realm -> realm.localization().getRealmSpecificLocales());

        verifyAnyAdminRoleReqired(realm -> realm.localization().getRealmLocalizationText("en", "test"));

        verifyAnyAdminRoleReqired(realm -> realm.localization().getRealmLocalizationTexts("en"));
        verifyAnyAdminRoleReqired(realm -> realm.localization().getRealmLocalizationTexts("en", false));

        invoke(realm -> realm.localization().createOrUpdateRealmLocalizationTexts("en", Map.of()), clients.get("view-realm"), false);
        invoke(realm -> realm.localization().createOrUpdateRealmLocalizationTexts("en", Map.of()), clients.get("manage-realm"), true);
        invoke(realm -> realm.localization().createOrUpdateRealmLocalizationTexts("en", Map.of()), clients.get("master-admin"), true);
        invoke(realm -> realm.localization().createOrUpdateRealmLocalizationTexts("en", Map.of()), clients.get("none"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("REALM2"), false);

        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("view-realm"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("manage-realm"), true);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("master-admin"), true);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("none"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationText("en", "test"), clients.get("REALM2"), false);

        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("view-realm"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("manage-realm"), true);
        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("master-admin"), true);
        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("none"), false);
        invoke(realm -> realm.localization().deleteRealmLocalizationTexts("en"), clients.get("REALM2"), false);
    }

    @Test
    public void testServerInfo() throws Exception {
        // user in master with no permission => forbidden
        Assert.assertThrows(ForbiddenException.class, () -> clients.get("master-none").serverInfo().getInfo());
        // user in master with any permission can see the system info
        ServerInfoRepresentation serverInfo = clients.get("master-view-realm").serverInfo().getInfo();
        Assert.assertNotNull(serverInfo.getSystemInfo());
        Assert.assertNotNull(serverInfo.getCpuInfo());
        Assert.assertNotNull(serverInfo.getMemoryInfo());

        // user in test realm with no permission => forbidden
        Assert.assertThrows(ForbiddenException.class, () -> clients.get("none").serverInfo().getInfo());
        // user in test realm with any permission cannot see the system info
        serverInfo = clients.get("view-realm").serverInfo().getInfo();
        Assert.assertNull(serverInfo.getSystemInfo());
        Assert.assertNull(serverInfo.getCpuInfo());
        Assert.assertNull(serverInfo.getMemoryInfo());
        serverInfo = clients.get("manage-users").serverInfo().getInfo();
        Assert.assertNull(serverInfo.getSystemInfo());
        Assert.assertNull(serverInfo.getCpuInfo());
        Assert.assertNull(serverInfo.getMemoryInfo());

        // assign the view-system permission to a test realm user and check the fallback works
        ClientRepresentation realmMgtRep = adminClient.realm(REALM_NAME).clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        ClientResource realmMgtRes = adminClient.realm(REALM_NAME).clients().get(realmMgtRep.getId());
        RoleRepresentation viewSystem = new RoleRepresentation();
        viewSystem.setName(AdminRoles.VIEW_SYSTEM);
        realmMgtRes.roles().create(viewSystem);
        viewSystem = realmMgtRes.roles().get(AdminRoles.VIEW_SYSTEM).toRepresentation();
        UserRepresentation userRep = adminClient.realm(REALM_NAME).users().search("view-realm", Boolean.TRUE).get(0);
        UserResource userRes = adminClient.realm(REALM_NAME).users().get(userRep.getId());
        userRes.roles().clientLevel(realmMgtRep.getId()).add(Collections.singletonList(viewSystem));
        try (Keycloak keycloak = adminClientFactory.create().realm(REALM_NAME)
                .username(userRep.getUsername()).password("password").clientId("test-client")
                .build()) {
            serverInfo = keycloak.serverInfo().getInfo();
            Assert.assertNotNull(serverInfo.getSystemInfo());
            Assert.assertNotNull(serverInfo.getCpuInfo());
            Assert.assertNotNull(serverInfo.getMemoryInfo());
        } finally {
            userRes.roles().clientLevel(realmMgtRep.getId()).remove(Collections.singletonList(viewSystem));
            realmMgtRes.roles().get(AdminRoles.VIEW_SYSTEM).remove();
        }
    }

    private void verifyAnyAdminRoleReqired(Invocation invocation) {
        invoke(invocation, clients.get("view-realm"), true);
        invoke(invocation, clients.get("manage-realm"), true);
        invoke(invocation, clients.get("multi"), true);
        invoke(invocation, clients.get("master-admin"), true);
        invoke(invocation, clients.get("none"), false);
        invoke(invocation, clients.get("REALM2"), false);
    }
}

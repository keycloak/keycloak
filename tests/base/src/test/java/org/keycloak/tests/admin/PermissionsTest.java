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

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.hamcrest.Matchers;
import org.jgroups.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.TestLdapConnectionRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.resources.admin.AdminAuth.Resource;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.ApiUtil;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.FederatedIdentityBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.RoleBuilder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.keycloak.services.resources.admin.AdminAuth.Resource.AUTHORIZATION;
import static org.keycloak.services.resources.admin.AdminAuth.Resource.CLIENT;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest(config = PermissionsTest.PermissionsTestServerConfig.class)
public class PermissionsTest {

    @InjectRealm(config = PermissionsTestRealmConfig1.class, ref = "realm1")
    ManagedRealm managedRealm1;

    @InjectRealm(config = PermissionsTestRealmConfig2.class, ref = "realm2")
    ManagedRealm managedRealm2;

    @InjectRealm(attachTo = "master", ref = "masterRealm")
    ManagedRealm managedMasterRealm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    private static final String REALM_NAME = "permissions-test";

    private Map<String, Keycloak> clients = new HashMap<>();

    @BeforeEach
    public void beforeMethod() { // todo rewrite
        Response response = managedMasterRealm.admin().users().create(UserConfigBuilder.create()
                .username("permissions-test-master-none")
                .password("password")
                .build()
        );
        String userUuid = ApiUtil.getCreatedId(response);
        managedMasterRealm.cleanup().add(r -> r.users().delete(userUuid));

        for (String role : AdminRoles.ALL_REALM_ROLES) {
            response = managedMasterRealm.admin().users().create(UserConfigBuilder.create()
                    .username("permissions-test-master-" + role)
                    .password("password")
                    .build());
            String roleUserUuid = ApiUtil.getCreatedId(response);
            managedMasterRealm.cleanup().add(r -> r.users().delete(roleUserUuid));

            String clientUuid = managedMasterRealm.admin().clients().findByClientId(REALM_NAME + "-realm").get(0).getId();
            RoleRepresentation roleRep = managedMasterRealm.admin().clients().get(clientUuid).roles().get(role).toRepresentation();
            managedMasterRealm.admin().users().get(roleUserUuid).roles().clientLevel(clientUuid).add(Collections.singletonList(roleRep));
        }

        clients.put(AdminRoles.REALM_ADMIN,
                adminClientFactory.create().realm(REALM_NAME).username(AdminRoles.REALM_ADMIN).password("password").clientId("test-client").clientSecret("secret").build());

        clients.put("none",
                adminClientFactory.create().realm(REALM_NAME).username("none").password("password").clientId("test-client").clientSecret("secret").build());

        clients.put("multi",
                adminClientFactory.create().realm(REALM_NAME).username("multi").password("password").clientId("test-client").clientSecret("secret").build());

        for (String role : AdminRoles.ALL_REALM_ROLES) {
            clients.put(role, adminClientFactory.create().realm(REALM_NAME).username(role).password("password").clientId("test-client").build());
        }

        clients.put("REALM2", adminClientFactory.create().realm("realm2").username("admin").password("password").clientId("test-client").build());

        clients.put("master-admin", adminClient); // todo maybe dont put it in the list

        clients.put("master-none", adminClientFactory.create().realm("master").username("permissions-test-master-none").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build());

        for (String role : AdminRoles.ALL_REALM_ROLES) {
            clients.put("master-" + role,
                    adminClientFactory.create().realm("master").username("permissions-test-master-" + role).password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build());
        }

    }

    @Test
    public void realms() throws Exception {
        // Check returned realms
        invoke((RealmResource realm) -> clients.get("master-none").realms().findAll(), clients.get("none"), false);
        invoke((RealmResource realm) -> clients.get("none").realms().findAll(), clients.get("none"), false);
        Assert.assertNames(clients.get("master-admin").realms().findAll(), "master", REALM_NAME, "realm2");
        Assert.assertNames(clients.get(AdminRoles.REALM_ADMIN).realms().findAll(), REALM_NAME);
        Assert.assertNames(clients.get("REALM2").realms().findAll(), "realm2");

        // Check realm only contains name if missing view realm permission
        List<RealmRepresentation> realms = clients.get(AdminRoles.VIEW_USERS).realms().findAll();
        Assert.assertNames(realms, REALM_NAME);
        assertGettersEmpty(realms.get(0));

        realms = clients.get(AdminRoles.VIEW_REALM).realms().findAll();
        Assert.assertNames(realms, REALM_NAME);
        assertNotNull(realms.get(0).getAccessTokenLifespan());

        // Check the same when access with users from 'master' realm
        realms = clients.get("master-" + AdminRoles.VIEW_USERS).realms().findAll();
        Assert.assertNames(realms, REALM_NAME);
        assertGettersEmpty(realms.get(0));

        realms = clients.get("master-" + AdminRoles.VIEW_REALM).realms().findAll();
        Assert.assertNames(realms, REALM_NAME);
        assertNotNull(realms.get(0).getAccessTokenLifespan());

        // Create realm
        invoke(realm -> clients.get("master-admin").realms().create(RealmConfigBuilder.create().name("master").build()),
                adminClient, true);
        invoke(realm -> clients.get("master-" + AdminRoles.MANAGE_USERS).realms().create(RealmConfigBuilder.create().name("master").build()),
                adminClient, false);
        invoke(realm -> clients.get(AdminRoles.REALM_ADMIN).realms().create(RealmConfigBuilder.create().name("master").build()),
                adminClient, false);

        // Get realm
        invoke(RealmResource::toRepresentation, Resource.REALM, false, true);

        RealmRepresentation realm = clients.get(AdminRoles.QUERY_REALMS).realm(REALM_NAME).toRepresentation();
        assertGettersEmpty(realm);
        assertNull(realm.isRegistrationEmailAsUsername());
        assertNull(realm.getAttributes());

        realm = clients.get(AdminRoles.VIEW_USERS).realm(REALM_NAME).toRepresentation();
        assertNotNull(realm.isRegistrationEmailAsUsername());

        realm = clients.get(AdminRoles.MANAGE_USERS).realm(REALM_NAME).toRepresentation();
        assertNotNull(realm.isRegistrationEmailAsUsername());

        // query users only if granted through fine-grained admin
        realm = clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).toRepresentation();
        assertNull(realm.isRegistrationEmailAsUsername());
        assertNull(realm.getAttributes());

        // this should pass given that users granted with "query" roles are allowed to access the realm with limited access
        for (String role : AdminRoles.ALL_QUERY_ROLES) {
            invoke(realmm -> clients.get(role).realms().realm(REALM_NAME).toRepresentation(), clients.get(role), true);
        }

        invoke(realm1 -> realm1.update(new RealmRepresentation()), Resource.REALM, true);
        invoke(RealmResource::pushRevocation, Resource.REALM, true);
        invoke(realm4 -> realm4.deleteSession("nosuch", false), Resource.USER, true);
        invoke(RealmResource::getClientSessionStats, Resource.REALM, false);

        invoke(RealmResource::getDefaultGroups, Resource.REALM, false);
        invoke(realm7 -> realm7.addDefaultGroup("nosuch"), Resource.REALM, true);
        invoke(realm9 -> realm9.removeDefaultGroup("nosuch"), Resource.REALM, true);
        GroupRepresentation newGroup = new GroupRepresentation();
        newGroup.setName("sample");
        adminClient.realm(REALM_NAME).groups().add(newGroup);
        GroupRepresentation group = adminClient.realms().realm(REALM_NAME).getGroupByPath("sample");

        invoke(realm2 -> realm2.getGroupByPath("sample"), Resource.USER, false);

        adminClient.realms().realm(REALM_NAME).groups().group(group.getId()).remove();

        invoke((realm5, response) -> {
            TestLdapConnectionRepresentation config = new TestLdapConnectionRepresentation(
                    "nosuch", "nosuch", "nosuch", "nosuch", "nosuch", "nosuch");
            response.set(realm5.testLDAPConnection(config.getAction(), config.getConnectionUrl(), config.getBindDn(),
                    config.getBindCredential(), config.getUseTruststoreSpi(), config.getConnectionTimeout()));
            response.set(realm5.testLDAPConnection(config));
        }, Resource.REALM, true);

        invoke((realm3, response) ->
                        response.set(realm3.partialImport(new PartialImportRepresentation())),
                Resource.REALM, true);

        invoke(RealmResource::clearRealmCache, Resource.REALM, true);
        invoke(RealmResource::clearUserCache, Resource.REALM, true);

        // Delete realm
        invoke(realm6 -> clients.get("master-admin").realms().realm("nosuch").remove(), adminClient, true);
        invoke(realm8 -> clients.get("REALM2").realms().realm(REALM_NAME).remove(), adminClient, false);
        invoke(realm11 -> clients.get(AdminRoles.MANAGE_USERS).realms().realm(REALM_NAME).remove(), adminClient, false);
        invoke(realm10 -> clients.get(AdminRoles.REALM_ADMIN).realms().realm(REALM_NAME).remove(), adminClient, true);

        // Revert realm removal
//        recreatePermissionRealm();
    }

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
    public void clientAuthorization() {
        String fooAuthzClientUuid = ApiUtil.getCreatedId(managedRealm1.admin().clients().create(ClientConfigBuilder.create().clientId("foo-authz").build()));
        ClientRepresentation foo = managedRealm1.admin().clients().get(fooAuthzClientUuid).toRepresentation();

        invoke((realm, response) -> {
            foo.setServiceAccountsEnabled(true);
            foo.setAuthorizationServicesEnabled(true);
            realm.clients().get(foo.getId()).update(foo);
        }, CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).authorization().getSettings(), AUTHORIZATION, false);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            ResourceServerRepresentation settings = authorization.getSettings();
            authorization.update(settings);
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.resources().resources();
        }, AUTHORIZATION, false);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.scopes().scopes();
        }, AUTHORIZATION, false);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.policies().policies();
        }, AUTHORIZATION, false);
        invoke((realm, response) -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            response.set(authorization.resources().create(new ResourceRepresentation("Test", Collections.emptySet())));
        }, AUTHORIZATION, true);
        invoke((realm, response) -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            response.set(authorization.scopes().create(new ScopeRepresentation("Test")));
        }, AUTHORIZATION, true);
        invoke((realm, response) -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();
            representation.setName("Test PermissionsTest");
            representation.addResource("Default Resource");
            response.set(authorization.permissions().resource().create(representation));
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.resources().resource("nosuch").update(new ResourceRepresentation());
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.scopes().scope("nosuch").update(new ScopeRepresentation());
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.policies().policy("nosuch").update(new PolicyRepresentation());
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.resources().resource("nosuch").remove();
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.scopes().scope("nosuch").remove();
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.policies().policy("nosuch").remove();
        }, AUTHORIZATION, true);
    }

    @Test
    public void roles() {
        RoleRepresentation newRole = RoleBuilder.create().name("sample-role").build();
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
    public void flows() throws Exception {
        invoke(realm -> realm.flows().getFormProviders(), Resource.REALM, false);
        invoke(realm -> realm.flows().getAuthenticatorProviders(), Resource.REALM, false);
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), Resource.REALM, false, true);
        invoke(realm -> realm.flows().getFormActionProviders(), Resource.REALM, false);
        invoke(realm -> realm.flows().getFlows(), Resource.REALM, false, true);
        invoke((realm, response) -> response.set(realm.flows().createFlow(new AuthenticationFlowRepresentation())), Resource.REALM, true);
        invoke(realm -> realm.flows().getFlow("nosuch"), Resource.REALM, false);
        invoke(realm -> realm.flows().deleteFlow("nosuch"), Resource.REALM, true);
        invoke((realm, response) -> response.set(realm.flows().copy("nosuch", Map.of())), Resource.REALM, true);
        invoke(realm -> realm.flows().addExecutionFlow("nosuch", Map.of()), Resource.REALM, true);
        invoke(realm -> realm.flows().addExecution("nosuch", Map.of()), Resource.REALM, true);
        invoke(realm -> realm.flows().getExecutions("nosuch"), Resource.REALM, false);
        invoke((RealmResource realm) -> realm.flows().getExecution("nosuch"), Resource.REALM, false);
        invoke(realm -> realm.flows().updateExecutions("nosuch", new AuthenticationExecutionInfoRepresentation()), Resource.REALM, true);
        invoke((realm, response) -> {
            AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
            rep.setAuthenticator("auth-cookie");
            rep.setRequirement("CONDITIONAL");
            response.set(realm.flows().addExecution(rep));
        }, Resource.REALM, true);
        invoke(realm -> realm.flows().raisePriority("nosuch"), Resource.REALM, true);
        invoke(realm -> realm.flows().lowerPriority("nosuch"), Resource.REALM, true);
        invoke(realm -> realm.flows().removeExecution("nosuch"), Resource.REALM, true);
        invoke((realm, response) ->
                response.set(realm.flows().newExecutionConfig("nosuch", new AuthenticatorConfigRepresentation())), Resource.REALM, true);
        invoke(realm -> realm.flows().getAuthenticatorConfig("nosuch"), Resource.REALM, false);
        invoke(realm -> realm.flows().getUnregisteredRequiredActions(), Resource.REALM, false);
        invoke(realm -> realm.flows().registerRequiredAction(new RequiredActionProviderSimpleRepresentation()), Resource.REALM, true);
        invoke(realm -> realm.flows().getRequiredActions(), Resource.REALM, false, true);
        invoke(realm -> realm.flows().getRequiredAction("nosuch"), Resource.REALM, false);
        invoke(realm -> realm.flows().removeRequiredAction("nosuch"), Resource.REALM, true);
        invoke(realm -> realm.flows().updateRequiredAction("nosuch", new RequiredActionProviderRepresentation()), Resource.REALM, true);
        invoke(realm -> realm.flows().getAuthenticatorConfigDescription("nosuch"), Resource.REALM, false);
        invoke(realm -> realm.flows().getPerClientConfigDescription(), Resource.REALM, false, true);
        invoke(realm -> realm.flows().getAuthenticatorConfig("nosuch"), Resource.REALM, false);
        invoke(realm -> realm.flows().removeAuthenticatorConfig("nosuch"), Resource.REALM, true);
        invoke(realm -> realm.flows().updateAuthenticatorConfig("nosuch", new AuthenticatorConfigRepresentation()), Resource.REALM, true);
        invoke(realm -> {
            clients.get(AdminRoles.VIEW_REALM).realm(REALM_NAME).flows().getPerClientConfigDescription();
            clients.get(AdminRoles.VIEW_REALM).realm(REALM_NAME).flows().getClientAuthenticatorProviders();
            clients.get(AdminRoles.VIEW_REALM).realm(REALM_NAME).flows().getRequiredActions();
        }, adminClient, true);

        // Re-create realm
//        adminClient.realm(REALM_NAME).remove();
//
//        recreatePermissionRealm();
    }

    @Test
    public void rolesById() {
        RoleRepresentation newRole = RoleBuilder.create().name("role-by-id").build();
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
        // users with query-user role should be able to query clients so the user detail page can be rendered successfully when fine-grained permissions are enabled.
        // if the admin wants to restrict the clients that an user can see he can define permissions for these clients
        invoke(realm -> clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).clients().findAll(), clients.get(AdminRoles.QUERY_USERS), true);
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

    private void verifyAnyAdminRoleReqired(Invocation invocation) {
        invoke(invocation, clients.get("view-realm"), true);
        invoke(invocation, clients.get("manage-realm"), true);
        invoke(invocation, clients.get("multi"), true);
        invoke(invocation, clients.get("master-admin"), true);
        invoke(invocation, clients.get("none"), false);
        invoke(invocation, clients.get("REALM2"), false);
    }

    private void invoke(final Invocation invocation, Resource resource, boolean manage) {
        invoke((realm, response) ->
                        invocation.invoke(realm),
                resource, manage);
    }

    private void invoke(final Invocation invocation, Resource resource, boolean manage, boolean skipDifferentRole) {
        invoke((realm, response) ->
                        invocation.invoke(realm),
                resource, manage, skipDifferentRole);
    }

    private void invoke(InvocationWithResponse invocation, Resource resource, boolean manage) {
        invoke(invocation, resource, manage, false);
    }

    private void invoke(InvocationWithResponse invocation, Resource resource, boolean manage, boolean skipDifferentRole) {
        String viewRole = getViewRole(resource);
        String manageRole = getManageRole(resource);
        String differentViewRole = getDifferentViewRole(resource);
        String differentManageRole = getDifferentManageRole(resource);

        invoke(invocation, clients.get("master-none"), false);
        invoke(invocation, clients.get("master-admin"), true);
        invoke(invocation, clients.get("master-" + viewRole), !manage);
        invoke(invocation, clients.get("master-" + manageRole), true);
        if (!skipDifferentRole) {
            invoke(invocation, clients.get("master-" + differentViewRole), false);
            invoke(invocation, clients.get("master-" + differentManageRole), false);
        }

        invoke(invocation, clients.get("none"), false);
        invoke(invocation, clients.get(AdminRoles.REALM_ADMIN), true);
        invoke(invocation, clients.get(viewRole), !manage);
        invoke(invocation, clients.get(manageRole), true);
        if (!skipDifferentRole) {
            invoke(invocation, clients.get(differentViewRole), false);
            invoke(invocation, clients.get(differentManageRole), false);
        }

        invoke(invocation, clients.get("REALM2"), false);
    }

    private void invoke(final Invocation invocation, Keycloak client, boolean expectSuccess) {
        invoke((realm, response) ->
                        invocation.invoke(realm),
                client, expectSuccess);
    }

    private void invoke(InvocationWithResponse invocation, Keycloak client, boolean expectSuccess) {
        int statusCode;
        try {
            AtomicReference<Response> responseReference = new AtomicReference<>();
            invocation.invoke(client.realm(REALM_NAME), responseReference);
            Response response = responseReference.get();
            if (response != null) {
                statusCode = response.getStatus();
            } else {
                // OK (we don't care about the exact status code
                statusCode = 200;
            }
        } catch (ClientErrorException e) {
            statusCode = e.getResponse().getStatus();
        }

        if (expectSuccess) {
            if (!(statusCode == 200 || statusCode == 201 || statusCode == 204 || statusCode == 404 || statusCode == 409 || statusCode == 400)) {
                fail("Expected permitted, but was " + statusCode);
            }
        } else {
            if (statusCode != 403) {
                fail("Expected 403, but was " + statusCode);
            }
        }
    }

    private String getViewRole(Resource resource) {
        return switch (resource) {
            case CLIENT -> AdminRoles.VIEW_CLIENTS;
            case USER -> AdminRoles.VIEW_USERS;
            case REALM -> AdminRoles.VIEW_REALM;
            case EVENTS -> AdminRoles.VIEW_EVENTS;
            case IDENTITY_PROVIDER -> AdminRoles.VIEW_IDENTITY_PROVIDERS;
            case AUTHORIZATION -> AdminRoles.VIEW_AUTHORIZATION;
            default -> throw new RuntimeException("Unexpected resource");
        };
    }

    private String getManageRole(Resource resource) {
        return switch (resource) {
            case CLIENT -> AdminRoles.MANAGE_CLIENTS;
            case USER -> AdminRoles.MANAGE_USERS;
            case REALM -> AdminRoles.MANAGE_REALM;
            case EVENTS -> AdminRoles.MANAGE_EVENTS;
            case IDENTITY_PROVIDER -> AdminRoles.MANAGE_IDENTITY_PROVIDERS;
            case AUTHORIZATION -> AdminRoles.MANAGE_AUTHORIZATION;
            default -> throw new RuntimeException("Unexpected resource");
        };
    }

    private String getDifferentViewRole(Resource resource) {
        return switch (resource) {
            case CLIENT -> AdminRoles.VIEW_USERS;
            case USER -> AdminRoles.VIEW_CLIENTS;
            case REALM -> AdminRoles.VIEW_EVENTS;
            case EVENTS, AUTHORIZATION -> AdminRoles.VIEW_IDENTITY_PROVIDERS;
            case IDENTITY_PROVIDER -> AdminRoles.VIEW_REALM;
            default -> throw new RuntimeException("Unexpected resouce");
        };
    }

    private String getDifferentManageRole(Resource resource) {
        return switch (resource) {
            case CLIENT -> AdminRoles.MANAGE_USERS;
            case USER -> AdminRoles.MANAGE_CLIENTS;
            case REALM -> AdminRoles.MANAGE_EVENTS;
            case EVENTS, AUTHORIZATION -> AdminRoles.MANAGE_IDENTITY_PROVIDERS;
            case IDENTITY_PROVIDER -> AdminRoles.MANAGE_REALM;
            default -> throw new RuntimeException("Unexpected resouce");
        };
    }

    public interface Invocation {

        void invoke(RealmResource realm);

    }

    public interface InvocationWithResponse {

        void invoke(RealmResource realm, AtomicReference<Response> response);

    }

    private void assertGettersEmpty(RealmRepresentation rep) {
        assertGettersEmpty(rep, "getRealm", "getAttributesOrEmpty", "getDisplayNameHtml",
                "getDisplayName", "getDefaultLocale", "getSupportedLocales");
    }

    private void assertGettersEmpty(Object rep, String... ignore) {
        List<String> ignoreList = Arrays.asList(ignore);

        for (Method m : rep.getClass().getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && m.getName().startsWith("get") && !ignoreList.contains(m.getName())) {
                try {
                    Object o = m.invoke(rep);
                    assertNull(o, "Expected " + m.getName() + " to be null");
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        }
    }

    private static class PermissionsTestRealmConfig1 implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(REALM_NAME);
            realm.addClient("test-client")
                    .enabled(true)
                    .publicClient(true)
                    .directAccessGrants();

            realm.addUser(AdminRoles.REALM_ADMIN)
                    .name("realm-admin", "realm-admin")
                    .email("realmadmin@localhost.com")
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            realm.addUser("multi")
                    .name("multi", "multi")
                    .email("multi@localhost.com")
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.QUERY_GROUPS, AdminRoles.MANAGE_REALM, AdminRoles.VIEW_CLIENTS);

            realm.addUser("none")
                    .name("none", "none")
                    .email("none@localhost.com")
                    .password("password");

            for (String role : AdminRoles.ALL_REALM_ROLES) {
                realm.addUser(role)
                        .name(role, role)
                        .email(role + "@localhost.com")
                        .password("password")
                        .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, role);
            }

            realm.addUser("admin")
                    .name("admin", "admin")
                    .email("admin" + "@localhost.com")
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            realm.smtp(MailServerConfiguration.HOST, Integer.parseInt(MailServerConfiguration.PORT), MailServerConfiguration.FROM);

            return realm;
        }
    }

    private static class PermissionsTestRealmConfig2 implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name("realm2");

            realm.addClient("test-client")
                    .publicClient(true)
                    .directAccessGrants();

            realm.addUser("admin")
                    .name("admin", "admin")
                    .email("admin" + "@localhost.com")
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            return realm;
        }
    }

    static class PermissionsTestServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.AUTHORIZATION);
        }
    }
}

/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.zerodowntime;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.common.util.Retry;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.CockroachdbContainerProvider;
import org.keycloak.testsuite.arquillian.HotRodContainerProvider;
import org.keycloak.testsuite.arquillian.LegacyKeycloakContainerProvider;
import org.keycloak.testsuite.arquillian.LegacyKeycloakContainerProvider.ZeroDowntimeContainer;
import static org.keycloak.testsuite.arquillian.LegacyKeycloakContainerProvider.ZeroDowntimeContainer.COCKROACH;
import static org.keycloak.testsuite.arquillian.LegacyKeycloakContainerProvider.ZeroDowntimeContainer.HOTROD;
import static org.keycloak.testsuite.arquillian.LegacyKeycloakContainerProvider.ZeroDowntimeContainer.NONE;
import static org.keycloak.testsuite.arquillian.LegacyKeycloakContainerProvider.ZeroDowntimeContainer.POSTGRES;
import org.keycloak.testsuite.arquillian.PostgresContainerProvider;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.updaters.SetSystemProperty;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.InfinispanContainer;
import org.keycloak.testsuite.util.LegacyKeycloakContainer;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * It tests that object stored by legacy version of keycloak could be read by
 * current one, then object is modified, therefore stored in current version and
 * then it's verified it could be read by legacy keycloak.
 *
 * See HOW-TO-RUN.md for some additional notes.
 *
 * @author vramik
 */
public class ZeroDowntimeTest extends AbstractKeycloakTest {

    @ArquillianResource private ContainerController controller;
    @ArquillianResource private LegacyKeycloakContainerProvider legacyKeycloakServerProvider;
    @ArquillianResource private PostgresContainerProvider postgresProvider;
    @ArquillianResource private CockroachdbContainerProvider cockroachdbProvider;
    @ArquillianResource private HotRodContainerProvider hotrodProvider;

    private final String legacyKeycloakHost = System.getProperty("keycloak.legacy.host", "localhost");
    private final String legacyKeycloakPort = System.getProperty("keycloak.legacy.port", "8091");
    private final String legacyKeycloakUrl = "http://" + legacyKeycloakHost + ":" + legacyKeycloakPort;

    private String currentAuthServer;
    private SetSystemProperty sysProp;
    private static boolean initialized;
    private static Keycloak legacyAdminClient;

    // junit4 enforces to use static method when using @BeforeClass or @ClassRule
    // TODO: after upgrade to junit5: replace @Before with @BeforeAll and get rid of the "initialized" flag
    @Before
    public void before() throws Exception {
        Assume.assumeFalse(LegacyKeycloakContainerProvider.CONTAINER.equals(ZeroDowntimeContainer.NONE));

        if (initialized) return;

        currentAuthServer = suiteContext.getAuthServerInfo().getArquillianContainer().getName();

        // stop current auth-server
        if (controller.isStarted(currentAuthServer)) {
            log.debug("Stopping auth server");
            controller.stop(currentAuthServer);
        }

        // stop and create new container
        handleContainer();

        //start legacy container
        LegacyKeycloakContainer legacyKeycloakServer = legacyKeycloakServerProvider.get();
        legacyKeycloakServer.start();
        legacyAdminClient = KeycloakBuilder.builder()
                .serverUrl(legacyKeycloakUrl)
                .realm(AuthRealm.MASTER)
                .username(AuthRealm.ADMIN)
                .password(AuthRealm.ADMIN)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .resteasyClient(AdminClientUtil.createResteasyClient())
                .build();

        // import test realm
        legacyAdminClient.realms().create(RealmBuilder.create()
                .name(AuthRealm.TEST)
                .attribute("attr", "val")
                .client(ClientBuilder.create()
                    .clientId("client"))
                .clientScope(ClientScopeBuilder.create().name("client_scope"))
                .roles(RolesBuilder.create()
                        .clientRole("client", RoleBuilder.create().name("client_role").singleAttribute("attr", "val").build())
                        .realmRole(RoleBuilder.create().name("realm_role").singleAttribute("attr", "val").build()))
                .group(GroupBuilder.create().name("group").build())
                .user(UserBuilder.create().username("user").addAttribute("attr", "val"))
                .build());

        // start current auth-server
        controller.start(currentAuthServer);
        reconnectAdminClient();

        testContext.registerAfterClassAction(() -> {
            legacyKeycloakServer.stop();
            if (sysProp != null) sysProp.revert();
        });

        initialized = true;
    }

    private void handleContainer() {
        switch (LegacyKeycloakContainerProvider.CONTAINER) {
            case POSTGRES:
                handlePostgres();
                break;
            case COCKROACH:
                handleCockroach();
                break;
            case HOTROD:
                handleHotRod();
                break;
            case NONE:
            default:
                throw new IllegalStateException("Unknown container!");
        }
    }

    private void handlePostgres() {
        PostgreSQLContainer postgres = postgresProvider.getContainer();
        //if suite container is running stop it
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
        if (Boolean.parseBoolean(System.getProperty("postgres.start-container-for-zero-downtime", "true"))) {
            postgres = postgresProvider.createContainer();
            postgres.start();

            log.infof("DatabaseInfo: %s, user=%s, pass=%s", postgres.getJdbcUrl(), PostgresContainerProvider.POSTGRES_DB_USER, PostgresContainerProvider.POSTGRES_DB_PASSWORD);

            sysProp = new SetSystemProperty("keycloak.map.storage.connectionsJpa.url", postgres.getJdbcUrl());
        }
    }

    private void handleCockroach() {
        final CockroachContainer cockroach = cockroachdbProvider.getContainer();
        //if suite container is running stop it
        if (cockroach != null && cockroach.isRunning()) {
            cockroach.stop();
        
            //it needs some time to stop
            Retry.executeWithBackoff(i -> {
                if (cockroach.isRunning()) { 
                    throw new AssertionError(String.format("Stopping CockroachDB container was not successful. Number of attempts: %d", i));
                }
            }, Duration.ofMinutes(1), 50);
        }

        // spawn new container
        if (Boolean.parseBoolean(System.getProperty("cockroachdb.start-container-for-zero-downtime", "true"))) {
            CockroachContainer newCockroach = cockroachdbProvider.createContainer();
            newCockroach.withStartupAttempts(3); // with podman it sometimes fails with com.sun.jna.LastErrorException: https://github.com/testcontainers/testcontainers-java/issues/6640
            newCockroach.start();

            log.infof("DatabaseInfo: %s, user=%s, pass=%s", newCockroach.getJdbcUrl(), CockroachdbContainerProvider.COCKROACHDB_DB_USER, CockroachdbContainerProvider.COCKROACHDB_DB_PASSWORD);

            sysProp = new SetSystemProperty("keycloak.map.storage.connectionsJpa.url", newCockroach.getJdbcUrl());

           /* 
            * CRDB container fails to connect intermitently with: 
            * org.postgresql.util.PSQLException: FATAL: password authentication failed for user "keycloak"
            *
            * It seems it needs some time to set correct permission for a user after fresh start
            * Waiting for successful connection.
            */
            Retry.executeWithBackoff(i -> {
                try {
                    DriverManager.getConnection(newCockroach.getJdbcUrl(), CockroachdbContainerProvider.COCKROACHDB_DB_USER, CockroachdbContainerProvider.COCKROACHDB_DB_PASSWORD);
                } catch (SQLException e) {
                    throw new AssertionError(String.format("Establishing connection was not successful. Number of attempts: %d", i));
                }
            }, Duration.ofMinutes(1), 500);
        }
    }

    private void handleHotRod() {
        InfinispanContainer hotRodContainer = hotrodProvider.getContainer();
        //if suite container is running stop it
        if (hotRodContainer != null && hotRodContainer.isRunning()) {
            hotRodContainer.stop();
        }
        // spawn new container
        if (Boolean.parseBoolean(System.getProperty("start-hotrod-container-for-zero-downtime", "true"))) {
            hotRodContainer = new InfinispanContainer();
            hotRodContainer.start();

            sysProp = new SetSystemProperty(HotRodContainerProvider.HOT_ROD_STORE_HOST_PROPERTY, hotRodContainer.getHost());
        }
    }

    @Test
    public void realmSmokeTest() throws Exception {
        //realm
        RealmRepresentation testRealmRep = adminClient.realm(AuthRealm.TEST).toRepresentation();
        String realmAttribute = testRealmRep.getAttributes().get("attr");
        assertThat(realmAttribute, equalTo("val"));
        adminClient.realm(AuthRealm.TEST).update(RealmBuilder.edit(testRealmRep).displayName("displayName").build());

        //verify objects can be read by legacy server
        RealmRepresentation returnedRealmRep = legacyAdminClient.realm(AuthRealm.TEST).toRepresentation();
        assertThat(returnedRealmRep.getDisplayName(), is("displayName"));
        assertThat(returnedRealmRep.getAttributes().get("attr"), is("val"));
    }

    @Test
    public void clientSmokeTest () throws Exception {
        //client
        List<ClientRepresentation> clients = adminClient.realm(AuthRealm.TEST).clients().findByClientId("client");
        assertThat(clients, hasSize(1));
        ClientRepresentation clientRep = clients.get(0);
        adminClient.realm(AuthRealm.TEST).clients().get(clientRep.getId()).update(ClientBuilder.edit(clientRep).addRedirectUri("*").build());

        //client role
        List<RoleRepresentation> clientRoles = adminClient.realm(AuthRealm.TEST).clients().get(clientRep.getId()).roles().list();
        assertThat(clientRoles, hasSize(1));
        RoleRepresentation clientRoleRep = clientRoles.get(0);
        assertThat(clientRoleRep.getName(), is("client_role"));
        adminClient.realm(AuthRealm.TEST).clients().get(clientRep.getId()).roles().get(clientRoleRep.getName()).update(RoleBuilder.edit(clientRoleRep).description("desc").build());

        //verify objects can be read by legacy server
        ClientRepresentation returnedClientRep = legacyAdminClient.realm(AuthRealm.TEST).clients().get(clientRep.getId()).toRepresentation();
        assertThat(returnedClientRep.getRedirectUris(), hasItem("*"));

        RoleRepresentation returnedClientRoleRep = legacyAdminClient.realm(AuthRealm.TEST).clients().get(clientRep.getId()).roles().get(clientRoleRep.getName()).toRepresentation();
        assertThat(returnedClientRoleRep.getDescription(), is("desc"));
        assertThat(returnedClientRoleRep.getAttributes().get("attr"), hasItem("val"));
    }

    @Test
    public void clientScopeSmokeTest () throws Exception {
        //client scope
        ClientScopeRepresentation clientScopeRep = adminClient.realm(AuthRealm.TEST).clientScopes().findAll().stream().filter(scope -> scope.getName().equals("client_scope")).findFirst().orElse(null);
        assertThat(clientScopeRep, notNullValue());
        adminClient.realm(AuthRealm.TEST).clientScopes().get(clientScopeRep.getId()).update(ClientScopeBuilder.edit(clientScopeRep).description("desc").build());

        //verify objects can be read by legacy server
        ClientScopeRepresentation returnedClientScopeRep = legacyAdminClient.realm(AuthRealm.TEST).clientScopes().get(clientScopeRep.getId()).toRepresentation();
        assertThat(returnedClientScopeRep.getDescription(), is("desc"));
    }

    @Test
    public void userRoleSmokeTest() throws Exception {
        //user
        List<UserRepresentation> users = adminClient.realm(AuthRealm.TEST).users().searchByUsername("user", Boolean.TRUE);
        assertThat(users, hasSize(1));
        UserRepresentation userRep = users.get(0);
        adminClient.realm(AuthRealm.TEST).users().get(userRep.getId()).update(UserBuilder.edit(userRep).email("test@test").build());

        //verify objects can be read by legacy server
        UserRepresentation returnedUserRep = legacyAdminClient.realm(AuthRealm.TEST).users().get(userRep.getId()).toRepresentation();
        assertThat(returnedUserRep.getEmail(), is("test@test"));
        assertThat(returnedUserRep.getAttributes().get("attr"), hasItem("val"));
    }

    @Test
    public void roleSmokeTest() throws Exception {
        //realm role
        RoleRepresentation realmRoleRep = adminClient.realm(AuthRealm.TEST).roles().get("realm_role").toRepresentation();
        adminClient.realm(AuthRealm.TEST).roles().get("realm_role").update(RoleBuilder.edit(realmRoleRep).description("desc").build());

        //verify objects can be read by legacy server
        RoleRepresentation returnedRoleRep = legacyAdminClient.realm(AuthRealm.TEST).roles().get(realmRoleRep.getName()).toRepresentation();
        assertThat(returnedRoleRep.getDescription(), is("desc"));
        assertThat(returnedRoleRep.getAttributes().get("attr"), hasItem("val"));
    }

    @Test
    public void groupSmokeTest() throws Exception {
        //group
        List<GroupRepresentation> groups = adminClient.realm(AuthRealm.TEST).groups().groups();
        assertThat(groups, hasSize(1));
        GroupRepresentation groupRep = groups.get(0);
        adminClient.realm(AuthRealm.TEST).groups().group(groupRep.getId()).update(GroupBuilder.edit(groupRep).singleAttribute("attr", "val").build());

        //verify objects can be read by legacy server
        GroupRepresentation returnedGroupRep = legacyAdminClient.realm(AuthRealm.TEST).groups().group(groupRep.getId()).toRepresentation();
        assertThat(returnedGroupRep.getAttributes().get("attr"), hasItem("val"));
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }
}

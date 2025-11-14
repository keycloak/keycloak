package org.keycloak.tests.admin.partialimport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.models.UserModel;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AbstractPartialImportTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD, config = PartialImportRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectClient(ref = "clientRolesClient", config = PartialImportRolesClientConfig.class)
    ManagedClient rolesClient;

    @InjectClient(ref = "clientServiceAccount", config = PartialImportServiceClientConfig.class)
    ManagedClient serviceClient;

    @InjectAdminEvents
    AdminEvents adminEvents;

    private static final String CLIENT_ROLES_CLIENT = "clientRolesClient";
    protected static final String CLIENT_SERVICE_ACCOUNT = "clientServiceAccount";
    protected static final String USER_PREFIX = "user";
    private static final String GROUP_PREFIX = "group";
    protected static final String CLIENT_PREFIX = "client";
    protected static final String REALM_ROLE_PREFIX = "realmRole";
    protected static final String CLIENT_ROLE_PREFIX = "clientRole";
    protected static final String[] IDP_ALIASES = {"twitter", "github", "facebook", "google", "linkedin-openid-connect", "microsoft", "stackoverflow"};
    protected static final int NUM_ENTITIES = IDP_ALIASES.length;
    private static final ResourceServerRepresentation resourceServerSampleSettings;

    protected PartialImportRepresentation piRep;

    static {
        try {
            resourceServerSampleSettings = JsonSerialization.readValue(
                    AbstractPartialImportTest.class.getResourceAsStream("sample-authz-partial-import.json"),
                    ResourceServerRepresentation.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load sample resource server configuration", e);
        }
    }

    @BeforeEach
    public void init() {
        piRep = new PartialImportRepresentation();
    }

    protected void setFail() {
        piRep.setIfResourceExists(PartialImportRepresentation.Policy.FAIL.toString());
    }

    protected void setSkip() {
        piRep.setIfResourceExists(PartialImportRepresentation.Policy.SKIP.toString());
    }

    protected void setOverwrite() {
        piRep.setIfResourceExists(PartialImportRepresentation.Policy.OVERWRITE.toString());
    }

    protected PartialImportResults doImport() {

        try (Response response = managedRealm.admin().partialImport(piRep)) {
            return response.readEntity(PartialImportResults.class);
        }
    }

    protected void addUsers() {
        List<UserRepresentation> users = new ArrayList<>();

        for (int i = 0; i < NUM_ENTITIES; i++) {
            UserRepresentation user = UserConfigBuilder.create().username(USER_PREFIX + i).email(USER_PREFIX + i + "@foo.com").name("foo", "bar").build();
            users.add(user);
        }

        piRep.setUsers(users);
    }

    protected void addUsersWithIds() {
        List<UserRepresentation> users = new ArrayList<>();

        for (int i = 0; i < NUM_ENTITIES; i++) {
            UserRepresentation user = UserConfigBuilder.create().id(UUID.randomUUID().toString()).username(USER_PREFIX + i).email(USER_PREFIX + i + "@foo.com").name("foo", "bar").build();
            users.add(user);
        }

        piRep.setUsers(users);
    }

    protected void addUsersWithTermsAndConditions() {
        List<UserRepresentation> users = new ArrayList<>();
        List<String> requiredActions = new ArrayList<>();
        requiredActions.add(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());

        for (int i = 0; i < NUM_ENTITIES; i++) {
            UserRepresentation user = UserConfigBuilder.create().username(USER_PREFIX + i).email(USER_PREFIX + i + "@foo.com").name("foo", "bar").build();
            user.setRequiredActions(requiredActions);
            users.add(user);
        }

        piRep.setUsers(users);
    }

    protected void addGroups() {
        List<GroupRepresentation> groups = new ArrayList<>();

        for (int i=0; i < NUM_ENTITIES; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName(GROUP_PREFIX + i);
            group.setPath("/" + GROUP_PREFIX + i);
            groups.add(group);
        }

        piRep.setGroups(groups);
    }

    protected void addClients(boolean withServiceAccounts) {
        List<ClientRepresentation> clients = new ArrayList<>();
        List<UserRepresentation> serviceAccounts = new ArrayList<>();

        for (int i = 0; i < NUM_ENTITIES; i++) {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(CLIENT_PREFIX + i);
            client.setName(CLIENT_PREFIX + i);
            clients.add(client);
            if (withServiceAccounts) {
                client.setServiceAccountsEnabled(true);
                client.setBearerOnly(false);
                client.setPublicClient(false);
                client.setAuthorizationSettings(resourceServerSampleSettings);
                client.setAuthorizationServicesEnabled(true);
                // create the user service account
                UserRepresentation serviceAccount = new UserRepresentation();
                serviceAccount.setUsername(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId());
                serviceAccount.setEnabled(true);
                serviceAccount.setEmail(serviceAccount.getUsername() + "@placeholder.org");
                serviceAccount.setServiceAccountClientId(client.getClientId());
                serviceAccounts.add(serviceAccount);
            }
        }

        if (withServiceAccounts) {
            if (piRep.getUsers() == null) {
                piRep.setUsers(new ArrayList<>());
            }
            piRep.getUsers().addAll(serviceAccounts);
        }
        piRep.setClients(clients);
    }

    protected void addProviders() {
        addProviders(false);
    }

    private void addProviders(boolean withMappers) {
        List<IdentityProviderRepresentation> providers = new ArrayList<>();
        List<IdentityProviderMapperRepresentation> mappers = new ArrayList<>();

        for (String alias : IDP_ALIASES) {
            IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
            idpRep.setAlias(alias);
            idpRep.setProviderId(alias);
            idpRep.setEnabled(true);
            idpRep.setAuthenticateByDefault(false);
            idpRep.setFirstBrokerLoginFlowAlias("first broker login");

            Map<String, String> config = new HashMap<>();
            config.put("clientSecret", "secret");
            config.put("clientId", alias);
            idpRep.setConfig(config);
            providers.add(idpRep);

            if(withMappers) {
                Map<String, String> mapConfig = new HashMap<>();
                mapConfig.put("external.role", "IDP.TEST_ROLE");
                mapConfig.put("syncMode", "FORCE");
                mapConfig.put("role", "TEST_ROLE");

                IdentityProviderMapperRepresentation idpMapRep = new IdentityProviderMapperRepresentation();
                idpMapRep.setName(alias+"_mapper");
                idpMapRep.setIdentityProviderAlias(alias);
                idpMapRep.setIdentityProviderMapper("keycloak-oidc-role-to-role-idp-mapper");
                idpMapRep.setConfig(mapConfig);

                mappers.add(idpMapRep);
            }
        }

        piRep.setIdentityProviders(providers);
        if (withMappers) {
            piRep.setIdentityProviderMappers(mappers);
        }
    }

    protected void addProviderMappers() {
        addProviders(true);
    }

    private List<RoleRepresentation> makeRoles(String prefix) {
        List<RoleRepresentation> roles = new ArrayList<>();

        for (int i = 0; i < NUM_ENTITIES; i++) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(prefix + i);
            roles.add(role);
        }

        return roles;
    }

    protected void addRealmRoles() {
        RolesRepresentation roles = piRep.getRoles();
        if (roles == null) roles = new RolesRepresentation();
        roles.setRealm(makeRoles(REALM_ROLE_PREFIX));
        piRep.setRoles(roles);
    }

    protected void addClientRoles() {
        RolesRepresentation roles = piRep.getRoles();
        if (roles == null) roles = new RolesRepresentation();
        Map<String, List<RoleRepresentation>> clientRolesMap = new HashMap<>();
        clientRolesMap.put(CLIENT_ROLES_CLIENT, makeRoles(CLIENT_ROLE_PREFIX));
        roles.setClient(clientRolesMap);
        piRep.setRoles(roles);
    }

    protected void testFail() {
        setFail();
        PartialImportResults results = doImport();
        assertNull(results.getErrorMessage());
        results = doImport(); // second time should fail
        assertNotNull(results.getErrorMessage());
    }

    protected void testSkip() {
        testSkip(NUM_ENTITIES);
    }

    protected void testSkip(int numberEntities) {
        setSkip();
        PartialImportResults results = doImport();
        assertEquals(numberEntities, results.getAdded());

        results = doImport();
        assertEquals(numberEntities, results.getSkipped());
    }

    protected void testOverwrite() {
        testOverwrite(NUM_ENTITIES);
    }

    protected void testOverwrite(int numberEntities) {
        setOverwrite();
        PartialImportResults results = doImport();
        assertEquals(numberEntities, results.getAdded());

        results = doImport();
        assertEquals(numberEntities, results.getOverwritten());
    }

    private static class PartialImportRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            builder.duplicateEmailsAllowed(false);

            return builder;
        }
    }

    private static class PartialImportRolesClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder builder) {
            builder.clientId(CLIENT_ROLES_CLIENT);
            builder.name(CLIENT_ROLES_CLIENT);
            builder.protocol("openid-connect");

            return builder;
        }
    }

    private static class PartialImportServiceClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder builder) {
            builder.clientId(CLIENT_SERVICE_ACCOUNT);
            builder.name(CLIENT_SERVICE_ACCOUNT);
            builder.secret("secret");
            builder.protocol("openid-connect");
            builder.rootUrl("http://localhost/foo");
            builder.publicClient(false);
            builder.serviceAccountsEnabled(true);

            return builder;
        }
    }

    public static class PartialImportServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            return builder.dependency("org.keycloak.tests", "keycloak-tests-custom-scripts");
        }
    }
}

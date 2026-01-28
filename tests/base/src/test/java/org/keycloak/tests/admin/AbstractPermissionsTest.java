package org.keycloak.tests.admin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.MailServerConfiguration;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = AbstractPermissionsTest.PermissionsTestServerConfig.class)
public class AbstractPermissionsTest {

    @InjectRealm(attachTo = "master", ref = "masterRealm")
    ManagedRealm managedMasterRealm;

    @InjectAdminClientFactory
    protected AdminClientFactory adminClientFactory;

    @InjectAdminClient
    protected Keycloak adminClient;

    protected static final String REALM_NAME = "permissions-test";
    protected static final String REALM_2_NAME = "realm2";

    protected Map<String, Keycloak> clients = new HashMap<>();

    @BeforeEach
    public void beforeEach() { // todo rewrite
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

        clients.put("REALM2", adminClientFactory.create().realm(REALM_2_NAME).username("admin").password("password").clientId("test-client").build());

        clients.put("master-admin", adminClient);

        clients.put("master-none", adminClientFactory.create().realm("master").username("permissions-test-master-none").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build());

        for (String role : AdminRoles.ALL_REALM_ROLES) {
            clients.put("master-" + role,
                    adminClientFactory.create().realm("master").username("permissions-test-master-" + role).password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build());
        }

    }

    protected void invoke(final Invocation invocation, AdminAuth.Resource resource, boolean manage) {
        invoke((realm, response) ->
                        invocation.invoke(realm),
                resource, manage);
    }

    protected void invoke(final Invocation invocation, AdminAuth.Resource resource, boolean manage, boolean skipDifferentRole) {
        invoke((realm, response) ->
                        invocation.invoke(realm),
                resource, manage, skipDifferentRole);
    }

    protected void invoke(InvocationWithResponse invocation, AdminAuth.Resource resource, boolean manage) {
        invoke(invocation, resource, manage, false);
    }

    protected void invoke(InvocationWithResponse invocation, AdminAuth.Resource resource, boolean manage, boolean skipDifferentRole) {
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

    protected void invoke(final Invocation invocation, Keycloak client, boolean expectSuccess) {
        invoke((realm, response) ->
                        invocation.invoke(realm),
                client, expectSuccess);
    }

    protected void invoke(InvocationWithResponse invocation, Keycloak client, boolean expectSuccess) {
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

    private String getViewRole(AdminAuth.Resource resource) {
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

    private String getManageRole(AdminAuth.Resource resource) {
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

    private String getDifferentViewRole(AdminAuth.Resource resource) {
        return switch (resource) {
            case CLIENT -> AdminRoles.VIEW_USERS;
            case USER -> AdminRoles.VIEW_CLIENTS;
            case REALM -> AdminRoles.VIEW_EVENTS;
            case EVENTS, AUTHORIZATION -> AdminRoles.VIEW_IDENTITY_PROVIDERS;
            case IDENTITY_PROVIDER -> AdminRoles.VIEW_REALM;
            default -> throw new RuntimeException("Unexpected resouce");
        };
    }

    private String getDifferentManageRole(AdminAuth.Resource resource) {
        return switch (resource) {
            case CLIENT -> AdminRoles.MANAGE_USERS;
            case USER -> AdminRoles.MANAGE_CLIENTS;
            case REALM -> AdminRoles.MANAGE_EVENTS;
            case EVENTS, AUTHORIZATION -> AdminRoles.MANAGE_IDENTITY_PROVIDERS;
            case IDENTITY_PROVIDER -> AdminRoles.MANAGE_REALM;
            default -> throw new RuntimeException("Unexpected resouce");
        };
    }

    protected interface Invocation {

        void invoke(RealmResource realm);

    }

    protected interface InvocationWithResponse {

        void invoke(RealmResource realm, AtomicReference<Response> response);

    }

    static class PermissionsTestServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.AUTHORIZATION);
        }
    }

    protected static class PermissionsTestRealmConfig1 implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(REALM_NAME);
            realm.addClient("test-client")
                    .enabled(true)
                    .publicClient(true)
                    .directAccessGrantsEnabled(true);

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

    protected static class PermissionsTestRealmConfig2 implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(REALM_2_NAME);

            realm.addClient("test-client")
                    .publicClient(true)
                    .directAccessGrantsEnabled(true);

            realm.addUser("admin")
                    .name("admin", "admin")
                    .email("admin" + "@localhost.com")
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            return realm;
        }
    }
}

package org.keycloak.tests.admin.authz.rbac;

import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.AfterEach;

import static java.util.function.Predicate.not;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

abstract class AbstractAdminRBACTest {

    @InjectRealm(attachTo = "master", ref = "master")
    ManagedRealm masterRealm;

    @InjectUser(realmRef = "master", config = MasterUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser masterUser;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectAdminClientFactory
    private AdminClientFactory adminClientFactory;

    Keycloak masterUserAdminClient;

    @AfterEach
    public void onAfterEach() {
        try {
            adminClient.realms().findAll().stream()
                    .filter(not((r) -> r.getRealm().equals(masterRealm.getName())))
                    .forEach((r) -> adminClient.realms().realm(r.getRealm()).remove());
        } catch (NotFoundException ignore) {
        }
    }

    protected void grantMasterRealmManagementRole(String targetRealmName, String masterUserName, String adminRole) {
        List<UserRepresentation> users = masterRealm.admin().users().search(masterUserName, true);
        assertEquals(1, users.size());
        UserRepresentation masterUser = users.get(0);
        RealmResource masterRealmApi = masterRealm.admin();
        UserResource userApi = masterRealmApi.users().get(masterUser.getId());
        ClientRepresentation mgmtClient = masterRealmApi.clients().findByClientId(targetRealmName + "-realm").get(0);
        ClientResource mgmtClientApi = masterRealmApi.clients().get(mgmtClient.getId());
        RoleRepresentation role = mgmtClientApi.roles().get(adminRole).toRepresentation();
        userApi.roles().clientLevel(mgmtClient.getId()).add(List.of(role));
    }

    protected void grantRealmManagementRole(RealmResource realm, String userName, String adminRole) {
        List<UserRepresentation> users = realm.users().search(userName, true);
        assertEquals(1, users.size());
        UserResource userApi = realm.users().get(users.get(0).getId());
        ClientRepresentation mgmtClient = realm.clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        ClientResource mgmtClientApi = realm.clients().get(mgmtClient.getId());
        RoleRepresentation role = mgmtClientApi.roles().get(adminRole).toRepresentation();
        userApi.roles().clientLevel(mgmtClient.getId()).add(List.of(role));
    }

    protected RealmResource createRealm(Keycloak adminClient, String name) {
        adminClient.realms().create(RealmConfigBuilder.create().name(name).build());
        return adminClient.realm(name);
    }

    protected UserRepresentation createUser(RealmResource realm, String username) {
        UserRepresentation user = UserConfigBuilder.create()
                .username(username)
                .email(username.concat("@keycloak.org"))
                .firstName("First")
                .lastName("Last")
                .enabled(true).build();

        try (Response response = realm.users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        return user;
    }

    protected void revokeRealmRole(ManagedUser user, String roleName) {
        user.admin().roles().realmLevel().listAll().stream().filter((r) -> r.getName().equals(roleName)).forEach((r) -> {
            user.admin().roles().realmLevel().remove(List.of(r));
        });
    }

    protected void grantRealmRole(RealmResource realm, UserRepresentation user, String roleName) {
        RoleRepresentation role;

        try {
            role = realm.roles().get(roleName).toRepresentation();
        } catch (NotFoundException nfe) {
            realm.roles().create(RoleConfigBuilder.create().name(roleName).build());
            role = realm.roles().get(roleName).toRepresentation();
        }

        UserResource userApi = realm.users().get(user.getId());
        userApi.roles().realmLevel().add(List.of(role));
        List<String> realmRoles = userApi.roles().realmLevel().listAll().stream().map(RoleRepresentation::getName).toList();
        assertTrue(realmRoles.contains(roleName));
    }

    protected void runAs(String realm, String username, Consumer<Keycloak> consumer) {
        Keycloak userClient = createAdminClient(realm, username);
        consumer.accept(userClient);
    }

    private Keycloak createAdminClient(String realmName, String username) {
        return adminClientFactory.create()
                .realm(realmName)
                .clientId("admin-cli")
                .username(username)
                .password("password")
                .build();
    }

    protected void assertForbidden(String message, Runnable runnable) {
        try {
            runnable.run();
            fail(message);
        } catch (jakarta.ws.rs.ForbiddenException expected) {
        }
    }

    private static class MasterUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("mymasteradmin")
                    .password("password")
                    .email("mymasteradmin@keycloak.org")
                    .firstName("f")
                    .lastName("l");
        }
    }
}

package org.keycloak.tests.admin.finegrainedadminv1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.services.resources.admin.fgap.ClientPermissionManagement;
import org.keycloak.services.resources.admin.fgap.GroupPermissionManagement;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

@KeycloakIntegrationTest(config = AbstractFineGrainedAdminTest.FineGrainedAdminServerConf.class)
public class FineGrainedAdminSearchTest extends AbstractFineGrainedAdminTest {

    @Test
    public void testUserPagination() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);

            session.getContext().setRealm(realm);

            GroupModel customerAGroup = session.groups().createGroup(realm, "Customer A");
            UserModel customerAManager = session.users().addUser(realm, "customer-a-manager");
            customerAManager.setFirstName("Customer");
            customerAManager.setLastName("A");
            customerAManager.setEmail("customer@a");
            customerAManager.credentialManager().updateCredential(UserCredentialModel.password("password"));
            ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            customerAManager.grantRole(realmAdminClient.getRole(AdminRoles.QUERY_USERS));
            customerAManager.setEnabled(true);
            UserModel regularAdminUser = session.users().addUser(realm, "regular-admin-user");
            regularAdminUser.setFirstName("Regular");
            regularAdminUser.setLastName("Admin");
            regularAdminUser.setEmail("regular@admin");
            regularAdminUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
            regularAdminUser.grantRole(realmAdminClient.getRole(AdminRoles.VIEW_USERS));
            regularAdminUser.setEnabled(true);

            AdminPermissionManagement management = AdminPermissions.management(session, realm);

            GroupPermissionManagement groupPermission = management.groups();

            groupPermission.setPermissionsEnabled(customerAGroup, true);

            UserPolicyRepresentation userPolicyRepresentation = new UserPolicyRepresentation();

            userPolicyRepresentation.setName("Only " + customerAManager.getUsername());
            userPolicyRepresentation.addUser(customerAManager.getId());

            Policy policy = groupPermission.viewMembersPermission(customerAGroup);

            AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);

            Policy userPolicy = provider.getStoreFactory().getPolicyStore().create(management.realmResourceServer(), userPolicyRepresentation);

            policy.addAssociatedPolicy(RepresentationToModel.toModel(userPolicyRepresentation, provider, userPolicy));

            for (int i = 0; i < 20; i++) {
                UserModel userModel = session.users().addUser(realm, "a" + i);
                userModel.setFirstName("test");
            }

            for (int i = 20; i < 40; i++) {
                UserModel userModel = session.users().addUser(realm, "b" + i);
                userModel.setFirstName("test");
                userModel.joinGroup(customerAGroup);
            }
        });

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("customer-a-manager").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<UserRepresentation> result = client.realm(REALM_NAME).users().search(null, "test", null, null, -1, 20);

            Assertions.assertEquals(20, result.size());
            assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("b"))));

            result = client.realm(REALM_NAME).users().search(null, "test", null, null, 20, 40);

            Assertions.assertEquals(0, result.size());
        }

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("regular-admin-user").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<UserRepresentation> result = client.realm(REALM_NAME).users().search(null, "test", null, null, -1, 20);

            Assertions.assertEquals(20, result.size());
            assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("a"))));

            client.realm(REALM_NAME).users().search(null, null, null, null, -1, -1);

            Assertions.assertEquals(20, result.size());
            assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("a"))));
        }

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("customer-a-manager").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<UserRepresentation> result = client.realm(REALM_NAME).users().search(null, null, null, null, -1, 20);

            Assertions.assertEquals(20, result.size());
            assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("b"))));

            result = client.realm(REALM_NAME).users().search("test", -1, 20, false);

            Assertions.assertEquals(20, result.size());
            assertThat(result, Matchers.everyItem(Matchers.hasProperty("username", Matchers.startsWith("b"))));

            result = client.realm(REALM_NAME).users().search("a", -1, 20, false);

            Assertions.assertEquals(0, result.size());
        }
    }

    @Test
    public void testClientsSearch() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);

            session.getContext().setRealm(realm);

            ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            UserModel regularAdminUser = session.users().addUser(realm, "regular-admin-user");
            regularAdminUser.setFirstName("Regular");
            regularAdminUser.setLastName("Admin");
            regularAdminUser.setEmail("regular@admin");
            regularAdminUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
            regularAdminUser.grantRole(realmAdminClient.getRole(AdminRoles.QUERY_CLIENTS));
            regularAdminUser.setEnabled(true);

            UserPolicyRepresentation userPolicyRepresentation = new UserPolicyRepresentation();

            userPolicyRepresentation.setName("Only " + regularAdminUser.getUsername());
            userPolicyRepresentation.addUser(regularAdminUser.getId());

            for (int i = 0; i < 30; i++) {
                realm.addClient("client-search-" + (i < 10 ? "0" + i : i));
            }

            AdminPermissionManagement management = AdminPermissions.management(session, realm);
            ClientPermissionManagement clientPermission = management.clients();
            ClientModel clientModel = realm.getClientByClientId("client-search-09");

            clientPermission.setPermissionsEnabled(clientModel, true);

            Policy policy = clientPermission.viewPermission(clientModel);
            AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
            Policy userPolicy = provider.getStoreFactory().getPolicyStore()
                    .create(management.realmResourceServer(), userPolicyRepresentation);

            policy.addAssociatedPolicy(RepresentationToModel.toModel(userPolicyRepresentation, provider, userPolicy));
        });

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("regular-admin-user").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<ClientRepresentation> result = client.realm(REALM_NAME).clients().findAll("client-search-", true, true, 0, 5);

            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals("client-search-09", result.get(0).getClientId());
        }

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);

            session.getContext().setRealm(realm);

            AdminPermissionManagement management = AdminPermissions.management(session, realm);

            ClientPermissionManagement clientPermission = management.clients();
            ClientModel clientModel = realm.getClientByClientId("client-search-10");

            clientPermission.setPermissionsEnabled(clientModel, true);

            Policy policy = clientPermission.viewPermission(clientModel);

            AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
            ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            ResourceServer resourceServer = provider.getStoreFactory().getResourceServerStore().findByClient(realmAdminClient);

            policy.addAssociatedPolicy(provider.getStoreFactory().getPolicyStore().findByName(resourceServer, "Only regular-admin-user"));
        });

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("regular-admin-user").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<ClientRepresentation> result = client.realm(REALM_NAME).clients().findAll("client-search-", true, true, -1, -1);

            Assertions.assertEquals(2, result.size());
        }

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("regular-admin-user").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<ClientRepresentation> result = client.realm(REALM_NAME).clients().findAll(null, true, false, 0, 5);

            Assertions.assertEquals(2, result.size());
        }

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("regular-admin-user").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<ClientRepresentation> result = client.realm(REALM_NAME).clients().findAll(null, true, false, 0, 1);

            Assertions.assertEquals(1, result.size());
            assertThat(result, Matchers.hasItem(Matchers.hasProperty("clientId", is("client-search-09"))));

            result = client.realm(REALM_NAME).clients().findAll(null, true, false, 1, 1);
            assertThat(result, Matchers.hasItem(Matchers.hasProperty("clientId", is("client-search-10"))));

            Assertions.assertEquals(1, result.size());

            result = client.realm(REALM_NAME).clients().findAll(null, true, false, 2, 1);

            Assertions.assertTrue(result.isEmpty());
        }

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("regular-admin-user").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<ClientRepresentation> result = client.realm(REALM_NAME).clients().findAll(null, true, false, -1, -1);

            Assertions.assertEquals(2, result.size());
        }

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);

            session.getContext().setRealm(realm);

            AdminPermissionManagement management = AdminPermissions.management(session, realm);

            ClientPermissionManagement clientPermission = management.clients();
            for (int i = 11; i < 30; i++) {
                ClientModel clientModel = realm.getClientByClientId("client-search-" + i);

                clientPermission.setPermissionsEnabled(clientModel, true);

                Policy policy = clientPermission.viewPermission(clientModel);

                AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
                ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
                ResourceServer resourceServer = provider.getStoreFactory().getResourceServerStore().findByClient(realmAdminClient);

                policy.addAssociatedPolicy(provider.getStoreFactory().getPolicyStore()
                        .findByName(resourceServer, "Only regular-admin-user"));
            }
        });

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("regular-admin-user").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<ClientRepresentation> clients = new ArrayList<>();

            List<ClientRepresentation> result = client.realm(REALM_NAME).clients().findAll("client-search-", true, true, 0, 10);
            clients.addAll(result);
            Assertions.assertEquals(10, result.size());
            assertThat(result.stream().map(rep -> rep.getClientId()).collect(Collectors.toList()), is(Arrays.asList("client-search-09",
                    "client-search-10",
                    "client-search-11",
                    "client-search-12",
                    "client-search-13",
                    "client-search-14",
                    "client-search-15",
                    "client-search-16",
                    "client-search-17",
                    "client-search-18")));

            result = client.realm(REALM_NAME).clients().findAll("client-search-", true, true, 10, 10);
            clients.addAll(result);
            Assertions.assertEquals(10, result.size());
            assertThat(result.stream().map(rep -> rep.getClientId()).collect(Collectors.toList()), is(Arrays.asList("client-search-19",
                    "client-search-20",
                    "client-search-21",
                    "client-search-22",
                    "client-search-23",
                    "client-search-24",
                    "client-search-25",
                    "client-search-26",
                    "client-search-27",
                    "client-search-28")));

            result = client.realm(REALM_NAME).clients().findAll("client-search-", true, true, 20, 10);
            clients.addAll(result);
            Assertions.assertEquals(1, result.size());
            assertThat(result, Matchers.hasItems(
                    Matchers.hasProperty("clientId", is(oneOf("client-search-29")))));
        }
    }

    @Test
    public void testClientsSearchAfterFirstPage() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);

            session.getContext().setRealm(realm);

            ClientModel realmAdminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            UserModel regularAdminUser = session.users().addUser(realm, "regular-admin-user");
            regularAdminUser.setFirstName("Regular");
            regularAdminUser.setLastName("Admin");
            regularAdminUser.setEmail("regular@admin");
            regularAdminUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
            regularAdminUser.grantRole(realmAdminClient.getRole(AdminRoles.QUERY_CLIENTS));
            regularAdminUser.setEnabled(true);

            UserPolicyRepresentation userPolicyRepresentation = new UserPolicyRepresentation();

            userPolicyRepresentation.setName("Only " + regularAdminUser.getUsername());
            userPolicyRepresentation.addUser(regularAdminUser.getId());

            AdminPermissionManagement management = AdminPermissions.management(session, realm);

            ClientPermissionManagement clientPermission = management.clients();

            for (int i = 15; i < 30; i++) {
                ClientModel clientModel = realm.addClient("client-search-" + (i < 10 ? "0" + i : i));
                clientPermission.setPermissionsEnabled(clientModel, true);

                Policy policy = clientPermission.viewPermission(clientModel);

                AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);

                if (i == 15) {
                    provider.getStoreFactory().getPolicyStore()
                            .create(management.realmResourceServer(), userPolicyRepresentation);
                }

                policy.addAssociatedPolicy(provider.getStoreFactory().getPolicyStore()
                        .findByName(management.realmResourceServer(), "Only regular-admin-user"));

            }
        });

        try (Keycloak client = adminClientFactory.create().realm(REALM_NAME)
                .username("regular-admin-user").password("password").clientId( Constants.ADMIN_CLI_CLIENT_ID).build()) {

            List<ClientRepresentation> clients = new ArrayList<>();

            List<ClientRepresentation> result = client.realm(REALM_NAME).clients().findAll("client-search-", true, true, 0, 10);
            clients.addAll(result);
            Assertions.assertEquals(10, result.size());
            assertThat(result.stream().map(rep -> rep.getClientId()).collect(Collectors.toList()), is(Arrays.asList(
                    "client-search-15",
                    "client-search-16",
                    "client-search-17",
                    "client-search-18",
                    "client-search-19",
                    "client-search-20",
                    "client-search-21",
                    "client-search-22",
                    "client-search-23",
                    "client-search-24")));

            result = client.realm(REALM_NAME).clients().findAll("client-search-", true, true, 10, 10);
            clients.addAll(result);
            Assertions.assertEquals(5, result.size());
            assertThat(result.stream().map(rep -> rep.getClientId()).collect(Collectors.toList()), is(Arrays.asList(
                    "client-search-25",
                    "client-search-26",
                    "client-search-27",
                    "client-search-28",
                    "client-search-29")));

            result = client.realm(REALM_NAME).clients().findAll("client-search-", true, true, 20, 10);
            clients.addAll(result);
            Assertions.assertTrue(result.isEmpty());
        }
    }

}

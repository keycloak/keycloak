package org.keycloak.tests.admin.finegrainedadminv1;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

@KeycloakIntegrationTest(config = AbstractFineGrainedAdminTest.FineGrainedAdminServerConf.class)
public class FineGrainedAdminMasterRealmTest extends AbstractFineGrainedAdminTest {

    @Test
    public void testMasterRealm() throws Exception {
        // test that master realm can still perform operations when policies are in place
        //
        runOnServer.run(FineGrainedAdminRestTest::setupPolices);
        runOnServer.run(FineGrainedAdminRestTest::setupUsers);

        UserRepresentation user1 = managedRealm.admin().users().search("user1").get(0);
        RoleRepresentation realmRole = managedRealm.admin().roles().get("realm-role").toRepresentation();
        List<RoleRepresentation> realmRoleSet = new LinkedList<>();
        realmRoleSet.add(realmRole);
        RoleRepresentation realmRole2 = managedRealm.admin().roles().get("realm-role2").toRepresentation();
        List<RoleRepresentation> realmRole2Set = new LinkedList<>();
        realmRole2Set.add(realmRole);
        ClientRepresentation client = managedRealm.admin().clients().findByClientId(CLIENT_NAME).get(0);
        RoleRepresentation clientRole = managedRealm.admin().clients().get(client.getId()).roles().get("client-role").toRepresentation();
        List<RoleRepresentation> clientRoleSet = new LinkedList<>();
        clientRoleSet.add(clientRole);

        adminClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().add(realmRoleSet);
        List<RoleRepresentation> roles = managedRealm.admin().users().get(user1.getId()).roles().realmLevel().listAll();
        Assertions.assertTrue(roles.stream().anyMatch((r) -> {
            return r.getName().equals("realm-role");
        }));
        adminClient.realm(REALM_NAME).users().get(user1.getId()).roles().realmLevel().remove(realmRoleSet);
        roles = managedRealm.admin().users().get(user1.getId()).roles().realmLevel().listAll();
        Assertions.assertTrue(roles.stream().noneMatch((r) -> {
            return r.getName().equals("realm-role");
        }));

        adminClient.realm(REALM_NAME).users().get(user1.getId()).roles().clientLevel(client.getId()).add(clientRoleSet);
        roles = managedRealm.admin().users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
        Assertions.assertTrue(roles.stream().anyMatch((r) -> {
            return r.getName().equals("client-role");
        }));
        adminClient.realm(REALM_NAME).users().get(user1.getId()).roles().clientLevel(client.getId()).remove(clientRoleSet);
        roles = managedRealm.admin().users().get(user1.getId()).roles().clientLevel(client.getId()).listAll();
        Assertions.assertTrue(roles.stream().noneMatch((r) -> {
            return r.getName().equals("client-role");
        }));
    }

    // KEYCLOAK-5152
    @Test
    public void testMasterRealmWithComposites() throws Exception {
        RoleRepresentation composite = new RoleRepresentation();
        composite.setName("composite");
        composite.setComposite(true);
        managedRealm.admin().roles().create(composite);
        composite = managedRealm.admin().roles().get("composite").toRepresentation();

        ClientRepresentation client = managedRealm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation createClient = managedRealm.admin().clients().get(client.getId()).roles().get(AdminRoles.CREATE_CLIENT).toRepresentation();
        RoleRepresentation queryRealms = managedRealm.admin().clients().get(client.getId()).roles().get(AdminRoles.QUERY_REALMS).toRepresentation();
        List<RoleRepresentation> composites = new LinkedList<>();
        composites.add(createClient);
        composites.add(queryRealms);
        managedRealm.admin().rolesById().addComposites(composite.getId(), composites);
    }

    // KEYCLOAK-5211
    @Test
    public void testCreateRealmCreateClient() throws Exception {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setName("fullScopedClient");
        rep.setClientId("fullScopedClient");
        rep.setFullScopeAllowed(true);
        rep.setSecret("618268aa-51e6-4e64-93c4-3c0bc65b8171");
        rep.setProtocol("openid-connect");
        rep.setPublicClient(false);
        rep.setEnabled(true);
        masterRealm.admin().clients().create(rep);

        Keycloak realmClient = adminClientFactory.create().realm("master")
                .username("admin").password("admin").clientId("fullScopedClient").clientSecret("618268aa-51e6-4e64-93c4-3c0bc65b8171").build();
        try {
            RealmRepresentation newRealm=new RealmRepresentation();
            newRealm.setRealm("anotherRealm");
            newRealm.setId("anotherRealm");
            newRealm.setEnabled(true);
            realmClient.realms().create(newRealm);

            ClientRepresentation newClient = new ClientRepresentation();

            newClient.setName("newClient");
            newClient.setClientId("newClient");
            newClient.setFullScopeAllowed(true);
            newClient.setSecret("secret");
            newClient.setProtocol("openid-connect");
            newClient.setPublicClient(false);
            newClient.setEnabled(true);
            Response response = realmClient.realm("anotherRealm").clients().create(newClient);
            Assertions.assertEquals(403, response.getStatus());
            response.close();

            realmClient.close();
            //creating new client to refresh token
            realmClient = adminClientFactory.create().realm("master")
                    .username("admin").password("admin").clientId("fullScopedClient").clientSecret("618268aa-51e6-4e64-93c4-3c0bc65b8171").build();
            assertThat(realmClient.realms().findAll().stream().map(RealmRepresentation::getRealm).collect(Collectors.toSet()),
                    hasItem("anotherRealm"));
            response = realmClient.realm("anotherRealm").clients().create(newClient);
            Assertions.assertEquals(201, response.getStatus());
            response.close();
        } finally {
            adminClient.realm("anotherRealm").remove();
            realmClient.close();
        }
    }

    // KEYCLOAK-5211
    @Test
    public void testCreateRealmCreateClientWithMaster() throws Exception {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setName("fullScopedClient");
        rep.setClientId("fullScopedClient");
        rep.setFullScopeAllowed(true);
        rep.setSecret("618268aa-51e6-4e64-93c4-3c0bc65b8171");
        rep.setProtocol("openid-connect");
        rep.setPublicClient(false);
        rep.setEnabled(true);
        masterRealm.admin().clients().create(rep);

        RealmRepresentation newRealm=new RealmRepresentation();
        newRealm.setRealm("anotherRealm");
        newRealm.setId("anotherRealm");
        newRealm.setEnabled(true);
        adminClient.realms().create(newRealm);

        try {
            ClientRepresentation newClient = new ClientRepresentation();

            newClient.setName("newClient");
            newClient.setClientId("newClient");
            newClient.setFullScopeAllowed(true);
            newClient.setSecret("secret");
            newClient.setProtocol("openid-connect");
            newClient.setPublicClient(false);
            newClient.setEnabled(true);
            Response response = adminClient.realm("anotherRealm").clients().create(newClient);
            Assertions.assertEquals(201, response.getStatus());
            response.close();
        } finally {
            adminClient.realm("anotherRealm").remove();

        }
    }
}

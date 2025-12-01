package org.keycloak.tests.admin.authz.fgap;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest
public class RealmRoleAdminTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @Test
    public void testManageAuthorizationRole() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientsResource clientsApi = realm.admin().clients();
        ClientRepresentation realmManagement = clientsApi.findByClientId("realm-management").get(0);
        RoleRepresentation manageAuthorizationRole = clientsApi.get(realmManagement.getId()).roles().get(AdminRoles.MANAGE_AUTHORIZATION).toRepresentation();
        RoleRepresentation viewClientsRole = clientsApi.get(realmManagement.getId()).roles().get(AdminRoles.VIEW_CLIENTS).toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmManagement.getId()).add(List.of(manageAuthorizationRole, viewClientsRole));

        clientsApi.create(ClientConfigBuilder.create()
                .clientId("authz-client")
                .secret("secret")
                .serviceAccountsEnabled(true)
                .authorizationServicesEnabled(true)
                .build()).close();
        List<ClientRepresentation> clients = clientsApi.findByClientId("authz-client");
        assertThat(clients, hasSize(1));
        ClientRepresentation client = clients.get(0);
        assertThat(clientsApi.get(client.getId()).authorization().getSettings(), notNullValue());

        clientsApi = realmAdminClient.realm(realm.getName()).clients();
        clients = clientsApi.findByClientId(client.getClientId());
        assertThat(clients, hasSize(1));

        clientsApi.get(client.getId()).authorization().getSettings();
        clientsApi.get(client.getId()).authorization().resources().resources();
        clientsApi.get(client.getId()).authorization().policies().policies();
        clientsApi.get(client.getId()).authorization().permissions().scope().findAll(null, null, null, null, null);
        clientsApi.get(client.getId()).authorization().permissions().resource().findByName("test");

        PolicyRepresentation policy = new PolicyRepresentation();

        policy.setName("User Policy");
        policy.setType("user");
        policy.setConfig(Map.of("users", "[]"));

        try (Response response = clientsApi.get(client.getId()).authorization().policies().create(policy)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }
    }
}

package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.realm.RealmConfig;
import org.keycloak.test.framework.realm.RealmConfigBuilder;

import java.util.List;

@KeycloakIntegrationTest
public class RealmWithClientAndUserTest {

    @InjectRealm(config = RealmWithClientAndUser.class)
    ManagedRealm realm;

    @Test
    public void testRealmWithClientAndUser() {
        List<ClientRepresentation> clients = realm.admin().clients().findByClientId("myclient");
        Assertions.assertEquals(1, clients.size());

        ClientRepresentation client = clients.get(0);
        Assertions.assertTrue(client.isEnabled());
        Assertions.assertTrue(client.isDirectAccessGrantsEnabled());
        Assertions.assertEquals("mysecret", client.getSecret());

        List<UserRepresentation> users = realm.admin().users().search("myadmin");
        Assertions.assertEquals(1, users.size());

        UserRepresentation user = users.get(0);
        Assertions.assertTrue(user.isEnabled());
        Assertions.assertEquals("My", user.getFirstName());
        Assertions.assertEquals("Admin", user.getLastName());
        Assertions.assertEquals("myadmin@localhost", user.getEmail());
        Assertions.assertTrue(user.isEmailVerified());

        MappingsRepresentation roles = realm.admin().users().get(user.getId()).roles().getAll();
        Assertions.assertEquals(1, roles.getClientMappings().get(Constants.REALM_MANAGEMENT_CLIENT_ID).getMappings().size());
    }

    public static class RealmWithClientAndUser implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("myclient")
                    .secret("mysecret")
                    .directAccessGrants();

            realm.addUser("myadmin")
                    .name("My", "Admin")
                    .email("myadmin@localhost")
                    .emailVerified()
                    .password("mypassword")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            return realm;
        }
    }

}

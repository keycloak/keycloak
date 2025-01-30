package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import java.util.List;

@KeycloakIntegrationTest
public class RealmSpecificAdminClientTest {

    @InjectRealm(config = RealmWithClientAndUser.class)
    ManagedRealm realm;

    @InjectAdminClient(ref = "bootstrap-client")
    Keycloak bootstrapAdminClient;

    @InjectAdminClient(
            mode = InjectAdminClient.Mode.MANAGED_REALM,
            realm =RealmWithClientAndUser.REALM,
            clientId = RealmWithClientAndUser.CLIENT_ID,
            clientSecret = RealmWithClientAndUser.CLIENT_SECRET,
            username = RealmWithClientAndUser.USERNAME,
            password = RealmWithClientAndUser.PASSWORD
    )
    Keycloak realmAdminClient;

    @Test
    public void testAdminClientIssuers() throws JWSInputException {
        AccessToken bootstrapAccessToken = new JWSInput(bootstrapAdminClient.tokenManager().getAccessToken().getToken()).readJsonContent(AccessToken.class);
        Assertions.assertTrue(bootstrapAccessToken.getIssuer().endsWith("/realms/master"));

        AccessToken realmAccessToken = new JWSInput(realmAdminClient.tokenManager().getAccessToken().getToken()).readJsonContent(AccessToken.class);
        Assertions.assertTrue(realmAccessToken.getIssuer().endsWith("/realms/" + realm.getName()));
    }

    @Test
    public void testRealmWithClientAndUser() {
        RealmResource realmResource = realmAdminClient.realms().realm(realm.getName());

        List<ClientRepresentation> clients = realmResource.clients().findByClientId("myclient");
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

        MappingsRepresentation roles = realmResource.users().get(user.getId()).roles().getAll();
        Assertions.assertEquals(1, roles.getClientMappings().get(Constants.REALM_MANAGEMENT_CLIENT_ID).getMappings().size());
    }

    public static class RealmWithClientAndUser implements RealmConfig {

        public final static String REALM = "myrealm";
        public final static String CLIENT_ID = "myclient";
        public final static String CLIENT_SECRET = "mysecret";
        public final static String USERNAME = "myadmin";
        public final static String PASSWORD = "mypassword";

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(REALM);

            realm.addClient(CLIENT_ID)
                    .secret(CLIENT_SECRET)
                    .directAccessGrants();

            realm.addUser(USERNAME)
                    .name("My", "Admin")
                    .email("myadmin@localhost")
                    .emailVerified()
                    .password(PASSWORD)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            return realm;
        }
    }

}

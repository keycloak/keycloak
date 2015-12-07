package org.keycloak.testsuite.adduser;

import org.codehaus.jackson.type.TypeReference;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;
import org.keycloak.representations.idm.*;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.util.JsonSerialization;
import org.keycloak.wildfly.adduser.AddUser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AddUserTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File dir;

    @Before
    public void before() throws IOException {
        dir = folder.newFolder();
        System.setProperty("jboss.server.config.user.dir", dir.getAbsolutePath());
        System.setProperty("jboss.server.config.dir", dir.getAbsolutePath());
    }

    @After
    public void after() {
        System.getProperties().remove("jboss.server.config.user.dir");
        System.getProperties().remove("jboss.server.config.dir");
    }

    @Test
    public void addUserTest() throws Throwable {
        AddUser.main(new String[]{"-u", "addusertest-admin", "-p", "password"});
        assertEquals(1, dir.listFiles().length);

        List<RealmRepresentation> realms = JsonSerialization.readValue(new FileInputStream(new File(dir, "keycloak-add-user.json")), new TypeReference<List<RealmRepresentation>>() {});
        assertEquals(1, realms.size());
        assertEquals(1, realms.get(0).getUsers().size());

        UserRepresentation user = realms.get(0).getUsers().get(0);
        assertEquals(new Integer(100000), user.getCredentials().get(0).getHashIterations());
        assertNull(user.getCredentials().get(0).getValue());
        assertEquals(new Pbkdf2PasswordEncoder(Base64Url.decode(user.getCredentials().get(0).getSalt()), 100000).encode("password"), user.getCredentials().get(0).getHashedSaltedValue());

        KeycloakServer server = new KeycloakServer();
        try {
            server.start();

            Keycloak keycloak = Keycloak.getInstance("http://localhost:8081/auth", "master", "addusertest-admin", "password", Constants.ADMIN_CLI_CLIENT_ID);
            keycloak.realms().findAll();

            RealmRepresentation testRealm = new RealmRepresentation();
            testRealm.setEnabled(true);
            testRealm.setId("test");
            testRealm.setRealm("test");

            keycloak.realms().create(testRealm);

            RealmResource realm = keycloak.realm("master");

            List<UserRepresentation> users = realm.users().search("addusertest-admin", null, null, null, null, null);
            assertEquals(1, users.size());

            UserRepresentation created = users.get(0);
            assertNotNull(created.getCreatedTimestamp());

            UserResource userResource = realm.users().get(created.getId());

            List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listAll();

            assertRoles(realmRoles, "admin", "offline_access");

            List<ClientRepresentation> clients = realm.clients().findAll();
            String accountId = null;
            for (ClientRepresentation c : clients) {
                if (c.getClientId().equals("account")) {
                    accountId = c.getId();
                }
            }

            List<RoleRepresentation> accountRoles = userResource.roles().clientLevel(accountId).listAll();

            assertRoles(accountRoles, "view-profile", "manage-account");

            keycloak.close();

            assertEquals(0, dir.listFiles().length);
        } finally {
            server.stop();
        }
    }

    public static void assertRoles(List<RoleRepresentation> actual, String... expected) {
        assertEquals(expected.length, actual.size());

        for (String e : expected) {
            boolean found = false;
            for (RoleRepresentation r : actual) {
                if (r.getName().equals(e)) {
                    found = true;
                }
            }
            if (!found) {
                fail("Role " + e + " not found");
            }
        }
    }

}

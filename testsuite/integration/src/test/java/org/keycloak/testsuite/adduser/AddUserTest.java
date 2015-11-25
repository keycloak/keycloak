package org.keycloak.testsuite.adduser;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.wildfly.adduser.AddUser;

import java.io.File;
import java.io.IOException;

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
        Assert.assertEquals(1, dir.listFiles().length);

        KeycloakServer server = new KeycloakServer();
        try {
            server.start();

            Keycloak keycloak = Keycloak.getInstance("http://localhost:8081/auth", "master", "addusertest-admin", "password", Constants.ADMIN_CONSOLE_CLIENT_ID);
            keycloak.realms().findAll();

            RealmRepresentation testRealm = new RealmRepresentation();
            testRealm.setEnabled(true);
            testRealm.setId("test");
            testRealm.setRealm("test");

            keycloak.realms().create(testRealm);

            keycloak.close();

            Assert.assertEquals(0, dir.listFiles().length);
        } finally {
            server.stop();
        }
    }

}

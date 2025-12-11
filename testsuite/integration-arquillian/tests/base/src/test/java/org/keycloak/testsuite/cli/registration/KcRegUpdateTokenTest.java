package org.keycloak.testsuite.cli.registration;

import java.io.IOException;

import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.client.cli.config.RealmConfigData;
import org.keycloak.client.cli.util.ConfigUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegUpdateTokenTest extends AbstractRegCliTest {

    @Test
    public void testUpdateToken() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();
        ConfigUtil.setHandler(handler);

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            KcRegExec exe = execute("config credentials --config '" + configFile.getName() + "' --server " + serverUrl + " --realm master --user admin --password admin");
            assertExitCodeAndStreamSizes(exe, 0, 0, 1);

            // read current registration access token
            ConfigData data = ConfigUtil.loadConfig();
            RealmConfigData rdata = data.getRealmConfigData(serverUrl, "test");
            Assert.assertNull("realm info set", rdata);

            // update registration access token
            exe = execute("update-token --config '" + configFile.getName() + "' reg-cli-secret-direct  --server " + serverUrl + " --realm test --user user1 --password userpass");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);

            // read current registration token
            data = ConfigUtil.loadConfig();
            rdata = data.getRealmConfigData(serverUrl, "test");
            Assert.assertEquals("current session realm unchanged", "master", data.getRealm());
            Assert.assertNotNull("realm info set", rdata);
            Assert.assertNull("on the fly login was transient", rdata.getToken());
            Assert.assertNotNull("client info has registration access token", rdata.getClients().get("reg-cli-secret-direct"));

            // use --no-config and on-the-fly auth
            exe = execute("update-token reg-cli-secret-direct --no-config --server " + serverUrl + " --realm test --user user1 --password userpass");
            assertExitCodeAndStreamSizes(exe, 0, 1, 1);

            // save the token
            String token = exe.stdoutLines().get(0);

            // test that the token works
            exe = execute("get reg-cli-secret-direct --no-config --server " + serverUrl + " --realm test -t " + token);

            assertExitCodeAndStdErrSize(exe, 0, 0);

            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assert.assertEquals("client representation returned", "reg-cli-secret-direct", client.getClientId());
        }
    }
}

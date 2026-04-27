package org.keycloak.testsuite.cli.registration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.client.registration.cli.KcRegMain.CMD;
import static org.keycloak.testsuite.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegUpdateTest extends AbstractRegCliTest {


    @Test
    public void testUpdateThoroughly() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(FileConfigHandler.getConfigFile())) {

            final String realm = "test";

            loginAsUser(configFile.getFile(), serverUrl, realm, "user1", "userpass");


            // create an object so we can update it
            KcRegExec exe = execute("create --config '" + configFile.getName() + "' -o -s clientId=my_client");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);

            Assertions.assertEquals(true, client.isEnabled(), "enabled");
            Assertions.assertEquals(false, client.isPublicClient(), "publicClient");
            Assertions.assertEquals(false, client.isBearerOnly(), "bearerOnly");
            Assertions.assertTrue(client.getRedirectUris().isEmpty(), "redirectUris is empty");


            // Merge update
            exe = execute("update my_client --config '" + configFile.getName() + "' -o " +
                        " -s enabled=false -s 'redirectUris=[\"http://localhost:8980/myapp/*\"]'");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assertions.assertEquals(false, client.isEnabled(), "enabled");
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirectUris");



            // Another merge update - test deleting an attribute, deleting a list item and adding a list item
            exe = execute("update my_client --config '" + configFile.getName() + "' -o -d redirectUris -s webOrigins+=http://localhost:8980/myapp -s webOrigins+=http://localhost:8981/myapp -d webOrigins[0]");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);

            Assertions.assertTrue(client.getRedirectUris().isEmpty(), "redirectUris is empty");
            Assertions.assertEquals(Arrays.asList("http://localhost:8981/myapp"), client.getWebOrigins(), "webOrigins");



            // Another merge update - test nested attributes and setting an attribute using json format
            // TODO KEYCLOAK-3705 Updating protocolMapper config via client registration endpoint has no effect
            /*
            exe = execute("update my_client --config '" + configFile.getName() + "' -o -s 'protocolMappers[0].config.\"id.token.claim\"=false' " +
                    "-s 'protocolMappers[4].config={\"single\": \"true\", \"attribute.nameformat\": \"Basic\", \"attribute.name\": \"Role\"}'");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assert.assertEquals("protocolMapper[0].config.\"id.token.claim\"", "false", client.getProtocolMappers().get(0).getConfig().get("id.token.claim"));
            Assert.assertEquals("protocolMappers[4].config.single", "true", client.getProtocolMappers().get(4).getConfig().get("single"));
            Assert.assertEquals("protocolMappers[4].config.\"attribute.nameformat\"", "Basic", client.getProtocolMappers().get(4).getConfig().get("attribute.nameformat"));
            Assert.assertEquals("protocolMappers[4].config.\"attribute.name\"", "Role", client.getProtocolMappers().get(4).getConfig().get("attribute.name"));
            */

            // update using oidc format


            // check that using an invalid attribute key is not ignored
            exe = execute("update my_client --nonexisting --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 2, 0, 3);
            Assertions.assertEquals("Unknown option: '--nonexisting'", exe.stderrLines().get(0), "error message");
            Assertions.assertEquals("Try '" + CMD + " update --help' for more information on the available options.", exe.stderrLines().get(2), "try help");


            // try use incompatible endpoint
            exe = execute("update my_client --config '" + configFile.getName() + "' -o -s enabled=true -e oidc");

            assertExitCodeAndStreamSizes(exe, 1, 0, 1);
            Assertions.assertEquals("Failed to set attribute 'enabled' on document type 'oidc'", exe.stderrLines().get(exe.stderrLines().size() - 1), "error message");



            // test overwrite from file
            exe = KcRegExec.newBuilder()
                    .argsLine("update my_client --config '" + configFile.getName() +
                            "' -o  -s clientId=my_client -s 'redirectUris=[\"http://localhost:8980/myapp/*\"]' -f -")
                    .stdin(new ByteArrayInputStream("{ \"enabled\": false }".getBytes()))
                    .execute();

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            // web origin is not sent to the server, thus it retains the current value
            Assertions.assertEquals(Arrays.asList("http://localhost:8981/myapp"), client.getWebOrigins(), "webOrigins");
            Assertions.assertFalse(client.isEnabled(), "enabled is false");
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirectUris");



            // test using merge with file
            exe = KcRegExec.newBuilder()
                    .argsLine("update my_client --config '" + configFile.getName() +
                            "' -o -s enabled=true -m -f -")
                    .stdin(new ByteArrayInputStream("{ \"webOrigins\": [\"http://localhost:8980/myapp\"] }".getBytes()))
                    .execute();

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp"), client.getWebOrigins(), "webOrigins");
            Assertions.assertTrue(client.isEnabled(), "enabled is true");
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirectUris");



            // remove registration access token
            exe = execute("config registration-token --config '" + configFile.getName() + "' --server " + serverUrl +
                    " --realm " + realm + " --client my_client -d");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            Assertions.assertNull(handler.loadConfig().ensureRealmConfigData(serverUrl, realm).getClients().get("my_client"), "my_client registration token");
        }
    }
}

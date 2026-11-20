package org.keycloak.tests.cli.registration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.cli.KcRegExec;
import org.keycloak.tests.cli.TempFileResource;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.client.registration.cli.KcRegMain.CMD;
import static org.keycloak.tests.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
class KcRegUpdateTest extends AbstractRegCliTest {


    @Test
    void testUpdateThoroughly() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(FileConfigHandler.getConfigFile())) {

            final String realm = "test";

            loginAsUser(configFile.getFile(), serverUrl, realm, "user1", "userpass");


            // create an object so we can update it
            KcRegExec exe = execute("create --config '" + configFile.getName() + "' -o -s client_name=my_client");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            OIDCClientRepresentation client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

            String clientId = client.getClientId();
            Assertions.assertNotNull(clientId);
            Assertions.assertEquals("my_client", client.getClientName());

            // Merge update
            exe = execute("update " + clientId + " --config '" + configFile.getName() + "' -o " +
                        " -s client_name=my_client -s 'redirect_uris=[\"http://localhost:8980/myapp/*\"]'");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirect_uris");



            // Another merge update - test deleting an attribute, deleting a list item and adding a list item
            exe = execute("update " + clientId + " --config '" + configFile.getName() + "' -o -d redirect_uris -s redirect_uris+=http://localhost:8980/myapp -s redirect_uris+=http://localhost:8981/myapp -d 'redirect_uris[0]'");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

            Assertions.assertEquals(Arrays.asList("http://localhost:8981/myapp"), client.getRedirectUris(), "redirect_uris");



            // check that using an invalid attribute key is not ignored
            exe = execute("update " + clientId + " --nonexisting --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 2, 0, 3);
            Assertions.assertEquals("Unknown option: '--nonexisting'", exe.stderrLines().get(0), "error message");
            Assertions.assertEquals("Try '" + CMD + " update --help' for more information on the available options.", exe.stderrLines().get(2), "try help");


            // Internal representation fields are rejected by the OIDC endpoint.
            exe = execute("update " + clientId + " --config '" + configFile.getName() + "' -o -s enabled=true -e oidc");

            assertExitCodeAndStreamSizes(exe, 1, 0, 1);
            Assertions.assertEquals("Failed to set attribute 'enabled' on document type 'oidc'", exe.stderrLines().get(exe.stderrLines().size() - 1), "error message");



            // test overwrite from file
            exe = KcRegExec.newBuilder()
                    .argsLine("update " + clientId + " --config '" + configFile.getName() +
                            "' -o -s client_id=" + clientId + " -s client_name=my_client -s 'redirect_uris=[\"http://localhost:8980/myapp/*\"]' -f -")
                    .stdin(new ByteArrayInputStream("{ \"client_name\": \"updated\" }".getBytes()))
                    .execute();

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
            Assertions.assertEquals("my_client", client.getClientName());
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirect_uris");



            // test using merge with file
            exe = KcRegExec.newBuilder()
                    .argsLine("update " + clientId + " --config '" + configFile.getName() +
                            "' -o -s client_name=my_client -m -f -")
                    .stdin(new ByteArrayInputStream("{ \"client_uri\": \"http://localhost:8980/myapp\" }".getBytes()))
                    .execute();

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
            Assertions.assertEquals("http://localhost:8980/myapp", client.getClientUri(), "client_uri");
            Assertions.assertEquals("my_client", client.getClientName());
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirect_uris");



            // remove registration access token
            exe = execute("config registration-token --config '" + configFile.getName() + "' --server " + serverUrl +
                    " --realm " + realm + " --client " + clientId + " -d");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            Assertions.assertNull(handler.loadConfig().ensureRealmConfigData(serverUrl, realm).getClients().get(clientId), "my_client registration token");
        }
    }
}

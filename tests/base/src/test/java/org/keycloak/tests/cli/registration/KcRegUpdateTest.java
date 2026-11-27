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


            // create an object so we can update it (OIDC assigns client_id)
            KcRegExec exe = execute("create --config '" + configFile.getName() + "' -o -s client_name=my_client");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            OIDCClientRepresentation client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

            Assertions.assertNotNull(client.getClientId(), "clientId");
            String clientId = client.getClientId();
            Assertions.assertEquals("my_client", client.getClientName(), "client_name");
            Assertions.assertTrue(client.getRedirectUris() == null || client.getRedirectUris().isEmpty(), "redirect_uris is empty");


            // Merge update
            exe = execute("update " + clientId + " --config '" + configFile.getName() + "' -o " +
                        " -s 'redirect_uris=[\"http://localhost:8980/myapp/*\"]'");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirect_uris");



            // Another merge update - test deleting an attribute, deleting a list item and adding a list item
            exe = execute("update " + clientId + " --config '" + configFile.getName() + "' -o -d redirect_uris -s grant_types+=authorization_code -s grant_types+=refresh_token -d grant_types[0]");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

            Assertions.assertTrue(client.getRedirectUris() == null || client.getRedirectUris().isEmpty(), "redirect_uris is empty");
            Assertions.assertEquals(Arrays.asList("refresh_token"), client.getGrantTypes(), "grant_types");



            // check that using an invalid attribute key is not ignored
            exe = execute("update " + clientId + " --nonexisting --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 2, 0, 3);
            Assertions.assertEquals("Unknown option: '--nonexisting'", exe.stderrLines().get(0), "error message");
            Assertions.assertEquals("Try '" + CMD + " update --help' for more information on the available options.", exe.stderrLines().get(2), "try help");


            // try use removed endpoint
            exe = execute("update " + clientId + " --config '" + configFile.getName() + "' -o -s client_name=x -e default");

            assertExitCodeAndStreamSizes(exe, 2, 0, 1);
            Assertions.assertTrue(exe.stderrLines().get(0).contains("no longer supported")
                    || exe.stderrLines().get(0).contains("Endpoint 'default'"), "error message: " + exe.stderrLines().get(0));



            // test overwrite from file
            exe = KcRegExec.newBuilder()
                    .argsLine("update " + clientId + " --config '" + configFile.getName() +
                            "' -o -s client_id=" + clientId + " -s 'redirect_uris=[\"http://localhost:8980/myapp/*\"]' -f -")
                    .stdin(new ByteArrayInputStream("{ \"client_name\": \"From File\" }".getBytes()))
                    .execute();

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
            Assertions.assertEquals("From File", client.getClientName(), "client_name");
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirect_uris");



            // test using merge with file
            exe = KcRegExec.newBuilder()
                    .argsLine("update " + clientId + " --config '" + configFile.getName() +
                            "' -o -s client_name=Merged -m -f -")
                    .stdin(new ByteArrayInputStream("{ \"client_uri\": \"http://localhost:8980/myapp\" }".getBytes()))
                    .execute();

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
            Assertions.assertEquals("http://localhost:8980/myapp", client.getClientUri(), "client_uri");
            Assertions.assertEquals("Merged", client.getClientName(), "client_name");
            Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirect_uris");



            // remove registration access token
            exe = execute("config registration-token --config '" + configFile.getName() + "' --server " + serverUrl +
                    " --realm " + realm + " --client " + clientId + " -d");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            Assertions.assertNull(handler.loadConfig().ensureRealmConfigData(serverUrl, realm).getClients().get(clientId), "registration token removed");
        }
    }
}

package org.keycloak.testsuite.cli.admin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.cli.KcAdmExec;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.cli.KcAdmExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmCreateTest extends AbstractAdmCliTest {

    @Test
    public void testCreateWithRealmOverride() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            // authenticate as a regular user against one realm
            KcAdmExec exe = execute("config credentials -x --config '" + configFile.getName() +
                    "' --server " + serverUrl + " --realm master --user admin --password admin");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);

            exe = execute("create clients --config '" + configFile.getName() + "' --server " + serverUrl + " -r test -s clientId=my_first_client");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);
        }
    }

    @Test
    public void testCreateIDPWithoutSyncMode() throws IOException {
        final String realm = "test";
        final RealmResource realmResource = adminClient.realm(realm);

        FileConfigHandler handler = initCustomConfigFile();
        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {
            loginAsUser(configFile.getFile(), serverUrl, realm, "user1", "userpass");

            final File idpJson = new File("target/test-classes/cli/idp-keycloak-without-sync-mode.json");
            KcAdmExec exe = execute("create identity-provider/instances/ -r " + realm + " -f " + idpJson.getAbsolutePath() + " --config " + configFile.getFile());
            assertExitCodeAndStdErrSize(exe, 0, 1);
        }

        // If the sync mode is not present on creating the idp, it will never be stored automatically. However, the model will always report behaviour as "LEGACY", so no errors should occur.
        Assertions.assertNull(realmResource.identityProviders().get("idpAlias").toRepresentation().getConfig().get(IdentityProviderModel.SYNC_MODE));
    }

    @Test
    public void testCreateThoroughly() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            final String realm = "test";

            // authenticate as a regular user against one realm
            KcAdmExec exe = KcAdmExec.execute("config credentials -x --config '" + configFile.getName() +
                    "' --server " + serverUrl + " --realm master --user admin --password admin");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);

            // create configuration from file using stdin redirect ... output an object
            String content = "{\n" +
                    "        \"clientId\": \"my_client\",\n" +
                    "        \"enabled\": true,\n" +
                    "        \"redirectUris\": [\"http://localhost:8980/myapp/*\"],\n" +
                    "        \"serviceAccountsEnabled\": true,\n" +
                    "        \"name\": \"My Client App\",\n" +
                    "        \"implicitFlowEnabled\": false,\n" +
                    "        \"publicClient\": true,\n" +
                    "        \"webOrigins\": [\"http://localhost:8980/myapp\"],\n" +
                    "        \"consentRequired\": false,\n" +
                    "        \"baseUrl\": \"http://localhost:8980/myapp\",\n" +
                    "        \"bearerOnly\": true,\n" +
                    "        \"standardFlowEnabled\": true\n" +
                    "}";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create clients --config '" + configFile.getName() + "' -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 0);

                ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
                Assertions.assertNotNull(client.getId(), "id");
                Assertions.assertEquals("my_client", client.getClientId(), "clientId");
                Assertions.assertEquals(true, client.isEnabled(), "enabled");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirectUris");
                Assertions.assertEquals(true, client.isServiceAccountsEnabled(), "serviceAccountsEnabled");
                Assertions.assertEquals("My Client App", client.getName(), "name");
                Assertions.assertEquals(false, client.isImplicitFlowEnabled(), "implicitFlowEnabled");
                Assertions.assertEquals(true, client.isPublicClient(), "publicClient");
                // note there is no server-side check if protocol is supported
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp"), client.getWebOrigins(), "webOrigins");
                Assertions.assertEquals(false, client.isConsentRequired(), "consentRequired");
                Assertions.assertEquals("http://localhost:8980/myapp", client.getBaseUrl(), "baseUrl");
                Assertions.assertEquals(true, client.isStandardFlowEnabled(), "bearerOnly");
                Assertions.assertNull(client.getProtocolMappers(), "mappers are not empty");

                // create configuration from file as a template and override clientId and other attributes ... output an object
                exe = execute("create clients --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName() +
                        "' -s clientId=my_client2 -s enabled=false -s 'redirectUris=[\"http://localhost:8980/myapp2/*\"]'" +
                        " -s 'name=My Client App II' -s 'webOrigins=[\"http://localhost:8980/myapp2\"]'" +
                        " -s baseUrl=http://localhost:8980/myapp2 -s rootUrl=http://localhost:8980/myapp2");

                assertExitCodeAndStdErrSize(exe, 0, 0);

                ClientRepresentation client2 = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
                Assertions.assertNotNull(client2.getId(), "id");
                Assertions.assertEquals("my_client2", client2.getClientId(), "clientId");
                Assertions.assertEquals(false, client2.isEnabled(), "enabled");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp2/*"), client2.getRedirectUris(), "redirectUris");
                Assertions.assertEquals(true, client2.isServiceAccountsEnabled(), "serviceAccountsEnabled");
                Assertions.assertEquals("My Client App II", client2.getName(), "name");
                Assertions.assertEquals(false, client2.isImplicitFlowEnabled(), "implicitFlowEnabled");
                Assertions.assertEquals(true, client2.isPublicClient(), "publicClient");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp2"), client2.getWebOrigins(), "webOrigins");
                Assertions.assertEquals(false, client2.isConsentRequired(), "consentRequired");
                Assertions.assertEquals("http://localhost:8980/myapp2", client2.getBaseUrl(), "baseUrl");
                Assertions.assertEquals("http://localhost:8980/myapp2", client2.getRootUrl(), "rootUrl");
                Assertions.assertEquals(true, client2.isStandardFlowEnabled(), "bearerOnly");
                Assertions.assertNull(client2.getProtocolMappers(), "mappers are not empty");
            }

            // simple create, output an id
            exe = execute("create clients --config '" + configFile.getName() + "' -i -s clientId=my_client3");

            assertExitCodeAndStreamSizes(exe, 0, 1, 0);

            // simple create, default output
            exe = execute("create clients --config '" + configFile.getName() + "' -s clientId=my_client4");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);
            Assertions.assertTrue(exe.stderrLines().get(exe.stderrLines().size() - 1).startsWith("Created new client with id '"), "only id returned");
        }
    }
}

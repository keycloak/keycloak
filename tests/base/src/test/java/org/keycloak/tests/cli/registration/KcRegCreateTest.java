package org.keycloak.tests.cli.registration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.cli.KcRegExec;
import org.keycloak.tests.cli.TempFileResource;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.cli.KcRegExec.execute;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public class KcRegCreateTest extends AbstractRegCliTest {

    @Test
    void testCreateWithRealmOverride() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            // authenticate as a regular user against one realm
            KcRegExec exe = execute("config credentials -x --config '" + configFile.getName() +
                    "' --insecure --server " + keycloakUrls.getBase() + " --realm master --user admin --password admin");

            assertExitCodeAndStreamSizes(exe, 0, 0, 3);

            // use initial token of another realm with server, and realm override
            String token = issueInitialAccessToken("test");
            exe = execute("create --config '" + configFile.getName() + "' --insecure --server " + keycloakUrls.getBase() + " --realm test -s client_name=my_first_client -t " + token);

            assertExitCodeAndStreamSizes(exe, 0, 0, 3);
        }
    }

    @Test
    void testCreateWithRemovedSaml2EndpointIsRejected() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {
            String token = issueInitialAccessToken("test");

            KcRegExec exe = execute("create --config '" + configFile.getName() + "' --insecure --server " + keycloakUrls.getBase() +
                    " --realm test -e saml2 -s client_name=saml2_removed_client -t " + token);

            Assertions.assertNotEquals(0, exe.exitCode(), "exitCode != 0");
            Assertions.assertTrue(exe.stderrString().contains("no longer supported") || exe.stderrString().contains("Endpoint not supported: saml2"), "error message: " + exe.stderrString());
        }
    }

    @Test
    void testCreateThoroughly() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {
            // set initial access token in config
            String token = issueInitialAccessToken("test");

            final String realm = "test";

            KcRegExec exe = execute("config initial-token -x --config '" + configFile.getName() +
                    "' --insecure --server " + keycloakUrls.getBase() + " --realm " + realm + " " + token);

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // check that current server, realm, and initial token are saved in the file
            ConfigData config = handler.loadConfig();
            Assertions.assertEquals(keycloakUrls.getBase(), config.getServerUrl(), "Config serverUrl");
            Assertions.assertEquals(realm, config.getRealm(), "Config realm");
            Assertions.assertEquals(token, config.ensureRealmConfigData(keycloakUrls.getBase(), realm).getInitialToken(), "Config initial access token");

            // create configuration from file using stdin redirect ... output an object
            String content = "{\n" +
                    "        \"redirect_uris\": [\"http://localhost:8980/myapp/*\"],\n" +
                    "        \"grant_types\": [\"authorization_code\", \"client_credentials\", \"refresh_token\"],\n" +
                    "        \"response_types\": [\"code\", \"none\"],\n" +
                    "        \"client_name\": \"My Client App\",\n" +
                    "        \"client_uri\": \"http://localhost:8980/myapp\"\n" +
                    "}";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                OIDCClientRepresentation client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
                Assertions.assertNotNull(client.getClientId(), "clientId");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris(), "redirect_uris");
                Assertions.assertEquals(Arrays.asList("authorization_code", "client_credentials", "refresh_token"), client.getGrantTypes(), "grant_types");
                Assertions.assertEquals(Arrays.asList("code", "none"), client.getResponseTypes(), "response_types");
                Assertions.assertEquals("My Client App", client.getClientName(), "client_name");
                Assertions.assertEquals("http://localhost:8980/myapp", client.getClientUri(), "client_uri");

                // create configuration from file as a template and override attributes ... output an object
                exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName() +
                        "' -s 'redirect_uris=[\"http://localhost:8980/myapp2/*\"]'" +
                        " -s 'client_name=My Client App II' -s client_uri=http://localhost:8980/myapp2");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                OIDCClientRepresentation client2 = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
                Assertions.assertNotNull(client2.getClientId(), "clientId");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp2/*"), client2.getRedirectUris(), "redirect_uris");
                Assertions.assertEquals("My Client App II", client2.getClientName(), "client_name");
                Assertions.assertEquals("http://localhost:8980/myapp2", client2.getClientUri(), "client_uri");


                // check that using an invalid attribute key is not ignored
                exe = execute("create --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName() + "' -s clientId=my_client3");

                assertExitCodeAndStreamSizes(exe, 1, 0, 1);
                Assertions.assertEquals("Failed to set attribute 'clientId' on document type 'oidc'", exe.stderrLines().get(0));
            }

            // simple create, output an id
            exe = execute("create --insecure --config '" + configFile.getName() + "' -i -s client_name=my_client3");

            assertExitCodeAndStreamSizes(exe, 0, 1, 2);
            String generatedClientId = exe.stdoutLines().get(0);
            Assertions.assertFalse(generatedClientId.isBlank(), "only clientId returned");

            // simple create, default output
            exe = execute("create --insecure --config '" + configFile.getName() + "' -s client_name=my_client4");

            assertExitCodeAndStreamSizes(exe, 0, 0, 3);
            String registeredMsg = exe.stderrLines().get(exe.stderrLines().size() - 1);
            Assertions.assertTrue(registeredMsg.startsWith("Registered new client with client_id '"), "registered message: " + registeredMsg);
            Assertions.assertTrue(registeredMsg.endsWith("'"), "registered message: " + registeredMsg);



            // create using oidc endpoint - autodetect format
            content = "        {\n" +
                    "            \"redirect_uris\" : [ \"http://localhost:8980/myapp/*\" ],\n" +
                    "            \"grant_types\" : [ \"authorization_code\", \"client_credentials\", \"refresh_token\" ],\n" +
                    "            \"response_types\" : [ \"code\", \"none\" ],\n" +
                    "            \"client_name\" : \"My Client App\",\n" +
                    "            \"client_uri\" : \"http://localhost:8980/myapp\"\n" +
                    "        }";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create --insecure --config '" + configFile.getName() + "' -s 'client_name=My Client App V' " +
                        " -s 'redirect_uris=[\"http://localhost:8980/myapp5/*\"]' -s client_uri=http://localhost:8980/myapp5" +
                        " -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                OIDCClientRepresentation client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

                Assertions.assertNotNull(client.getClientId(), "clientId");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp5/*"), client.getRedirectUris(), "redirect_uris");
                Assertions.assertEquals(Arrays.asList("authorization_code", "client_credentials", "refresh_token"), client.getGrantTypes(), "grant_types");
                Assertions.assertEquals(Arrays.asList("code", "none"), client.getResponseTypes(), "response_types");
                Assertions.assertEquals("My Client App V", client.getClientName(), "client_name");
                Assertions.assertEquals("http://localhost:8980/myapp5", client.getClientUri(), "client_uri");



                // try use removed endpoint
                exe = execute("create --config '" + configFile.getName() + "' -e default -f '" + tmpFile.getName() + "'");

                Assertions.assertNotEquals(0, exe.exitCode(), "exitCode != 0");
                Assertions.assertTrue(exe.stderrString().contains("no longer supported") || exe.stderrString().contains("Endpoint 'default'"),
                        "error message: " + exe.stderrString());
            }

            // delete initial token
            exe = execute("config initial-token --config '" + configFile.getName() + "' --insecure --server " + serverUrl + " --realm " + realm + " --delete");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            config = handler.loadConfig();
            Assertions.assertNull(config.ensureRealmConfigData(serverUrl, realm).getInitialToken(), "initial token == null");
        }
    }

    @Test
    void testCreateWithAuthorizationServices() throws IOException {
        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            KcRegExec exe = execute("config credentials -x --config '" + configFile.getName() +
                    "' --insecure --server " + keycloakUrls.getBase() + " --realm master --user admin --password admin");
            assertExitCodeAndStreamSizes(exe, 0, 0, 3);

            String token = issueInitialAccessToken("test");
            RealmResource realm = adminClient.realm("test");
            ClientsResource clients = realm.clients();

            // create using oidc endpoint - autodetect format
            String content = "        {\n" +
                    "            \"redirect_uris\" : [ \"http://localhost:8980/myapp/*\" ],\n" +
                    "            \"grant_types\" : [ \"authorization_code\", \"client_credentials\", \"refresh_token\", \"" + OAuth2Constants.UMA_GRANT_TYPE + "\" ],\n" +
                    "            \"response_types\" : [ \"code\", \"none\" ],\n" +
                    "            \"client_name\" : \"My Reg Authz\",\n" +
                    "            \"client_uri\" : \"http://localhost:8980/myapp\"\n" +
                    "        }";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create --insecure --config '" + configFile.getName() + "' -s 'client_name=My Reg Authz' --realm test -t " + token +
                        " -s 'redirect_uris=[\"http://localhost:8980/myapp5/*\"]' -s client_uri=http://localhost:8980/myapp5" +
                        " -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                OIDCClientRepresentation oidcClient = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

                Assertions.assertNotNull(oidcClient.getClientId(), "clientId");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp5/*"), oidcClient.getRedirectUris(), "redirect_uris");
                assertThat("grant_types", oidcClient.getGrantTypes(), Matchers.containsInAnyOrder("authorization_code", "client_credentials", "refresh_token", OAuth2Constants.UMA_GRANT_TYPE));
                Assertions.assertEquals(Arrays.asList("code", "none"), oidcClient.getResponseTypes(), "response_types");
                Assertions.assertEquals("My Reg Authz", oidcClient.getClientName(), "client_name");
                Assertions.assertEquals("http://localhost:8980/myapp5", oidcClient.getClientUri(), "client_uri");

                ClientRepresentation clientRep = clients.findByClientId(oidcClient.getClientId()).get(0);
                ClientResource client = clients.get(clientRep.getId());

                clientRep = client.toRepresentation();
                Assertions.assertTrue(clientRep.getAuthorizationServicesEnabled());

                ResourceServerRepresentation settings = client.authorization().getSettings();

                Assertions.assertEquals(PolicyEnforcementMode.ENFORCING, settings.getPolicyEnforcementMode());
                Assertions.assertTrue(settings.isAllowRemoteResourceManagement());

                List<RoleRepresentation> roles = client.roles().list();

                Assertions.assertEquals(1, roles.size());
                Assertions.assertEquals("uma_protection", roles.get(0).getName());

                UserRepresentation serviceAccount = realm.users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + clientRep.getClientId()).get(0);
                Assertions.assertNotNull(serviceAccount);
                List<RoleRepresentation> serviceAccountRoles = realm.users().get(serviceAccount.getId()).roles().clientLevel(clientRep.getId()).listAll();
                Assertions.assertTrue(serviceAccountRoles.stream().anyMatch(roleRepresentation -> "uma_protection".equals(roleRepresentation.getName())));
            }
        }
    }
}

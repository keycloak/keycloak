package org.keycloak.tests.cli.registration;

import java.io.File;
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
import org.keycloak.tests.cli.exec.AbstractExec;
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
            String content = "{\"client_name\":\"My Client App\","
                    + "\"redirect_uris\":[\"http://localhost:8980/myapp/*\"],"
                    + "\"client_uri\":\"http://localhost:8980/myapp\"}";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {
                exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f - < '" + tmpFile.getName() + "'");
                assertExitCodeAndStdErrSize(exe, 0, 2);
                OIDCClientRepresentation client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
                Assertions.assertNotNull(client.getClientId(), "generated client_id");
                Assertions.assertEquals("My Client App", client.getClientName());
                Assertions.assertEquals(List.of("http://localhost:8980/myapp/*"), client.getRedirectUris());
                Assertions.assertEquals("http://localhost:8980/myapp", client.getClientUri());

                exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName()
                        + "' -s 'client_name=My Client App II' -s 'redirect_uris=[\"http://localhost:8980/myapp2/*\"]'");
                assertExitCodeAndStdErrSize(exe, 0, 2);
                OIDCClientRepresentation client2 = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);
                Assertions.assertNotEquals(client.getClientId(), client2.getClientId());
                Assertions.assertEquals("My Client App II", client2.getClientName());
                Assertions.assertEquals(List.of("http://localhost:8980/myapp2/*"), client2.getRedirectUris());

                exe = execute("create --config '" + configFile.getName() + "' -f '" + tmpFile.getName() + "' -s clientId=my_client3");
                assertExitCodeAndStreamSizes(exe, 1, 0, 1);
                Assertions.assertEquals("Failed to set attribute 'clientId' on document type 'oidc'", exe.stderrLines().get(0));
            }

            exe = execute("create --insecure --config '" + configFile.getName() + "' -i -s client_name=my_client3");
            assertExitCodeAndStreamSizes(exe, 0, 1, 2);
            String generatedId = exe.stdoutLines().get(0);
            Assertions.assertFalse(generatedId.isBlank(), "only generated client_id returned");
            Assertions.assertTrue(handler.loadConfig().sessionRealmConfigData().getClients().containsKey(generatedId));

            exe = execute("create --insecure --config '" + configFile.getName() + "' -s client_name=my_client4");
            assertExitCodeAndStreamSizes(exe, 0, 0, 3);
            Assertions.assertTrue(exe.stderrLines().get(exe.stderrLines().size() - 1).startsWith("Registered new client with client_id '"));

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



                // try use incompatible endpoint override
                exe = execute("create --config '" + configFile.getName() + "' -e default -f '" + tmpFile.getName() + "'");

                assertExitCodeAndStreamSizes(exe, 2, 0, 2);
                Assertions.assertTrue(exe.stderrLines().get(0).contains("Endpoint not supported: default"));
            }

            // TODO: SAML is not tested with FIPS enabled as it does not work. This needs to be revisited when SAML works with FIPS
            if (!AbstractExec.isFips()) {

                // test create saml formated xml - format autodetection
                File samlSpMetaFile = resourceFile("org/keycloak/tests/cli/kcreg/saml-sp-metadata.xml");
                Assertions.assertTrue(samlSpMetaFile.isFile(), "saml-sp-metadata.xml exists");

                exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f - < '" + samlSpMetaFile.getAbsolutePath() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
                Assertions.assertNotNull(client.getId(), "id");
                Assertions.assertEquals("http://localhost:8080/sales-post-enc/", client.getClientId(), "clientId");
                Assertions.assertEquals(Arrays.asList("http://localhost:8081/sales-post-enc/saml"), client.getRedirectUris(), "redirectUris");
                Assertions.assertEquals("username", client.getAttributes().get("saml_name_id_format"), "attributes.saml_name_id_format");
                Assertions.assertEquals("http://localhost:8081/sales-post-enc/saml", client.getAttributes().get("saml_assertion_consumer_url_post"), "attributes.saml_assertion_consumer_url_post");
                Assertions.assertEquals("RSA_SHA256", client.getAttributes().get("saml.signature.algorithm"), "attributes.saml.signature.algorithm");
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
            exe = execute("create --config '" + configFile.getName() + "' --insecure --server " + keycloakUrls.getBase() + " --realm test -o -s client_name=authz-client -s 'grant_types=[\"authorization_code\",\"client_credentials\",\"" + OAuth2Constants.UMA_GRANT_TYPE + "\"]' -t " + token);
            assertExitCodeAndStdErrSize(exe, 0, 2);
            OIDCClientRepresentation created = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

            RealmResource realm = adminClient.realm("test");
            ClientsResource clients = realm.clients();
            ClientRepresentation clientRep = clients.findByClientId(created.getClientId()).get(0);

            ClientResource client = clients.get(clientRep.getId());

            clientRep = client.toRepresentation();
            Assertions.assertTrue(clientRep.getAuthorizationServicesEnabled());

            ResourceServerRepresentation settings = client.authorization().getSettings();

            Assertions.assertEquals(PolicyEnforcementMode.ENFORCING, settings.getPolicyEnforcementMode());
            Assertions.assertTrue(settings.isAllowRemoteResourceManagement());

            List<RoleRepresentation> roles = client.roles().list();

            Assertions.assertEquals(1, roles.size());
            Assertions.assertEquals("uma_protection", roles.get(0).getName());

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

                client = clients.get(clients.findByClientId(oidcClient.getClientId()).get(0).getId());

                clientRep = client.toRepresentation();
                Assertions.assertTrue(clientRep.getAuthorizationServicesEnabled());

                settings = client.authorization().getSettings();

                Assertions.assertEquals(PolicyEnforcementMode.ENFORCING, settings.getPolicyEnforcementMode());
                Assertions.assertTrue(settings.isAllowRemoteResourceManagement());

                roles = client.roles().list();

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

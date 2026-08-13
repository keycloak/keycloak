package org.keycloak.testsuite.cli.registration;

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
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.cli.KcRegExec.execute;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegCreateTest extends AbstractRegCliTest {

    @Before
    public void assumeTLSEnabled() {
        Assume.assumeTrue(AUTH_SERVER_SSL_REQUIRED);
    }

    @Test
    public void testCreateWithRealmOverride() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            // authenticate as a regular user against one realm
            KcRegExec exe = execute("config credentials -x --config '" + configFile.getName() +
                    "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm master --user admin --password admin");

            assertExitCodeAndStreamSizes(exe, 0, 0, 3);

            // use initial token of another realm with server, and realm override
            String token = issueInitialAccessToken("test");
            exe = execute("create --config '" + configFile.getName() + "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm test -s clientId=my_first_client -t " + token);

            assertExitCodeAndStreamSizes(exe, 0, 0, 3);
        }
    }


    @Test
    public void testCreateThoroughly() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {
            // set initial access token in config
            String token = issueInitialAccessToken("test");

            final String realm = "test";

            KcRegExec exe = execute("config initial-token -x --config '" + configFile.getName() +
                    "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm " + realm + " " + token);

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // check that current server, realm, and initial token are saved in the file
            ConfigData config = handler.loadConfig();
            Assertions.assertEquals(oauth.AUTH_SERVER_ROOT, config.getServerUrl(), "Config serverUrl");
            Assertions.assertEquals(realm, config.getRealm(), "Config realm");
            Assertions.assertEquals(token, config.ensureRealmConfigData(oauth.AUTH_SERVER_ROOT, realm).getInitialToken(), "Config initial access token");

            // create configuration from file using stdin redirect ... output an object
            String content = "{\n" +
                    "        \"clientId\": \"my_client\",\n" +
                    "        \"enabled\": true,\n" +
                    "        \"redirectUris\": [\"http://localhost:8980/myapp/*\"],\n" +
                    "        \"serviceAccountsEnabled\": true,\n" +
                    "        \"name\": \"My Client App\",\n" +
                    "        \"implicitFlowEnabled\": false,\n" +
                    "        \"publicClient\": true,\n" +
                    "        \"protocol\": \"openid-connect\",\n" +
                    "        \"webOrigins\": [\"http://localhost:8980/myapp\"],\n" +
                    "        \"consentRequired\": false,\n" +
                    "        \"baseUrl\": \"http://localhost:8980/myapp\",\n" +
                    "        \"rootUrl\": \"http://localhost:8980/myapp\",\n" +
                    "        \"bearerOnly\": true,\n" +
                    "        \"standardFlowEnabled\": true\n" +
                    "}";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 2);

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
                Assertions.assertEquals("openid-connect", client.getProtocol(), "protocol");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp"), client.getWebOrigins(), "webOrigins");
                Assertions.assertEquals(false, client.isConsentRequired(), "consentRequired");
                Assertions.assertEquals("http://localhost:8980/myapp", client.getBaseUrl(), "baseUrl");
                Assertions.assertEquals("http://localhost:8980/myapp", client.getRootUrl(), "rootUrl");
                Assertions.assertEquals(true, client.isStandardFlowEnabled(), "bearerOnly");
                Assertions.assertNull(client.getProtocolMappers(), "mappers are null");

                // create configuration from file as a template and override clientId and other attributes ... output an object
                exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName() +
                        "' -s clientId=my_client2 -s enabled=false -s 'redirectUris=[\"http://localhost:8980/myapp2/*\"]'" +
                        " -s 'name=My Client App II' -s protocol=openid-connect -s 'webOrigins=[\"http://localhost:8980/myapp2\"]'" +
                        " -s baseUrl=http://localhost:8980/myapp2 -s rootUrl=http://localhost:8980/myapp2");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                ClientRepresentation client2 = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
                Assertions.assertNotNull(client2.getId(), "id");
                Assertions.assertEquals("my_client2", client2.getClientId(), "clientId");
                Assertions.assertEquals(false, client2.isEnabled(), "enabled");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp2/*"), client2.getRedirectUris(), "redirectUris");
                Assertions.assertEquals(true, client2.isServiceAccountsEnabled(), "serviceAccountsEnabled");
                Assertions.assertEquals("My Client App II", client2.getName(), "name");
                Assertions.assertEquals(false, client2.isImplicitFlowEnabled(), "implicitFlowEnabled");
                Assertions.assertEquals(true, client2.isPublicClient(), "publicClient");
                Assertions.assertEquals("openid-connect", client2.getProtocol(), "protocol");
                Assertions.assertEquals(Arrays.asList("http://localhost:8980/myapp2"), client2.getWebOrigins(), "webOrigins");
                Assertions.assertEquals(false, client2.isConsentRequired(), "consentRequired");
                Assertions.assertEquals("http://localhost:8980/myapp2", client2.getBaseUrl(), "baseUrl");
                Assertions.assertEquals("http://localhost:8980/myapp2", client2.getRootUrl(), "rootUrl");
                Assertions.assertEquals(true, client2.isStandardFlowEnabled(), "bearerOnly");
                Assertions.assertNull(client2.getProtocolMappers(), "mappers are null");


                // check that using an invalid attribute key is not ignored
                exe = execute("create --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName() + "' -s client_id=my_client3");

                assertExitCodeAndStreamSizes(exe, 1, 0, 1);
                Assertions.assertEquals("Failed to set attribute 'client_id' on document type 'default'", exe.stderrLines().get(0));
            }

            // simple create, output an id
            exe = execute("create --insecure --config '" + configFile.getName() + "' -i -s clientId=my_client3");

            assertExitCodeAndStreamSizes(exe, 0, 1, 2);
            Assertions.assertEquals("my_client3", exe.stdoutLines().get(0), "only clientId returned");

            // simple create, default output
            exe = execute("create --insecure --config '" + configFile.getName() + "' -s clientId=my_client4");

            assertExitCodeAndStreamSizes(exe, 0, 0, 3);
            Assertions.assertEquals("Registered new client with client_id 'my_client4'", exe.stderrLines().get(exe.stderrLines().size() - 1), "only clientId returned");



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

                assertExitCodeAndStreamSizes(exe, 1, 0, 1);
                Assertions.assertEquals("Attribute 'redirect_uris' not supported on document type 'default'", exe.stderrLines().get(0), "Error message");
            }

            // TODO: SAML is not tested with FIPS enabled as it does not work. This needs to be revisited when SAML works with FIPS
            if (AuthServerTestEnricher.AUTH_SERVER_FIPS_MODE == FipsMode.DISABLED) {

                // test create saml formated xml - format autodetection
                File samlSpMetaFile = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcreg/saml-sp-metadata.xml");
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
    public void testCreateWithAuthorizationServices() throws IOException {
        ProfileAssume.assumeFeatureEnabled(Profile.Feature.AUTHORIZATION);

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            KcRegExec exe = execute("config credentials -x --config '" + configFile.getName() +
                    "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm master --user admin --password admin");
            assertExitCodeAndStreamSizes(exe, 0, 0, 3);

            String token = issueInitialAccessToken("test");
            exe = execute("create --config '" + configFile.getName() + "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm test -s clientId=authz-client -s authorizationServicesEnabled=true -t " + token);
            assertExitCodeAndStreamSizes(exe, 0, 0, 3);

            RealmResource realm = adminClient.realm("test");
            ClientsResource clients = realm.clients();
            ClientRepresentation clientRep = clients.findByClientId("authz-client").get(0);

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

                client = clients.get(oidcClient.getClientId());

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

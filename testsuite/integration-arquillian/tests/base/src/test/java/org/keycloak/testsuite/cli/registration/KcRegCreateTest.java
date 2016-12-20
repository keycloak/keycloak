package org.keycloak.testsuite.cli.registration;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.config.FileConfigHandler;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.keycloak.testsuite.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegCreateTest extends AbstractRegCliTest {

    @Test
    public void testCreateWithRealmOverride() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            // authenticate as a regular user against one realm
            KcRegExec exe = execute("config credentials -x --config '" + configFile.getName() +
                    "' --server " + serverUrl + " --realm master --user admin --password admin");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);

            // use initial token of another realm with server, and realm override
            String token = issueInitialAccessToken("test");
            exe = execute("create --config '" + configFile.getName() + "' --server " + serverUrl + " --realm test -s clientId=my_first_client -t " + token);

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);
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
                    "' --server " + serverUrl + " --realm " + realm + " " + token);

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // check that current server, realm, and initial token are saved in the file
            ConfigData config = handler.loadConfig();
            Assert.assertEquals("Config serverUrl", serverUrl, config.getServerUrl());
            Assert.assertEquals("Config realm", realm, config.getRealm());
            Assert.assertEquals("Config initial access token", token, config.ensureRealmConfigData(serverUrl, realm).getInitialToken());

            // create configuration from file using stdin redirect ... output an object
            String content = "{\n" +
                    "        \"clientId\": \"my_client\",\n" +
                    "        \"enabled\": true,\n" +
                    "        \"redirectUris\": [\"http://localhost:8980/myapp/*\"],\n" +
                    "        \"serviceAccountsEnabled\": true,\n" +
                    "        \"name\": \"My Client App\",\n" +
                    "        \"implicitFlowEnabled\": false,\n" +
                    "        \"publicClient\": true,\n" +
                    "        \"protocol\": \"leycloak-oidc\",\n" +
                    "        \"webOrigins\": [\"http://localhost:8980/myapp\"],\n" +
                    "        \"consentRequired\": false,\n" +
                    "        \"baseUrl\": \"http://localhost:8980/myapp\",\n" +
                    "        \"rootUrl\": \"http://localhost:8980/myapp\",\n" +
                    "        \"bearerOnly\": true,\n" +
                    "        \"standardFlowEnabled\": true\n" +
                    "}";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create --config '" + configFile.getName() + "' -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 0);

                ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
                Assert.assertNotNull("id", client.getId());
                Assert.assertEquals("clientId", "my_client", client.getClientId());
                Assert.assertEquals("enabled", true, client.isEnabled());
                Assert.assertEquals("redirectUris", Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris());
                Assert.assertEquals("serviceAccountsEnabled", true, client.isServiceAccountsEnabled());
                Assert.assertEquals("name", "My Client App", client.getName());
                Assert.assertEquals("implicitFlowEnabled", false, client.isImplicitFlowEnabled());
                Assert.assertEquals("publicClient", true, client.isPublicClient());
                // note there is no server-side check if protocol is supported
                Assert.assertEquals("protocol", "leycloak-oidc", client.getProtocol());
                Assert.assertEquals("webOrigins", Arrays.asList("http://localhost:8980/myapp"), client.getWebOrigins());
                Assert.assertEquals("consentRequired", false, client.isConsentRequired());
                Assert.assertEquals("baseUrl", "http://localhost:8980/myapp", client.getBaseUrl());
                Assert.assertEquals("rootUrl", "http://localhost:8980/myapp", client.getRootUrl());
                Assert.assertEquals("bearerOnly", true, client.isStandardFlowEnabled());
                Assert.assertFalse("mappers not empty", client.getProtocolMappers().isEmpty());

                // create configuration from file as a template and override clientId and other attributes ... output an object
                exe = execute("create --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName() +
                        "' -s clientId=my_client2 -s enabled=false -s 'redirectUris=[\"http://localhost:8980/myapp2/*\"]'" +
                        " -s 'name=My Client App II' -s protocol=keycloak-oidc -s 'webOrigins=[\"http://localhost:8980/myapp2\"]'" +
                        " -s baseUrl=http://localhost:8980/myapp2 -s rootUrl=http://localhost:8980/myapp2");

                assertExitCodeAndStdErrSize(exe, 0, 0);

                ClientRepresentation client2 = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
                Assert.assertNotNull("id", client2.getId());
                Assert.assertEquals("clientId", "my_client2", client2.getClientId());
                Assert.assertEquals("enabled", false, client2.isEnabled());
                Assert.assertEquals("redirectUris", Arrays.asList("http://localhost:8980/myapp2/*"), client2.getRedirectUris());
                Assert.assertEquals("serviceAccountsEnabled", true, client2.isServiceAccountsEnabled());
                Assert.assertEquals("name", "My Client App II", client2.getName());
                Assert.assertEquals("implicitFlowEnabled", false, client2.isImplicitFlowEnabled());
                Assert.assertEquals("publicClient", true, client2.isPublicClient());
                Assert.assertEquals("protocol", "keycloak-oidc", client2.getProtocol());
                Assert.assertEquals("webOrigins", Arrays.asList("http://localhost:8980/myapp2"), client2.getWebOrigins());
                Assert.assertEquals("consentRequired", false, client2.isConsentRequired());
                Assert.assertEquals("baseUrl", "http://localhost:8980/myapp2", client2.getBaseUrl());
                Assert.assertEquals("rootUrl", "http://localhost:8980/myapp2", client2.getRootUrl());
                Assert.assertEquals("bearerOnly", true, client2.isStandardFlowEnabled());
                Assert.assertFalse("mappers not empty", client2.getProtocolMappers().isEmpty());


                // check that using an invalid attribute key is not ignored
                exe = execute("create --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName() + "' -s client_id=my_client3");

                assertExitCodeAndStreamSizes(exe, 1, 0, 1);
                Assert.assertEquals("Failed to set attribute 'client_id' on document type 'default'", exe.stderrLines().get(0));
            }

            // simple create, output an id
            exe = execute("create --config '" + configFile.getName() + "' -i -s clientId=my_client3");

            assertExitCodeAndStreamSizes(exe, 0, 1, 0);
            Assert.assertEquals("only clientId returned", "my_client3", exe.stdoutLines().get(0));

            // simple create, default output
            exe = execute("create --config '" + configFile.getName() + "' -s clientId=my_client4");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);
            Assert.assertEquals("only clientId returned", "Registered new client with client_id 'my_client4'", exe.stderrLines().get(0));



            // create using oidc endpoint - autodetect format
            content = "        {\n" +
                    "            \"redirect_uris\" : [ \"http://localhost:8980/myapp/*\" ],\n" +
                    "            \"grant_types\" : [ \"authorization_code\", \"client_credentials\", \"refresh_token\" ],\n" +
                    "            \"response_types\" : [ \"code\", \"none\" ],\n" +
                    "            \"client_name\" : \"My Client App\",\n" +
                    "            \"client_uri\" : \"http://localhost:8980/myapp\"\n" +
                    "        }";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create --config '" + configFile.getName() + "' -s 'client_name=My Client App V' " +
                        " -s 'redirect_uris=[\"http://localhost:8980/myapp5/*\"]' -s client_uri=http://localhost:8980/myapp5" +
                        " -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 0);

                OIDCClientRepresentation client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

                Assert.assertNotNull("clientId", client.getClientId());
                Assert.assertEquals("redirect_uris", Arrays.asList("http://localhost:8980/myapp5/*"), client.getRedirectUris());
                Assert.assertEquals("grant_types", Arrays.asList("authorization_code", "client_credentials", "refresh_token"), client.getGrantTypes());
                Assert.assertEquals("response_types", Arrays.asList("code", "none"), client.getResponseTypes());
                Assert.assertEquals("client_name", "My Client App V", client.getClientName());
                Assert.assertEquals("client_uri", "http://localhost:8980/myapp5", client.getClientUri());



                // try use incompatible endpoint override
                exe = execute("create --config '" + configFile.getName() + "' -e default -f '" + tmpFile.getName() + "'");

                assertExitCodeAndStreamSizes(exe, 1, 0, 1);
                Assert.assertEquals("Error message", "Attribute 'redirect_uris' not supported on document type 'default'", exe.stderrLines().get(0));
            }


            // test create saml formated xml - format autodetection
            File samlSpMetaFile = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcreg/saml-sp-metadata.xml");
            Assert.assertTrue("saml-sp-metadata.xml exists", samlSpMetaFile.isFile());

            exe = execute("create --config '" + configFile.getName() + "' -o -f - < '" + samlSpMetaFile.getAbsolutePath() + "'");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assert.assertNotNull("id", client.getId());
            Assert.assertEquals("clientId", "http://localhost:8080/sales-post-enc/", client.getClientId());
            Assert.assertEquals("redirectUris", Arrays.asList("http://localhost:8081/sales-post-enc/saml"), client.getRedirectUris());
            Assert.assertEquals("attributes.saml_name_id_format", "username", client.getAttributes().get("saml_name_id_format"));
            Assert.assertEquals("attributes.saml_assertion_consumer_url_post", "http://localhost:8081/sales-post-enc/saml", client.getAttributes().get("saml_assertion_consumer_url_post"));
            Assert.assertEquals("attributes.saml.signature.algorithm", "RSA_SHA256", client.getAttributes().get("saml.signature.algorithm"));


            // delete initial token
            exe = execute("config initial-token --config '" + configFile.getName() + "' --server " + serverUrl + " --realm " + realm + " --delete");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            config = handler.loadConfig();
            Assert.assertNull("initial token == null", config.ensureRealmConfigData(serverUrl, realm).getInitialToken());
        }
    }
}

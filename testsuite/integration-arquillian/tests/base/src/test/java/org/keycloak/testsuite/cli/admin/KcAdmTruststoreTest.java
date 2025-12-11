package org.keycloak.testsuite.cli.admin;

import java.io.File;
import java.io.IOException;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.testsuite.cli.KcAdmExec;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.client.admin.cli.KcAdmMain.DEFAULT_CONFIG_FILE_PATH;
import static org.keycloak.client.cli.util.OsUtil.EOL;
import static org.keycloak.testsuite.cli.KcAdmExec.execute;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmTruststoreTest extends AbstractAdmCliTest {

    @Test
    public void testTruststore() throws IOException {

        File truststore = new File("src/test/resources/keystore/keycloak.truststore");

        KcAdmExec exe = execute("config truststore --no-config '" + truststore.getAbsolutePath() + "'");

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assert.assertEquals("stderr first line", "Unsupported option: --no-config", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + KcAdmMain.CMD + " config truststore --help' for more information on the available options.", exe.stderrLines().get(1));

        // only run this test if ssl protected keycloak server is available
        if (!AUTH_SERVER_SSL_REQUIRED) {
            System.out.println("TEST SKIPPED - This test requires HTTPS. Run with '-Pauth-server-quarkus -Dauth.server.ssl.required=true'");
            return;
        }

        initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(FileConfigHandler.getConfigFile())) {

            if (runIntermittentlyFailingTests()) {
                // configure truststore
                exe = execute("config truststore --config '" + configFile.getName() + "' '" + truststore.getAbsolutePath() + "'");

                assertExitCodeAndStreamSizes(exe, 0, 0, 0);


                // perform authentication against server - asks for password, then for truststore password
                exe = KcAdmExec.newBuilder()
                        .argsLine("config credentials --server " + OAuthClient.AUTH_SERVER_ROOT + " --realm test --user user1" +
                                " --config '" + configFile.getName() + "'")
                        .executeAsync();

                exe.waitForStdout("Enter password: ");
                exe.sendToStdin("userpass" + EOL);
                exe.waitForStdout("Enter truststore password: ");
                exe.sendToStdin("secret" + EOL);
                exe.waitCompletion();

                assertExitCodeAndStreamSizes(exe, 0, 2, 1);


                // configure truststore with password
                exe = execute("config truststore --config '" + configFile.getName() + "' --trustpass secret '" + truststore.getAbsolutePath() + "'");

                assertExitCodeAndStreamSizes(exe, 0, 0, 0);

                // perform authentication against server - asks for password, then for truststore password
                exe = KcAdmExec.newBuilder()
                        .argsLine("config credentials --server " + OAuthClient.AUTH_SERVER_ROOT + " --realm test --user user1" +
                                " --config '" + configFile.getName() + "'")
                        .executeAsync();

                exe.waitForStdout("Enter password: ");
                exe.sendToStdin("userpass" + EOL);
                exe.waitCompletion();

                assertExitCodeAndStreamSizes(exe, 0, 1, 1);

            } else {
                System.out.println("TEST SKIPPED PARTIALLY - This test currently suffers from intermittent failures. Use -Dtest.intermittent=true to run it in full.");
            }
        }

        // configure truststore with password
        exe = execute("config truststore --trustpass secret '" + truststore.getAbsolutePath() + "'");
        assertExitCodeAndStreamSizes(exe, 0, 0, 0);

        // perform authentication against server - asks for password, then for truststore password
        exe = execute("config credentials --server " + serverUrl + " --realm test --user user1 --password userpass");
        assertExitCodeAndStreamSizes(exe, 0, 0, 1);

        exe = execute("config truststore --delete");
        assertExitCodeAndStreamSizes(exe, 0, 0, 0);

        exe = execute("config truststore --delete '" + truststore.getAbsolutePath() + "'");
        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assert.assertEquals("incompatible", "Option --delete is mutually exclusive with specifying a TRUSTSTORE", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + KcAdmMain.CMD + " config truststore --help' for more information on the available options.", exe.stderrLines().get(1));

        exe = execute("config truststore --delete --trustpass secret");
        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assert.assertEquals("no truststore error", "Options --trustpass and --delete are mutually exclusive", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + KcAdmMain.CMD + " config truststore --help' for more information on the available options.", exe.stderrLines().get(1));

        FileConfigHandler cfghandler = new FileConfigHandler();
        FileConfigHandler.setConfigFile(DEFAULT_CONFIG_FILE_PATH);
        ConfigData config = cfghandler.loadConfig();
        Assert.assertNull("truststore null", config.getTruststore());
        Assert.assertNull("trustpass null", config.getTrustpass());


        // perform no-config CRUD test against ssl protected endpoint
        testCRUDWithOnTheFlyAuth(serverUrl,
                "--user user1 --password userpass", " --truststore '" + truststore.getAbsolutePath() + "' --trustpass secret",
                "Logging into " + serverUrl + " as user user1 of realm test");
    }
}

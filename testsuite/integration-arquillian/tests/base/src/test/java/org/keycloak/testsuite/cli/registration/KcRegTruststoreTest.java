package org.keycloak.testsuite.cli.registration;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.config.FileConfigHandler;
import org.keycloak.client.registration.cli.util.ConfigUtil;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.TempFileResource;

import java.io.File;
import java.io.IOException;

import static org.keycloak.client.registration.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_PATH;
import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.EOL;
import static org.keycloak.testsuite.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegTruststoreTest extends AbstractRegCliTest {

    static File TRUSTSTORE = new File("src/test/resources/keystore/keycloak.truststore");

    @Test
    public void testTruststore() throws IOException {

        // only run this test if ssl protected keycloak server is available
        if (!isAuthServerSSL()) {
            System.out.println("TEST SKIPPED - This test requires HTTPS. Run with '-Pauth-server-wildfly,ssl'");
            return;
        }

        // Authenticate against server - with username and password - should fail since we didn't set up truststore yet
        KcRegExec exe = execute("config credentials --server " + serverUrl + " --realm test --user user1 --password userpass");
        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertTrue("no valid certificate", exe.stderrLines().get(1).indexOf("unable to find valid certification path") != -1);

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            if (runIntermittentlyFailingTests()) {
                // configure truststore
                exe = execute("config truststore --config '" + configFile.getName() + "' '" + TRUSTSTORE.getAbsolutePath() + "'");

                assertExitCodeAndStreamSizes(exe, 0, 0, 0);


                // perform authentication against server - asks for password, then for truststore password
                exe = KcRegExec.newBuilder()
                        .argsLine("config credentials --server " + serverUrl + " --realm test --user user1" +
                                " --config '" + configFile.getName() + "'")
                        .executeAsync();

                exe.waitForStdout("Enter password: ");
                exe.sendToStdin("userpass" + EOL);
                exe.waitForStdout("Enter truststore password: ");
                exe.sendToStdin("secret" + EOL);
                exe.waitCompletion();

                assertExitCodeAndStreamSizes(exe, 0, 2, 1);


                // configure truststore with password
                exe = execute("config truststore --config '" + configFile.getName() + "' --trustpass secret '" + TRUSTSTORE.getAbsolutePath() + "'");

                assertExitCodeAndStreamSizes(exe, 0, 0, 0);

                // perform authentication against server - asks for password, truststore is accessed automatically using saved password
                exe = KcRegExec.newBuilder()
                        .argsLine("config credentials --server " + serverUrl + " --realm test --user user1" +
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

        // configure truststore - protect it with password
        exe = execute("config truststore --trustpass secret '" + TRUSTSTORE.getAbsolutePath() + "'");
        assertExitCodeAndStreamSizes(exe, 0, 0, 0);

        // perform authentication against server - truststore is accessed automatically using the saved password
        exe = execute("config credentials --server " + serverUrl + " --realm test --user user1 --password userpass");
        assertExitCodeAndStreamSizes(exe, 0, 0, 1);

        exe = execute("config truststore --delete");
        assertExitCodeAndStreamSizes(exe, 0, 0, 0);

        exe = execute("config truststore --delete '" + TRUSTSTORE.getAbsolutePath() + "'");
        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("incompatible", "Option --delete is mutually exclusive with specifying a TRUSTSTORE", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " help config truststore' for more information", exe.stderrLines().get(1));

        exe = execute("config truststore --delete --trustpass secret");
        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("no truststore error", "Options --trustpass and --delete are mutually exclusive", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " help config truststore' for more information", exe.stderrLines().get(1));

        FileConfigHandler cfghandler = new FileConfigHandler();
        cfghandler.setConfigFile(DEFAULT_CONFIG_FILE_PATH);
        ConfigData config = cfghandler.loadConfig();
        Assert.assertNull("truststore null", config.getTruststore());
        Assert.assertNull("trustpass null", config.getTrustpass());


        // perform no-config CRUD test against ssl protected endpoint
        testCRUDWithOnTheFlyAuth(serverUrl,
                "--user user1 --password userpass", " --truststore '" + TRUSTSTORE.getAbsolutePath() + "' --trustpass secret",
                "Logging into " + serverUrl + " as user user1 of realm test");
    }


    @Test
    public void testUpdateTokenTruststore() throws IOException {

        // only run this test if ssl protected keycloak server is available
        if (!isAuthServerSSL()) {
            System.out.println("TEST SKIPPED - This test requires HTTPS. Run with '-Pauth-server-wildfly,ssl'");
            return;
        }

        FileConfigHandler handler = initCustomConfigFile();
        ConfigUtil.setHandler(handler);

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {
            // configure truststore - protect it with password
            KcRegExec exe = execute("config truststore  --config '" + configFile.getName() + "' --trustpass secret '" + TRUSTSTORE.getAbsolutePath() + "'");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            KcRegUpdateTokenTest.testUpdateTokenUsingConfig(serverUrl, configFile.getFile(), " --truststore '" + TRUSTSTORE.getAbsolutePath() + "' --trustpass secret");
        }
    }
}

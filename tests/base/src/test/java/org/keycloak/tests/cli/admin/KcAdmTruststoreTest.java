package org.keycloak.tests.cli.admin;

import java.io.File;
import java.io.IOException;

import org.keycloak.client.admin.cli.KcAdmMain;
import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.cli.KcAdmExec;
import org.keycloak.tests.cli.TempFileResource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.client.admin.cli.KcAdmMain.DEFAULT_CONFIG_FILE_PATH;
import static org.keycloak.client.cli.util.OsUtil.EOL;
import static org.keycloak.tests.cli.KcAdmExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
class KcAdmTruststoreTest extends AbstractAdmCliTest {

    @Test
    void testTruststore() throws IOException {

        File truststore = resourceFile("org/keycloak/tests/cli/keycloak.truststore");

        KcAdmExec exe = execute("config truststore --no-config '" + truststore.getAbsolutePath() + "'");

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assertions.assertEquals("Unsupported option: --no-config", exe.stderrLines().get(0), "stderr first line");
        Assertions.assertEquals("Try '" + KcAdmMain.CMD + " config truststore --help' for more information on the available options.", exe.stderrLines().get(1), "try help");

        initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(FileConfigHandler.getConfigFile())) {

            if (runIntermittentlyFailingTests()) {
                // configure truststore
                exe = execute("config truststore --config '" + configFile.getName() + "' '" + truststore.getAbsolutePath() + "'");

                assertExitCodeAndStreamSizes(exe, 0, 0, 0);


                // perform authentication against server - asks for password, then for truststore password
                exe = KcAdmExec.newBuilder()
                        .argsLine("config credentials --server " + keycloakUrls.getBase() + " --realm test --user user1" +
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
                        .argsLine("config credentials --server " + keycloakUrls.getBase() + " --realm test --user user1" +
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
        Assertions.assertEquals("Option --delete is mutually exclusive with specifying a TRUSTSTORE", exe.stderrLines().get(0), "incompatible");
        Assertions.assertEquals("Try '" + KcAdmMain.CMD + " config truststore --help' for more information on the available options.", exe.stderrLines().get(1), "try help");

        exe = execute("config truststore --delete --trustpass secret");
        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assertions.assertEquals("Options --trustpass and --delete are mutually exclusive", exe.stderrLines().get(0), "no truststore error");
        Assertions.assertEquals("Try '" + KcAdmMain.CMD + " config truststore --help' for more information on the available options.", exe.stderrLines().get(1), "try help");

        FileConfigHandler cfghandler = new FileConfigHandler();
        FileConfigHandler.setConfigFile(DEFAULT_CONFIG_FILE_PATH);
        ConfigData config = cfghandler.loadConfig();
        Assertions.assertNull(config.getTruststore(), "truststore null");
        Assertions.assertNull(config.getTrustpass(), "trustpass null");


        // perform no-config CRUD test against ssl protected endpoint
        testCRUDWithOnTheFlyAuth(serverUrl,
                "--user user1 --password userpass", " --truststore '" + truststore.getAbsolutePath() + "' --trustpass secret",
                "Logging into " + serverUrl + " as user user1 of realm test");
    }
}

package org.keycloak.testsuite.cli.registration;

import java.io.IOException;

import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.TempFileResource;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.client.cli.util.OsUtil.EOL;
import static org.keycloak.client.registration.cli.KcRegMain.CMD;
import static org.keycloak.testsuite.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegConfigTest extends AbstractRegCliTest {

    @Test
    public void testRegistrationToken() throws IOException {

        initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(FileConfigHandler.getConfigFile())) {

            // without --server
            KcRegExec exe = execute("config registration-token --config '" + configFile.getName() + "' ");
            assertExitCodeAndStreamSizes(exe, 2, 0, 2);
            Assert.assertEquals("error message", "Required option not specified: --server", exe.stderrLines().get(0));
            Assert.assertEquals("try help", "Try '" + CMD + " config registration-token --help' for more information on the available options.", exe.stderrLines().get(1));

            // without --realm
            exe = execute("config registration-token --config '" + configFile.getName() + "' --server http://localhost:8080/auth");
            assertExitCodeAndStreamSizes(exe, 2, 0, 2);
            Assert.assertEquals("error message", "Required option not specified: --realm", exe.stderrLines().get(0));
            Assert.assertEquals("try help", "Try '" + CMD + " config registration-token --help' for more information on the available options.", exe.stderrLines().get(1));

            // without --client
            exe = execute("config registration-token --config '" + configFile.getName() + "' --server http://localhost:8080/auth --realm test");
            assertExitCodeAndStreamSizes(exe, 2, 0, 2);
            Assert.assertEquals("error message", "Required option not specified: --client", exe.stderrLines().get(0));
            Assert.assertEquals("try help", "Try '" + CMD + " config registration-token --help' for more information on the available options.", exe.stderrLines().get(1));

            // specify token on cmdline
            exe = execute("config registration-token --config '" + configFile.getName() + "' --server http://localhost:8080/auth --realm test --client my_client NEWTOKEN");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            if (runIntermittentlyFailingTests()) {
                // don't specify token - must be prompted for it
                exe = KcRegExec.newBuilder()
                        .argsLine("config registration-token --config '" + configFile.getName() + "' --server http://localhost:8080/auth --realm test --client my_client")
                        .executeAsync();

                exe.waitForStdout("Enter Registration Access Token:");
                exe.sendToStdin("NEWTOKEN" + EOL);
                exe.waitCompletion();
                assertExitCodeAndStreamSizes(exe, 0, 1, 0);

            } else {
                System.out.println("TEST SKIPPED PARTIALLY - This test currently suffers from intermittent failures. Use -Dtest.intermittent=true to run it in full.");
            }

            // delete non-existent token
            exe = execute("config registration-token --config '" + configFile.getName() + "' --server http://localhost:8080/auth --realm test --client nonexistent --delete");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // delete token
            exe = execute("config registration-token --config '" + configFile.getName() + "' --server http://localhost:8080/auth --realm test --client my_client --delete");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);
        }
    }

    @Test
    public void testNoConfigOption() throws IOException {

        KcRegExec exe = execute("config registration-token --no-config --server http://localhost:8080/auth --realm test --client my_client --delete");
        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assert.assertEquals("stderr first line", "Unsupported option: --no-config", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " config registration-token --help' for more information on the available options.", exe.stderrLines().get(1));

        exe = execute("config initial-token --no-config --server http://localhost:8080/auth --realm test --delete");
        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assert.assertEquals("stderr first line", "Unsupported option: --no-config", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " config initial-token --help' for more information on the available options.", exe.stderrLines().get(1));

    }
}

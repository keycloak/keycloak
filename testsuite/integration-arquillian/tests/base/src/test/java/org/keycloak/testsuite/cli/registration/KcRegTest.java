package org.keycloak.testsuite.cli.registration;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.config.FileConfigHandler;
import org.keycloak.client.registration.cli.config.RealmConfigData;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.client.registration.cli.util.OsUtil.EOL;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import static org.keycloak.testsuite.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegTest extends AbstractRegCliTest {

    @Test
    public void testNoArgs() {
        /*
         *  Test (sub)commands without any arguments
         */
        KcRegExec exe = execute("");

        assertExitCodeAndStdErrSize(exe, 1, 0);

        List<String> lines = exe.stdoutLines();
        Assert.assertTrue("stdout output not empty", lines.size() > 0);
        Assert.assertEquals("stdout first line", "Keycloak Client Registration CLI", lines.get(0));
        Assert.assertEquals("stdout one but last line", "Use '" + KcRegExec.CMD + " help <command>' for more information about a given command.", lines.get(lines.size() - 2));
        Assert.assertEquals("stdout last line", "", lines.get(lines.size() - 1));


        /*
         * Test commands without arguments
         */
        exe = execute("config");
        assertExitCodeAndStreamSizes(exe, 1, 0, 1);
        Assert.assertEquals("error message",
                "Sub-command required by '" + CMD + " config' - one of: 'credentials', 'truststore', 'initial-token', 'registration-token'",
                exe.stderrLines().get(0));

        exe = execute("config credentials");
        assertExitCodeAndStdErrSize(exe, 1, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " config credentials --server SERVER_URL --realm REALM [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("config initial-token");
        assertExitCodeAndStdErrSize(exe, 1, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " config initial-token --server SERVER --realm REALM [--delete | TOKEN] [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("config registration-token");
        assertExitCodeAndStdErrSize(exe, 1, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " config registration-token --server SERVER --realm REALM --client CLIENT [--delete | TOKEN] [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("config truststore");
        assertExitCodeAndStdErrSize(exe, 1, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " config truststore [TRUSTSTORE | --delete] [--trustpass PASSWORD] [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("create");
        assertExitCodeAndStdErrSize(exe, 1, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " create [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "No file nor attribute values specified", exe.stderrLines().get(0));

        exe = execute("get");
        assertExitCodeAndStdErrSize(exe, 1, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " get CLIENT [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        exe = execute("update");
        assertExitCodeAndStdErrSize(exe, 1, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " update CLIENT [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "No file nor attribute values specified", exe.stderrLines().get(0));

        exe = execute("delete");
        assertExitCodeAndStdErrSize(exe, 1, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " delete CLIENT [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        exe = execute("attrs");
        Assert.assertEquals("exit code", 0, exe.exitCode());
        Assert.assertTrue("stdout has response", exe.stdoutLines().size() > 10);
        Assert.assertEquals("first line", "Attributes for default format:", exe.stdoutLines().get(0));

        exe = execute("update-token");
        assertExitCodeAndStdErrSize(exe, 1, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " update-token CLIENT [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        exe = execute("help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        lines = exe.stdoutLines();
        Assert.assertTrue("stdout output not empty", lines.size() > 0);
        Assert.assertEquals("stdout first line", "Keycloak Client Registration CLI", lines.get(0));
        Assert.assertEquals("stdout one but last line", "Use '" + KcRegExec.CMD + " help <command>' for more information about a given command.", lines.get(lines.size() - 2));
        Assert.assertEquals("stdout last line", "", lines.get(lines.size() - 1));
    }

    @Test
    public void testHelpGlobalOption() {
        /*
         *  Test --help for all commands
         */
        KcRegExec exe = execute("--help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Keycloak Client Registration CLI", exe.stdoutLines().get(0));

        exe = execute("create --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " create [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("get --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " get CLIENT [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("update --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " update CLIENT [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("delete --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " delete CLIENT [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("attrs --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " attrs [ATTRIBUTE] [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("update-token --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " update-token CLIENT [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("config --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " config SUB_COMMAND [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = execute("config credentials --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line",
                "Usage: " + CMD + " config credentials --server SERVER_URL --realm REALM [ARGUMENTS]",
                exe.stdoutLines().get(0));

        exe = execute("config initial-token --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line",
                "Usage: " + CMD + " config initial-token --server SERVER --realm REALM [--delete | TOKEN] [ARGUMENTS]",
                exe.stdoutLines().get(0));

        exe = execute("config registration-token --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line",
                "Usage: " + CMD + " config registration-token --server SERVER --realm REALM --client CLIENT [--delete | TOKEN] [ARGUMENTS]",
                exe.stdoutLines().get(0));

        exe = execute("config truststore --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line",
                "Usage: " + CMD + " config truststore [TRUSTSTORE | --delete] [--trustpass PASSWORD] [ARGUMENTS]",
                exe.stdoutLines().get(0));

    }

    @Test
    public void testBadCommand() {
        /*
         *  Test most basic execution with non-existent command
         */
        KcRegExec exe = execute("nonexistent");

        assertExitCodeAndStreamSizes(exe, 1, 0, 1);
        Assert.assertEquals("stderr first line", "Unknown command: nonexistent", exe.stderrLines().get(0));
    }

    @Test
    public void testBadOptionInPlaceOfCommand() {
        /*
         *  Test most basic execution with non-existent option
         */
        KcRegExec exe = execute("--nonexistent");

        assertExitCodeAndStreamSizes(exe, 1, 0, 1);
        Assert.assertEquals("stderr first line", "Unknown command: --nonexistent", exe.stderrLines().get(0));
    }

    @Test
    public void testBadOption() {
        /*
         *  Test sub-command execution with non-existent option
         */

        KcRegExec exe = execute("get my_client --nonexistent");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("stderr first line", "Invalid option: --nonexistent", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " help get' for more information", exe.stderrLines().get(1));
    }

    @Test
    public void testCredentialsServerAndRealmWithDefaultConfig() {
        /*
         *  Test without --server specified
         */
        KcRegExec exe = execute("config credentials --server " + serverUrl + " --realm master");

        assertExitCodeAndStreamSizes(exe, 0, 0, 0);
    }

    @Test
    public void testCredentialsNoServerWithDefaultConfig() {
        /*
         *  Test without --server specified
         */
        KcRegExec exe = execute("config credentials --realm master --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("stderr first line", "Required option not specified: --server", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " help config credentials' for more information", exe.stderrLines().get(1));
    }

    @Test
    public void testCredentialsNoRealmWithDefaultConfig() {
        /*
         *  Test without --server specified
         */
        KcRegExec exe = execute("config credentials --server " + serverUrl + " --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("stderr first line", "Required option not specified: --realm", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " help config credentials' for more information", exe.stderrLines().get(1));
    }

    @Test
    public void testCredentialsWithNoConfig() {
        /*
         *  Test with --no-config specified which is not supported
         */
        KcRegExec exe = KcRegExec.execute("config credentials --no-config --server " + serverUrl + " --realm master --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("stderr first line", "Unsupported option: --no-config", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " help config credentials' for more information", exe.stderrLines().get(1));
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testUserLoginWithDefaultConfig() {
        /*
         *  Test most basic user login, using the default admin-cli as a client
         */
        KcRegExec exe = execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 0, 0, 1);
        Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as user admin of realm master", exe.stderrLines().get(0));
    }

    @Test
    public void testUserLoginWithDefaultConfigInteractive() throws IOException {
        /*
         *  Test user login with interaction - provide user password after prompted for it
         */

        if (!runIntermittentlyFailingTests()) {
            System.out.println("TEST SKIPPED - This test currently suffers from intermittent failures. Use -Dtest.intermittent=true to run it.");
            return;
        }

        KcRegExec exe = KcRegExec.newBuilder()
                .argsLine("config credentials --server " + serverUrl + " --realm master --user admin")
                .executeAsync();

        exe.waitForStdout("Enter password: ");
        exe.sendToStdin("admin" + EOL);
        exe.waitCompletion();

        assertExitCodeAndStreamSizes(exe, 0, 1, 1);
        Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as user admin of realm master", exe.stderrLines().get(0));


        /*
         *  Run the test one more time with stdin redirect
         */
        File tmpFile = new File(KcRegExec.WORK_DIR + "/" + UUID.randomUUID().toString() + ".tmp");
        try {
            FileOutputStream tmpos = new FileOutputStream(tmpFile);
            tmpos.write("admin".getBytes());
            tmpos.write(EOL.getBytes());
            tmpos.close();

            exe = execute("config credentials --server " + serverUrl + " --realm master --user admin < '" + tmpFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 0, 1, 1);
            Assert.assertTrue("Enter password prompt", exe.stdoutLines().get(0).startsWith("Enter password: "));
            Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as user admin of realm master", exe.stderrLines().get(0));

        } finally {
            tmpFile.delete();
        }
    }

    @Test
    public void testClientLoginWithDefaultConfigInteractive() throws IOException {
        /*
         *  Test client login with interaction - login using service account, and provide a client secret after prompted for it
         */

        if (!runIntermittentlyFailingTests()) {
            System.out.println("TEST SKIPPED - This test currently suffers from intermittent failures. Use -Dtest.intermittent=true to run it.");
            return;
        }

        // use -Dtest.intermittent=true to run this test
        KcRegExec exe = KcRegExec.newBuilder()
                .argsLine("config credentials --server " + serverUrl + " --realm test --client reg-cli-secret")
                .executeAsync();

        exe.waitForStdout("Enter client secret: ");
        exe.sendToStdin("password" + EOL);
        exe.waitCompletion();

        assertExitCodeAndStreamSizes(exe, 0, 1, 1);
        Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as service-account-reg-cli-secret of realm test", exe.stderrLines().get(0));

        /*
         *  Run the test one more time with stdin redirect
         */
        File tmpFile = new File(KcRegExec.WORK_DIR + "/" + UUID.randomUUID().toString() + ".tmp");
        try {
            FileOutputStream tmpos = new FileOutputStream(tmpFile);
            tmpos.write("password".getBytes());
            tmpos.write(EOL.getBytes());
            tmpos.close();

            exe = KcRegExec.newBuilder()
                    .argsLine("config credentials --server " + serverUrl + " --realm test --client reg-cli-secret < '" + tmpFile.getName() + "'")
                    .execute();

            assertExitCodeAndStreamSizes(exe, 0, 1, 1);
            Assert.assertTrue("Enter client secret prompt", exe.stdoutLines().get(0).startsWith("Enter client secret: "));
            Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as service-account-reg-cli-secret of realm test", exe.stderrLines().get(0));
        } finally {
            tmpFile.delete();
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testUserLoginWithCustomConfig() {
        /*
         *  Test user login using a custom config file
         */
        FileConfigHandler handler = initCustomConfigFile();

        File configFile = new File(handler.getConfigFile());
        try {
            KcRegExec exe = execute("config credentials --server " + serverUrl + " --realm master" +
                    " --user admin --password admin --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);
            Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as user admin of realm master", exe.stderrLines().get(0));

            // make sure the config file exists, and has the right content
            ConfigData config = handler.loadConfig();
            Assert.assertEquals("serverUrl", serverUrl, config.getServerUrl());
            Assert.assertEquals("realm", "master", config.getRealm());
            RealmConfigData realmcfg = config.sessionRealmConfigData();
            Assert.assertNotNull("realm config data no null", realmcfg);
            Assert.assertEquals("realm cfg serverUrl", serverUrl, realmcfg.serverUrl());
            Assert.assertEquals("realm cfg realm", "master", realmcfg.realm());
            Assert.assertEquals("client id", "admin-cli", realmcfg.getClientId());
            Assert.assertNotNull("token not null", realmcfg.getToken());
            Assert.assertNotNull("refresh token not null", realmcfg.getRefreshToken());
            Assert.assertNotNull("token expires not null", realmcfg.getExpiresAt());
            Assert.assertNotNull("token expires in future", realmcfg.getExpiresAt() > System.currentTimeMillis());
            Assert.assertNotNull("refresh token expires not null", realmcfg.getRefreshExpiresAt());
            Assert.assertNotNull("refresh token expires in future", realmcfg.getRefreshExpiresAt() > System.currentTimeMillis());
            Assert.assertTrue("clients is empty", realmcfg.getClients().isEmpty());

        } finally {
            configFile.delete();
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testCustomConfigLoginCreateDelete() throws IOException {
        /*
         *  Test user login, create, delete session using a custom config file
         */

        // prepare for loading a config file
        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            KcRegExec exe = execute("config credentials --server " + serverUrl +
                    " --realm master --user admin --password admin --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);

            // remember the state of config file
            ConfigData config1 = handler.loadConfig();




            exe = execute("create --config '" + configFile.getName() + "' -s clientId=test-client -o");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            // check changes to config file
            ConfigData config2 = handler.loadConfig();
            assertFieldsEqualWithExclusions(config1, config2, "endpoints." + serverUrl + ".master.clients.test-client");

            // check that registration access token is now set
            Assert.assertNotNull(config2.sessionRealmConfigData().getClients().get("test-client"));


            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assert.assertEquals("clientId", "test-client", client.getClientId());
            Assert.assertNotNull("registrationAccessToken", client.getRegistrationAccessToken());
            Assert.assertEquals("registrationAccessToken in returned json same as in config",
                    config2.sessionRealmConfigData().getClients().get("test-client"), client.getRegistrationAccessToken());




            exe = execute("delete test-client --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // check changes to config file
            ConfigData config3 = handler.loadConfig();
            assertFieldsEqualWithExclusions(config2, config3, "endpoints." + serverUrl + ".master.clients.test-client");

            // check that registration access token is no longer there
            Assert.assertTrue("clients empty", config3.sessionRealmConfigData().getClients().isEmpty());
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testCRUDWithOnTheFlyUserAuth() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using username, and password.
         */
        testCRUDWithOnTheFlyAuth(serverUrl, "--user user1 --password userpass", "",
                "Logging into " + serverUrl + " as user user1 of realm test");
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testCRUDWithOnTheFlyUserAuthWithClientSecret() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using username, password, and client secret.
         */
        // try client without direct grants enabled
        KcRegExec exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client reg-cli-secret --secret password");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Client not allowed for direct access grants [unauthorized_client]", exe.stderrLines().get(1));


        // try wrong user password
        exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password wrong --client reg-cli-secret-direct --secret password");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Invalid user credentials [invalid_grant]", exe.stderrLines().get(1));


        // try wrong client secret
        exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client reg-cli-secret-direct --secret wrong");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Invalid client secret [unauthorized_client]", exe.stderrLines().get(1));


        // try whole CRUD
        testCRUDWithOnTheFlyAuth(serverUrl, "--user user1 --password userpass --client reg-cli-secret-direct --secret password", "",
                "Logging into " + serverUrl + " as user user1 of realm test");
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testCRUDWithOnTheFlyUserAuthWithSignedJwtClient() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using username, password, and client JWT signature.
         */
        File keystore = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcreg/reg-cli-keystore.jks");
        Assert.assertTrue("reg-cli-keystore.jks exists", keystore.isFile());

        // try client without direct grants enabled
        KcRegExec exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client reg-cli-jwt --keystore '" + keystore.getAbsolutePath() + "'" +
                " --storepass storepass --keypass keypass --alias reg-cli");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Client not allowed for direct access grants [unauthorized_client]", exe.stderrLines().get(1));


        // try wrong user password
        exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password wrong --client reg-cli-jwt-direct --keystore '" + keystore.getAbsolutePath() + "'" +
                " --storepass storepass --keypass keypass --alias reg-cli");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Invalid user credentials [invalid_grant]", exe.stderrLines().get(1));


        // try wrong storepass
        exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client reg-cli-jwt-direct --keystore '" + keystore.getAbsolutePath() + "'" +
                " --storepass wrong --keypass keypass --alias reg-cli");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Failed to load private key: Keystore was tampered with, or password was incorrect", exe.stderrLines().get(1));


        // try whole CRUD
        testCRUDWithOnTheFlyAuth(serverUrl,
                "--user user1 --password userpass  --client reg-cli-jwt-direct --keystore '" + keystore.getAbsolutePath() + "'" +
                        " --storepass storepass --keypass keypass --alias reg-cli", "",
                "Logging into " + serverUrl + " as user user1 of realm test");

    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testCRUDWithOnTheFlyServiceAccountWithClientSecret() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using only client secret - service account is used.
         */
        testCRUDWithOnTheFlyAuth(serverUrl, "--client reg-cli-secret --secret password", "",
                "Logging into " + serverUrl + " as service-account-reg-cli-secret of realm test");
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testCRUDWithOnTheFlyServiceAccountWithSignedJwtClient() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using only client JWT signature - service account is used.
         */
        File keystore = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcreg/reg-cli-keystore.jks");
        Assert.assertTrue("reg-cli-keystore.jks exists", keystore.isFile());

        testCRUDWithOnTheFlyAuth(serverUrl,
                "--client reg-cli-jwt --keystore '" + keystore.getAbsolutePath() + "' --storepass storepass --keypass keypass --alias reg-cli", "",
                "Logging into " + serverUrl + " as service-account-reg-cli-jwt of realm test");
    }

    @Test
    public void testCreateDeleteWithInitialAndRegistrationTokensWithUnsecureOption() throws IOException {
        /*
         *  Test create using initial client token, and subsequent delete using registration access token.
         *  A config file is used to save registration access token for newly created client.
         */
        testCreateDeleteWithInitialAndRegistrationTokensWithUnsecureOption(true);
    }

    @Test
    public void testCreateDeleteWithInitialAndRegistrationTokensWithUnsecureOptionNoConfig() throws IOException {
        /*
         *  Test create using initial client token, and subsequent delete using registration access token.
         *  No config file is used so registration access token for newly created client is not saved to config.
         */
        testCreateDeleteWithInitialAndRegistrationTokensWithUnsecureOption(false);
    }

    private void testCreateDeleteWithInitialAndRegistrationTokensWithUnsecureOption(boolean useConfig) throws IOException {
        Assume.assumeTrue(AUTH_SERVER_SSL_REQUIRED);

        // prepare for loading a config file
        // only used when useConfig is true
        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            String token = issueInitialAccessToken("master");

            final String realm = "master";

            KcRegExec exe = execute("create " + (useConfig ? ("--config '" + configFile.getAbsolutePath()) + "'" : "--no-config")
                    + " --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm " + realm + " -s clientId=test-client2 -o -t " + token);

            Assert.assertEquals("exitCode == 0", 0, exe.exitCode());


            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);

            Assert.assertEquals("clientId", "test-client2", client.getClientId());
            Assert.assertNotNull("registrationAccessToken", client.getRegistrationAccessToken());


            if (useConfig) {
                ConfigData config = handler.loadConfig();
                Assert.assertEquals("Registration Access Token in config file", client.getRegistrationAccessToken(),
                        config.ensureRealmConfigData(oauth.AUTH_SERVER_ROOT, realm).getClients().get("test-client2"));
            } else {
                Assert.assertFalse("There should be no config file", configFile.isFile());
            }

            exe = execute("delete test-client2 " + (useConfig ? ("--config '" + configFile.getAbsolutePath()) + "'" : "--no-config")
                    + " --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm " + realm + " -t " + client.getRegistrationAccessToken());

            assertExitCodeAndStreamSizes(exe, 0, 0, 2);
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testCreateWithAllowedHostsWithoutAuthenticationNoConfig() throws IOException {

        testCreateWithAllowedHostsWithoutAuthentication("test", false);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testCreateWithAllowedHostsWithoutAuthentication() throws IOException {

        testCreateWithAllowedHostsWithoutAuthentication("test", true);
    }

    private void testCreateWithAllowedHostsWithoutAuthentication(String realm, boolean useConfig) throws IOException {

        addLocalhostToAllowedHosts(realm);

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {
            KcRegExec exe = execute("create " + (useConfig ? ("--config '" + configFile.getAbsolutePath()) + "'" : "--no-config")
                    + " --server " + serverUrl + " --realm " + realm + " -s clientId=test-client -o");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);

            Assert.assertEquals("clientId", "test-client", client.getClientId());
            Assert.assertNotNull("registrationAccessToken", client.getRegistrationAccessToken());

            exe = execute("delete test-client " + (useConfig ? ("--config '" + configFile.getAbsolutePath()) + "'" : "--no-config")
                    + " --server " + serverUrl + " --realm " + realm + " -t " + client.getRegistrationAccessToken());

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);
        }
    }

}

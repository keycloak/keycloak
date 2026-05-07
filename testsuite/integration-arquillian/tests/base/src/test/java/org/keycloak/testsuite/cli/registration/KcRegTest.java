package org.keycloak.testsuite.cli.registration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.keycloak.client.cli.config.ConfigData;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.client.cli.config.RealmConfigData;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.KeystoreUtils;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import org.junit.Assume;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.client.cli.util.OsUtil.EOL;
import static org.keycloak.client.registration.cli.KcRegMain.CMD;
import static org.keycloak.testsuite.cli.KcRegExec.execute;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

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

        assertExitCodeAndStdErrSize(exe, 2, 0);

        List<String> lines = exe.stdoutLines();
        Assertions.assertTrue(lines.size() > 0, "stdout output not empty");
        Assertions.assertEquals("Keycloak Client Registration CLI", lines.get(0), "stdout first line");
        Assertions.assertEquals("Use '" + KcRegExec.CMD + " help <command>' for more information about a given command.", lines.get(lines.size() - 2), "stdout one but last line");
        Assertions.assertEquals("", lines.get(lines.size() - 1), "stdout last line");


        /*
         * Test commands without arguments
         */
        exe = execute("config");
        assertExitCodeAndStreamSizes(exe, 2, 8, 0);
        Assertions.assertEquals("Usage: kcreg.sh config SUB_COMMAND [ARGUMENTS]",
                exe.stdoutLines().get(0),
                "error message");

        exe = execute("config credentials");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "help message returned");
        Assertions.assertEquals("Usage: " + CMD + " config credentials --server SERVER_URL --realm REALM [ARGUMENTS]", exe.stdoutLines().get(0), "help message");

        exe = execute("config initial-token");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "help message returned");
        Assertions.assertEquals("Usage: " + CMD + " config initial-token --server SERVER --realm REALM [--delete | TOKEN] [ARGUMENTS]", exe.stdoutLines().get(0), "help message");

        exe = execute("config registration-token");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "help message returned");
        Assertions.assertEquals("Usage: " + CMD + " config registration-token --server SERVER --realm REALM --client CLIENT [--delete | TOKEN] [ARGUMENTS]", exe.stdoutLines().get(0), "help message");

        exe = execute("config truststore");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "help message returned");
        Assertions.assertEquals("Usage: " + CMD + " config truststore [TRUSTSTORE | --delete] [--trustpass PASSWORD] [ARGUMENTS]", exe.stdoutLines().get(0), "help message");

        exe = execute("create");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "help message returned");
        Assertions.assertEquals("Usage: " + CMD + " create [ARGUMENTS]", exe.stdoutLines().get(0), "help message");
        //Assert.assertEquals("error message", "No file nor attribute values specified", exe.stderrLines().get(0));

        exe = execute("get");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "help message returned");
        Assertions.assertEquals("Usage: " + CMD + " get CLIENT [ARGUMENTS]", exe.stdoutLines().get(0), "help message");
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        exe = execute("update");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "help message returned");
        Assertions.assertEquals("Usage: " + CMD + " update CLIENT [ARGUMENTS]", exe.stdoutLines().get(0), "help message");
        //Assert.assertEquals("error message", "No file nor attribute values specified", exe.stderrLines().get(0));

        exe = execute("delete");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "help message returned");
        Assertions.assertEquals("Usage: " + CMD + " delete CLIENT [ARGUMENTS]", exe.stdoutLines().get(0), "help message");
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        exe = execute("attrs");
        Assertions.assertEquals(0, exe.exitCode(), "exit code");
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "stdout has response");
        Assertions.assertEquals("Attributes for default format:", exe.stdoutLines().get(0), "first line");

        exe = execute("update-token");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assertions.assertTrue(exe.stdoutLines().size() > 10, "help message returned");
        Assertions.assertEquals("Usage: " + CMD + " update-token CLIENT [ARGUMENTS]", exe.stdoutLines().get(0), "help message");
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        exe = execute("help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        lines = exe.stdoutLines();
        Assertions.assertTrue(lines.size() > 0, "stdout output not empty");
        Assertions.assertEquals("Keycloak Client Registration CLI", lines.get(0), "stdout first line");
        Assertions.assertEquals("Use '" + KcRegExec.CMD + " help <command>' for more information about a given command.", lines.get(lines.size() - 2), "stdout one but last line");
        Assertions.assertEquals("", lines.get(lines.size() - 1), "stdout last line");
    }

    @Test
    public void testHelpGlobalOption() {
        /*
         *  Test --help for all commands
         */
        KcRegExec exe = execute("--help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Keycloak Client Registration CLI", exe.stdoutLines().get(0), "stdout first line");

        exe = execute("create --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " create [ARGUMENTS]", exe.stdoutLines().get(0), "stdout first line");

        exe = execute("get --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " get CLIENT [ARGUMENTS]", exe.stdoutLines().get(0), "stdout first line");

        exe = execute("update --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " update CLIENT [ARGUMENTS]", exe.stdoutLines().get(0), "stdout first line");

        exe = execute("delete --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " delete CLIENT [ARGUMENTS]", exe.stdoutLines().get(0), "stdout first line");

        exe = execute("attrs --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " attrs [ATTRIBUTE] [ARGUMENTS]", exe.stdoutLines().get(0), "stdout first line");

        exe = execute("update-token --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " update-token CLIENT [ARGUMENTS]", exe.stdoutLines().get(0), "stdout first line");

        exe = execute("config --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " config SUB_COMMAND [ARGUMENTS]", exe.stdoutLines().get(0), "stdout first line");

        exe = execute("config credentials --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " config credentials --server SERVER_URL --realm REALM [ARGUMENTS]",
                exe.stdoutLines().get(0),
                "stdout first line");

        exe = execute("config initial-token --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " config initial-token --server SERVER --realm REALM [--delete | TOKEN] [ARGUMENTS]",
                exe.stdoutLines().get(0),
                "stdout first line");

        exe = execute("config registration-token --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " config registration-token --server SERVER --realm REALM --client CLIENT [--delete | TOKEN] [ARGUMENTS]",
                exe.stdoutLines().get(0),
                "stdout first line");

        exe = execute("config truststore --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assertions.assertEquals("Usage: " + CMD + " config truststore [TRUSTSTORE | --delete] [--trustpass PASSWORD] [ARGUMENTS]",
                exe.stdoutLines().get(0),
                "stdout first line");

    }

    @Test
    public void testBadCommand() {
        /*
         *  Test most basic execution with non-existent command
         */
        KcRegExec exe = execute("nonexistent");

        assertExitCodeAndStreamSizes(exe, 2, 0, 3);
        Assertions.assertEquals("Unmatched argument at index 0: 'nonexistent'", exe.stderrLines().get(0), "stderr first line");
    }

    @Test
    public void testBadOptionInPlaceOfCommand() {
        /*
         *  Test most basic execution with non-existent option
         */
        KcRegExec exe = execute("--nonexistent");

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assertions.assertEquals("Unknown option: '--nonexistent'", exe.stderrLines().get(0), "stderr first line");
    }

    @Test
    public void testBadOption() {
        /*
         *  Test sub-command execution with non-existent option
         */

        KcRegExec exe = execute("get my_client --nonexistent");

        assertExitCodeAndStreamSizes(exe, 2, 0, 3);
        Assertions.assertEquals("Unknown option: '--nonexistent'", exe.stderrLines().get(0), "stderr first line");
        Assertions.assertEquals("Try '" + CMD + " get --help' for more information on the available options.", exe.stderrLines().get(2), "try help");
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

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assertions.assertEquals("Required option not specified: --server", exe.stderrLines().get(0), "stderr first line");
        Assertions.assertEquals("Try '" + CMD + " config credentials --help' for more information on the available options.", exe.stderrLines().get(1), "try help");
    }

    @Test
    public void testCredentialsNoRealmWithDefaultConfig() {
        /*
         *  Test without --server specified
         */
        KcRegExec exe = execute("config credentials --server " + serverUrl + " --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assertions.assertEquals("Required option not specified: --realm", exe.stderrLines().get(0), "stderr first line");
        Assertions.assertEquals("Try '" + CMD + " config credentials --help' for more information on the available options.", exe.stderrLines().get(1), "try help");
    }

    @Test
    public void testCredentialsWithNoConfig() {
        /*
         *  Test with --no-config specified which is not supported
         */
        KcRegExec exe = KcRegExec.execute("config credentials --no-config --server " + serverUrl + " --realm master --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assertions.assertEquals("Unsupported option: --no-config", exe.stderrLines().get(0), "stderr first line");
        Assertions.assertEquals("Try '" + CMD + " config credentials --help' for more information on the available options.", exe.stderrLines().get(1), "try help");
    }

    @Test
    public void testUserLoginWithDefaultConfig() {
        /*
         *  Test most basic user login, using the default admin-cli as a client
         */
        KcRegExec exe = execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 0, 0, 1);
        Assertions.assertEquals("Logging into " + serverUrl + " as user admin of realm master", exe.stderrLines().get(0), "stderr first line");
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
        Assertions.assertEquals("Logging into " + serverUrl + " as user admin of realm master", exe.stderrLines().get(0), "stderr first line");


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
            Assertions.assertTrue(exe.stdoutLines().get(0).startsWith("Enter password: "), "Enter password prompt");
            Assertions.assertEquals("Logging into " + serverUrl + " as user admin of realm master", exe.stderrLines().get(0), "stderr first line");

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
        Assertions.assertEquals("Logging into " + serverUrl + " as service-account-reg-cli-secret of realm test", exe.stderrLines().get(0), "stderr first line");

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
            Assertions.assertTrue(exe.stdoutLines().get(0).startsWith("Enter client secret: "), "Enter client secret prompt");
            Assertions.assertEquals("Logging into " + serverUrl + " as service-account-reg-cli-secret of realm test", exe.stderrLines().get(0), "stderr first line");
        } finally {
            tmpFile.delete();
        }
    }

    @Test
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
            Assertions.assertEquals("Logging into " + serverUrl + " as user admin of realm master", exe.stderrLines().get(0), "stderr first line");

            // make sure the config file exists, and has the right content
            ConfigData config = handler.loadConfig();
            Assertions.assertEquals(serverUrl, config.getServerUrl(), "serverUrl");
            Assertions.assertEquals("master", config.getRealm(), "realm");
            RealmConfigData realmcfg = config.sessionRealmConfigData();
            Assertions.assertNotNull(realmcfg, "realm config data no null");
            Assertions.assertEquals(serverUrl, realmcfg.serverUrl(), "realm cfg serverUrl");
            Assertions.assertEquals("master", realmcfg.realm(), "realm cfg realm");
            Assertions.assertEquals("admin-cli", realmcfg.getClientId(), "client id");
            Assertions.assertNotNull(realmcfg.getToken(), "token not null");
            Assertions.assertNotNull(realmcfg.getRefreshToken(), "refresh token not null");
            Assertions.assertNotNull(realmcfg.getExpiresAt(), "token expires not null");
            Assertions.assertNotNull(realmcfg.getExpiresAt() > System.currentTimeMillis(), "token expires in future");
            Assertions.assertNotNull(realmcfg.getRefreshExpiresAt(), "refresh token expires not null");
            Assertions.assertNotNull(realmcfg.getRefreshExpiresAt() > System.currentTimeMillis(), "refresh token expires in future");
            Assertions.assertTrue(realmcfg.getClients().isEmpty(), "clients is empty");

        } finally {
            configFile.delete();
        }
    }

    @Test
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
            Assertions.assertNotNull(config2.sessionRealmConfigData().getClients().get("test-client"));


            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assertions.assertEquals("test-client", client.getClientId(), "clientId");
            Assertions.assertNotNull(client.getRegistrationAccessToken(), "registrationAccessToken");
            Assertions.assertEquals(config2.sessionRealmConfigData().getClients().get("test-client"), client.getRegistrationAccessToken(), "registrationAccessToken in returned json same as in config");




            exe = execute("delete test-client --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // check changes to config file
            ConfigData config3 = handler.loadConfig();
            assertFieldsEqualWithExclusions(config2, config3, "endpoints." + serverUrl + ".master.clients.test-client");

            // check that registration access token is no longer there
            Assertions.assertTrue(config3.sessionRealmConfigData().getClients().isEmpty(), "clients empty");
        }
    }

    @Test
    public void testCRUDWithOnTheFlyUserAuth() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using username, and password.
         */
        testCRUDWithOnTheFlyAuth(serverUrl, "--user user1 --password userpass", "",
                "Logging into " + serverUrl + " as user user1 of realm test");
    }

    @Test
    public void testCRUDWithOnTheFlyUserAuthWithClientSecret() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using username, password, and client secret.
         */
        // try client without direct grants enabled
        KcRegExec exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client reg-cli-secret --secret password");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assertions.assertEquals("Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0), "login message");
        Assertions.assertEquals("Client not allowed for direct access grants [unauthorized_client]", exe.stderrLines().get(exe.stderrLines().size() - 1), "error message");


        // try wrong user password
        exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password wrong --client reg-cli-secret-direct --secret password");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assertions.assertEquals("Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0), "login message");
        Assertions.assertEquals("Invalid user credentials [invalid_grant]", exe.stderrLines().get(exe.stderrLines().size() - 1), "error message");


        // try wrong client secret
        exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client reg-cli-secret-direct --secret wrong");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assertions.assertEquals("Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0), "login message");
        Assertions.assertEquals("Invalid client or Invalid client credentials [unauthorized_client]", exe.stderrLines().get(exe.stderrLines().size() - 1), "error message");


        // try whole CRUD
        testCRUDWithOnTheFlyAuth(serverUrl, "--user user1 --password userpass --client reg-cli-secret-direct --secret password", "",
                "Logging into " + serverUrl + " as user user1 of realm test");
    }

    @Test
    public void testCRUDWithOnTheFlyUserAuthWithSignedJwtClient_JKSKeystore() throws IOException {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreUtil.KeystoreFormat.JKS);
        testCRUDWithOnTheFlyUserAuthWithSignedJwtClient(KeystoreUtil.KeystoreFormat.JKS.getPrimaryExtension());
    }

    @Test
    public void testCRUDWithOnTheFlyUserAuthWithSignedJwtClient_PKCS12Keystore() throws IOException {
        KeystoreUtils.assumeKeystoreTypeSupported(KeystoreUtil.KeystoreFormat.PKCS12);
        testCRUDWithOnTheFlyUserAuthWithSignedJwtClient(KeystoreUtil.KeystoreFormat.PKCS12.getPrimaryExtension());
    }

    private void testCRUDWithOnTheFlyUserAuthWithSignedJwtClient(String keystoreFileExtension) throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using username, password, and client JWT signature.
         */
        File keystore = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcreg/reg-cli-keystore." + keystoreFileExtension);
        Assertions.assertTrue(keystore.isFile(), "reg-cli-keystore." + keystoreFileExtension + " exists");

        // try client without direct grants enabled
        KcRegExec exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client reg-cli-jwt --keystore '" + keystore.getAbsolutePath() + "'" +
                " --storepass storepass --keypass keypass --alias reg-cli");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assertions.assertEquals("Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0), "login message");
        Assertions.assertEquals("Client not allowed for direct access grants [unauthorized_client]", exe.stderrLines().get(exe.stderrLines().size() - 1), "error message");


        // try wrong user password
        exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password wrong --client reg-cli-jwt-direct --keystore '" + keystore.getAbsolutePath() + "'" +
                " --storepass storepass --keypass keypass --alias reg-cli");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assertions.assertEquals("Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0), "login message");
        Assertions.assertEquals("Invalid user credentials [invalid_grant]", exe.stderrLines().get(exe.stderrLines().size() - 1), "error message");


        // try wrong storepass
        exe = execute("get test-client --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client reg-cli-jwt-direct --keystore '" + keystore.getAbsolutePath() + "'" +
                " --storepass wrong --keypass keypass --alias reg-cli");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assertions.assertEquals("Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0), "login message");
        Assertions.assertTrue(exe.stderrLines().get(exe.stderrLines().size() - 1).startsWith("Failed to load private key: "), "error message");


        // try whole CRUD
        testCRUDWithOnTheFlyAuth(serverUrl,
                "--user user1 --password userpass  --client reg-cli-jwt-direct --keystore '" + keystore.getAbsolutePath() + "'" +
                        " --storepass storepass --keypass keypass --alias reg-cli", "",
                "Logging into " + serverUrl + " as user user1 of realm test");

    }

    @Test
    public void testCRUDWithOnTheFlyServiceAccountWithClientSecret() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using only client secret - service account is used.
         */
        testCRUDWithOnTheFlyAuth(serverUrl, "--client reg-cli-secret --secret password", "",
                "Logging into " + serverUrl + " as service-account-reg-cli-secret of realm test");
    }

    @Test
    public void testCRUDWithOnTheFlyServiceAccountWithSignedJwtClient() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using only client JWT signature - service account is used.
         */
        File keystore = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcreg/reg-cli-keystore.p12");
        Assertions.assertTrue(keystore.isFile(), "reg-cli-keystore.p12 exists");

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

            Assertions.assertEquals(0, exe.exitCode(), "exitCode == 0");


            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);

            Assertions.assertEquals("test-client2", client.getClientId(), "clientId");
            Assertions.assertNotNull(client.getRegistrationAccessToken(), "registrationAccessToken");


            if (useConfig) {
                ConfigData config = handler.loadConfig();
                Assertions.assertEquals(client.getRegistrationAccessToken(),
                        config.ensureRealmConfigData(oauth.AUTH_SERVER_ROOT, realm).getClients().get("test-client2"),
                        "Registration Access Token in config file");
            } else {
                Assertions.assertFalse(configFile.isFile(), "There should be no config file");
            }

            exe = execute("delete test-client2 " + (useConfig ? ("--config '" + configFile.getAbsolutePath()) + "'" : "--no-config")
                    + " --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm " + realm + " -t " + client.getRegistrationAccessToken());

            assertExitCodeAndStreamSizes(exe, 0, 0, 2);
        }
    }

    @Test
    public void testCreateWithAllowedHostsWithoutAuthenticationNoConfig() throws IOException {

        testCreateWithAllowedHostsWithoutAuthentication("test", false);
    }

    @Test
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

            Assertions.assertEquals("test-client", client.getClientId(), "clientId");
            Assertions.assertNotNull(client.getRegistrationAccessToken(), "registrationAccessToken");

            exe = execute("delete test-client " + (useConfig ? ("--config '" + configFile.getAbsolutePath()) + "'" : "--no-config")
                    + " --server " + serverUrl + " --realm " + realm + " -t " + client.getRegistrationAccessToken());

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);
        }
    }

}

package org.keycloak.testsuite.cli.registration;

import org.junit.Assert;
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

import static org.keycloak.client.registration.cli.util.OsUtil.EOL;
import static org.keycloak.testsuite.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegTest extends AbstractCliTest {

    @Test
    public void testNoArgs() {
        /*
         *  Test most basic execution that returns the initial help
         */
        KcRegExec exe = execute("");

        Assert.assertEquals("exitCode == 0", 0, exe.exitCode());

        List<String> lines = exe.stdoutLines();
        Assert.assertTrue("stdout output not empty", lines.size() > 0);
        Assert.assertEquals("stdout first line", "Keycloak Client Registration CLI", lines.get(0));
        Assert.assertEquals("stdout one but last line", "Use '" + KcRegExec.CMD + " help <command>' for more information about a given command.", lines.get(lines.size() - 2));
        Assert.assertEquals("stdout last line", "", lines.get(lines.size() - 1));

        lines = exe.stderrLines();
        Assert.assertTrue("stderr output empty", lines.size() == 0);
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

        assertExitCodeAndStreamSizes(exe, 1, 0, 1);
        Assert.assertEquals("stderr first line", "Invalid option: --nonexistent", exe.stderrLines().get(0));
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

        assertExitCodeAndStreamSizes(exe, 1, 0, 1);
        Assert.assertEquals("stderr first line", "Required option not specified: --server", exe.stderrLines().get(0));
    }

    @Test
    public void testCredentialsNoRealmWithDefaultConfig() {
        /*
         *  Test without --server specified
         */
        KcRegExec exe = execute("config credentials --server " + serverUrl + " --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 1, 0, 1);
        Assert.assertEquals("stderr first line", "Required option not specified: --realm", exe.stderrLines().get(0));
    }

    @Test
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
    public void testCustomConfigLoginCreateDelete() throws IOException {
        /*
         *  Test user login, create, delete session using a custom config file
         */

        // prepare for loading a config file
        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            KcRegExec exe = execute("config credentials --server " + serverUrl +
                    " --realm master --user admin --password admin --config '" + configFile.getName() + "'");

            Assert.assertEquals("exitCode == 0", 0, exe.exitCode());

            // remember the state of config file
            ConfigData config1 = handler.loadConfig();




            exe = execute("create --config '" + configFile.getName() + "' -s clientId=test-client -o");

            Assert.assertEquals("exitCode == 0", 0, exe.exitCode());

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
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Client not allowed for direct access grants [invalid_grant]", exe.stderrLines().get(1));


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
        Assert.assertEquals("error message", "Client not allowed for direct access grants [invalid_grant]", exe.stderrLines().get(1));


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
        File keystore = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcreg/reg-cli-keystore.jks");
        Assert.assertTrue("reg-cli-keystore.jks exists", keystore.isFile());

        testCRUDWithOnTheFlyAuth(serverUrl,
                "--client reg-cli-jwt --keystore '" + keystore.getAbsolutePath() + "' --storepass storepass --keypass keypass --alias reg-cli", "",
                "Logging into " + serverUrl + " as service-account-reg-cli-jwt of realm test");
    }

    @Test
    public void testCreateDeleteWithInitialAndRegistrationTokens() throws IOException {
        /*
         *  Test create using initial client token, and subsequent delete using registration access token.
         *  A config file is used to save registration access token for newly created client.
         */
        testCreateDeleteWithInitialAndRegistrationTokens(true);
    }

    @Test
    public void testCreateDeleteWithInitialAndRegistrationTokensNoConfig() throws IOException {
        /*
         *  Test create using initial client token, and subsequent delete using registration access token.
         *  No config file is used so registration access token for newly created client is not saved to config.
         */
        testCreateDeleteWithInitialAndRegistrationTokens(false);
    }

    private void testCreateDeleteWithInitialAndRegistrationTokens(boolean useConfig) throws IOException {

        // prepare for loading a config file
        // only used when useConfig is true
        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            String token = issueInitialAccessToken("master");

            final String realm = "master";

            KcRegExec exe = execute("create " + (useConfig ? ("--config '" + configFile.getAbsolutePath()) + "'" : "--no-config")
                    + " --server " + serverUrl + " --realm " + realm + " -s clientId=test-client2 -o -t " + token);

            Assert.assertEquals("exitCode == 0", 0, exe.exitCode());


            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);

            Assert.assertEquals("clientId", "test-client2", client.getClientId());
            Assert.assertNotNull("registrationAccessToken", client.getRegistrationAccessToken());


            if (useConfig) {
                ConfigData config = handler.loadConfig();
                Assert.assertEquals("Registration Access Token in config file", client.getRegistrationAccessToken(),
                        config.ensureRealmConfigData(serverUrl, realm).getClients().get("test-client2"));
            } else {
                Assert.assertFalse("There should be no config file", configFile.isFile());
            }

            exe = execute("delete test-client2 " + (useConfig ? ("--config '" + configFile.getAbsolutePath()) + "'" : "--no-config")
                    + " --server " + serverUrl + " --realm " + realm + " -t " + client.getRegistrationAccessToken());

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);
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

            Assert.assertEquals("exitCode == 0", 0, exe.exitCode());

            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);

            Assert.assertEquals("clientId", "test-client", client.getClientId());
            Assert.assertNotNull("registrationAccessToken", client.getRegistrationAccessToken());
        }
    }

}
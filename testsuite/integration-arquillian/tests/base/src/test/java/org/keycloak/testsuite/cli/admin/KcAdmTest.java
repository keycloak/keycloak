package org.keycloak.testsuite.cli.admin;

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
import org.keycloak.testsuite.cli.KcAdmExec;
import org.keycloak.testsuite.util.KeystoreUtils;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.client.admin.cli.KcAdmMain.CMD;
import static org.keycloak.client.cli.util.OsUtil.EOL;
import static org.keycloak.testsuite.cli.KcAdmExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmTest extends AbstractAdmCliTest {

    @Test
    public void testBadCommand() {
        /*
         *  Test most basic execution with non-existent command
         */
        KcAdmExec exe = execute("nonexistent");

        assertExitCodeAndStreamSizes(exe, 2, 0, 3);
        Assert.assertEquals("stderr first line", "Unmatched argument at index 0: 'nonexistent'", exe.stderrLines().get(0));
    }


    @Test
    public void testNoArgs() {
        /*
         *  Test (sub)commands without any arguments
         */
        KcAdmExec exe = KcAdmExec.execute("");

        assertExitCodeAndStdErrSize(exe, 2, 0);

        List<String> lines = exe.stdoutLines();
        Assert.assertTrue("stdout output not empty", lines.size() > 0);
        Assert.assertEquals("stdout first line", "Keycloak Admin CLI", lines.get(0));
        Assert.assertEquals("stdout one but last line", "Use '" + KcAdmExec.CMD + " help <command>' for more information about a given command.", lines.get(lines.size() - 2));
        Assert.assertEquals("stdout last line", "", lines.get(lines.size() - 1));


        /*
         * Test commands without arguments
         */
        exe = KcAdmExec.execute("config");
        assertExitCodeAndStreamSizes(exe, 2, 8, 0);
        Assert.assertEquals("error message",
                "Usage: kcadm.sh config SUB_COMMAND [ARGUMENTS]",
                exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("config credentials");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "       " + CMD + " config credentials --server SERVER_URL --realm REALM --user USER [--password PASSWORD] [ARGUMENTS]", exe.stdoutLines().get(1));

        exe = KcAdmExec.execute("config truststore");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " config truststore [TRUSTSTORE | --delete] [--trustpass PASSWORD] [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("create");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " create ENDPOINT_URI [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "No file nor attribute values specified", exe.stderrLines().get(0));

        exe = KcAdmExec.execute("get");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " get ENDPOINT_URI [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        exe = KcAdmExec.execute("update");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " update ENDPOINT_URI [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "No file nor attribute values specified", exe.stderrLines().get(0));

        exe = KcAdmExec.execute("delete");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " delete ENDPOINT_URI [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        //exe = KcAdmExec.execute("get-roles");
        //assertExitCodeAndStdErrSize(exe, 0, 0);
        //try {
        //    JsonNode node = JsonSerialization.readValue(exe.stdout(), JsonNode.class);
        //    Assert.assertTrue("is JSON array", node.isArray());
        //} catch (IOException e) {
        //    throw new AssertionError("Response should be a JSON array", e);
        //}

        //Assert.assertTrue("JSON message returned", exe.stdoutLines().size() > 10);
        //Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        //Assert.assertEquals("help message", "Usage: " + CMD + " get-roles [--cclientid CLIENT_ID | --cid ID] [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("add-roles");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " add-roles (--uusername USERNAME | --uid ID) [--cclientid CLIENT_ID | --cid ID] (--rolename NAME | --roleid ID)+ [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        exe = KcAdmExec.execute("remove-roles");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " remove-roles (--uusername USERNAME | --uid ID) [--cclientid CLIENT_ID | --cid ID] (--rolename NAME | --roleid ID)+ [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("set-password");
        assertExitCodeAndStdErrSize(exe, 2, 0);
        Assert.assertTrue("help message returned", exe.stdoutLines().size() > 10);
        Assert.assertEquals("help message", "Usage: " + CMD + " set-password (--username USERNAME | --userid ID) [--new-password PASSWORD] [ARGUMENTS]", exe.stdoutLines().get(0));
        //Assert.assertEquals("error message", "CLIENT not specified", exe.stderrLines().get(0));

        exe = KcAdmExec.execute("help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        lines = exe.stdoutLines();
        Assert.assertTrue("stdout output not empty", lines.size() > 0);
        Assert.assertEquals("stdout first line", "Keycloak Admin CLI", lines.get(0));
        Assert.assertEquals("stdout one but last line", "Use '" + KcAdmExec.CMD + " help <command>' for more information about a given command.", lines.get(lines.size() - 2));
        Assert.assertEquals("stdout last line", "", lines.get(lines.size() - 1));
    }

    @Test
    public void testHelpGlobalOption() {
        /*
         *  Test --help for all commands
         */
        KcAdmExec exe = KcAdmExec.execute("--help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Keycloak Admin CLI", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("create --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " create ENDPOINT_URI [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("get --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " get ENDPOINT_URI [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("update --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " update ENDPOINT_URI [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("delete --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " delete ENDPOINT_URI [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("get-roles --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " get-roles [--cclientid CLIENT_ID | --cid ID] [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("add-roles --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " add-roles (--uusername USERNAME | --uid ID) [--cclientid CLIENT_ID | --cid ID] (--rolename NAME | --roleid ID)+ [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("remove-roles --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " remove-roles (--uusername USERNAME | --uid ID) [--cclientid CLIENT_ID | --cid ID] (--rolename NAME | --roleid ID)+ [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("set-password --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " set-password (--username USERNAME | --userid ID) [--new-password PASSWORD] [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("config --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line", "Usage: " + CMD + " config SUB_COMMAND [ARGUMENTS]", exe.stdoutLines().get(0));

        exe = KcAdmExec.execute("config credentials --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout line",
                "       " + CMD + " config credentials --server SERVER_URL --realm REALM --user USER [--password PASSWORD] [ARGUMENTS]",
                exe.stdoutLines().get(1));

        exe = KcAdmExec.execute("config truststore --help");
        assertExitCodeAndStdErrSize(exe, 0, 0);
        Assert.assertEquals("stdout first line",
                "Usage: " + CMD + " config truststore [TRUSTSTORE | --delete] [--trustpass PASSWORD] [ARGUMENTS]",
                exe.stdoutLines().get(0));
    }

    @Test
    public void testBadOptionInPlaceOfCommand() {
        /*
         *  Test most basic execution with non-existent option
         */
        KcAdmExec exe = KcAdmExec.execute("--nonexistent");

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assert.assertEquals("stderr first line", "Unknown option: '--nonexistent'", exe.stderrLines().get(0));
    }

    @Test
    public void testBadOption() {
        /*
         *  Test sub-command execution with non-existent option
         */

        KcAdmExec exe = KcAdmExec.execute("get users --nonexistent");

        assertExitCodeAndStreamSizes(exe, 2, 0, 3);
        Assert.assertEquals("stderr first line", "Unknown option: '--nonexistent'", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " get --help' for more information on the available options.", exe.stderrLines().get(2));

        exe = KcAdmExec.execute("set-password --nonexistent");

        assertExitCodeAndStreamSizes(exe, 2, 0, 3);
        Assert.assertEquals("stderr first line", "Unknown option: '--nonexistent'", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " set-password --help' for more information on the available options.", exe.stderrLines().get(2));
    }

    @Test
    public void testBadOverlappingOption() {
        KcAdmExec exe = KcAdmExec.execute("config credentials --server http://localhost:8080 --realm master --username admin --password admin");

        assertExitCodeAndStreamSizes(exe, 2, 0, 3);
        Assert.assertEquals("stderr first line", "Unknown options: '--username', 'admin'", exe.stderrLines().get(0));
    }

    @Test
    public void testCredentialsServerAndRealmWithDefaultConfig() {
        /*
         *  Test without --server specified
         */
        KcAdmExec exe = KcAdmExec.execute("config credentials --server " + serverUrl + " --realm master");

        assertExitCodeAndStreamSizes(exe, 0, 0, 0);
    }

    @Test
    public void testCredentialsNoServerWithDefaultConfig() {
        /*
         *  Test without --server specified
         */
        KcAdmExec exe = KcAdmExec.execute("config credentials --realm master --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assert.assertEquals("stderr first line", "Required option not specified: --server", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " config credentials --help' for more information on the available options.", exe.stderrLines().get(1));
    }

    @Test
    public void testCredentialsNoRealmWithDefaultConfig() {
        /*
         *  Test without --server specified
         */
        KcAdmExec exe = KcAdmExec.execute("config credentials --server " + serverUrl + " --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assert.assertEquals("stderr first line", "Required option not specified: --realm", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " config credentials --help' for more information on the available options.", exe.stderrLines().get(1));
    }

    @Test
    public void testCredentialsWithNoConfig() {
        /*
         *  Test with --no-config specified which is not supported
         */
        KcAdmExec exe = KcAdmExec.execute("config credentials --no-config --server " + serverUrl + " --realm master --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 2, 0, 2);
        Assert.assertEquals("stderr first line", "Unsupported option: --no-config", exe.stderrLines().get(0));
        Assert.assertEquals("try help", "Try '" + CMD + " config credentials --help' for more information on the available options.", exe.stderrLines().get(1));
    }

    @Test
    public void testUserLoginWithDefaultConfig() {
        /*
         *  Test most basic user login, using the default admin-cli as a client
         */
        KcAdmExec exe = KcAdmExec.execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");

        assertExitCodeAndStreamSizes(exe, 0, 0, 1);
        Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as user admin of realm master", exe.stderrLines().get(0));
    }

    @Test
    public void testUserLoginWithAngleBrackets() {
        KcAdmExec exe = KcAdmExec.execute("config credentials --server " + serverUrl + " --realm test --user 'special>>character' --password '<password>'");

        assertExitCodeAndStreamSizes(exe, 0, 0, 1);
        Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as user special>>character of realm test", exe.stderrLines().get(0));
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

        KcAdmExec exe = KcAdmExec.newBuilder()
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
        File tmpFile = new File(KcAdmExec.WORK_DIR + "/" + UUID.randomUUID().toString() + ".tmp");
        try {
            FileOutputStream tmpos = new FileOutputStream(tmpFile);
            tmpos.write("admin".getBytes());
            tmpos.write(EOL.getBytes());
            tmpos.close();

            exe = KcAdmExec.execute("config credentials --server " + serverUrl + " --realm master --user admin < '" + tmpFile.getName() + "'");

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
        KcAdmExec exe = KcAdmExec.newBuilder()
                .argsLine("config credentials --server " + serverUrl + " --realm test --client admin-cli-secret")
                .executeAsync();

        exe.waitForStdout("Enter client secret: ");
        exe.sendToStdin("password" + EOL);
        exe.waitCompletion();

        assertExitCodeAndStreamSizes(exe, 0, 1, 1);
        Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as service-account-admin-cli-secret of realm test", exe.stderrLines().get(0));

        /*
         *  Run the test one more time with stdin redirect
         */
        File tmpFile = new File(KcAdmExec.WORK_DIR + "/" + UUID.randomUUID().toString() + ".tmp");
        try {
            FileOutputStream tmpos = new FileOutputStream(tmpFile);
            tmpos.write("password".getBytes());
            tmpos.write(EOL.getBytes());
            tmpos.close();

            exe = KcAdmExec.newBuilder()
                    .argsLine("config credentials --server " + serverUrl + " --realm test --client admin-cli-secret < '" + tmpFile.getName() + "'")
                    .execute();

            assertExitCodeAndStreamSizes(exe, 0, 1, 1);
            Assert.assertTrue("Enter client secret prompt", exe.stdoutLines().get(0).startsWith("Enter client secret: "));
            Assert.assertEquals("stderr first line", "Logging into " + serverUrl + " as service-account-admin-cli-secret of realm test", exe.stderrLines().get(0));
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

        File configFile = new File(FileConfigHandler.getConfigFile());
        try {
            KcAdmExec exe = KcAdmExec.execute("config credentials --server " + serverUrl + " --realm master" +
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

        try (TempFileResource configFile = new TempFileResource(FileConfigHandler.getConfigFile())) {

            KcAdmExec exe = KcAdmExec.execute("config credentials --server " + serverUrl +
                    " --realm master --user admin --password admin --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 0, 0, 1);

            // remember the state of config file
            ConfigData config1 = handler.loadConfig();




            exe = KcAdmExec.execute("create --config '" + configFile.getName() + "' clients -s clientId=test-client -o");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            // check changes to config file
            ConfigData config2 = handler.loadConfig();
            assertFieldsEqualWithExclusions(config1, config2);


            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assert.assertEquals("clientId", "test-client", client.getClientId());



            exe = KcAdmExec.execute("delete clients/" + client.getId() + " --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // check changes to config file
            ConfigData config3 = handler.loadConfig();
            assertFieldsEqualWithExclusions(config2, config3);
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
        KcAdmExec exe = KcAdmExec.execute("get clients --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client admin-cli-secret --secret password");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Client not allowed for direct access grants [unauthorized_client]", exe.stderrLines().get(exe.stderrLines().size() - 1));


        // try wrong user password
        exe = KcAdmExec.execute("get clients --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password wrong --client admin-cli-secret-direct --secret password");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Invalid user credentials [invalid_grant]", exe.stderrLines().get(exe.stderrLines().size() - 1));


        // try wrong client secret
        exe = KcAdmExec.execute("get clients --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client admin-cli-secret-direct --secret wrong");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Invalid client or Invalid client credentials [unauthorized_client]", exe.stderrLines().get(exe.stderrLines().size() - 1));


        // try whole CRUD
        testCRUDWithOnTheFlyAuth(serverUrl, "--user user1 --password userpass --client admin-cli-secret-direct --secret password", "",
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
        File keystore = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcadm/admin-cli-keystore." + keystoreFileExtension);
        Assert.assertTrue("admin-cli-keystore." + keystoreFileExtension + " must exist, but it does not exists", keystore.isFile());

        // try client without direct grants enabled
        KcAdmExec exe = KcAdmExec.execute("get clients --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client admin-cli-jwt --keystore '" + keystore.getAbsolutePath() + "'" +
                " --storepass storepass --keypass keypass --alias admin-cli");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Client not allowed for direct access grants [unauthorized_client]", exe.stderrLines().get(exe.stderrLines().size() - 1));


        // try wrong user password
        exe = KcAdmExec.execute("get clients --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password wrong --client admin-cli-jwt-direct --keystore '" + keystore.getAbsolutePath() + "'" +
                " --storepass storepass --keypass keypass --alias admin-cli");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertEquals("error message", "Invalid user credentials [invalid_grant]", exe.stderrLines().get(exe.stderrLines().size() - 1));


        // try wrong storepass
        exe = KcAdmExec.execute("get clients --no-config --server " + serverUrl + " --realm test" +
                " --user user1 --password userpass --client admin-cli-jwt-direct --keystore '" + keystore.getAbsolutePath() + "'" +
                " --storepass wrong --keypass keypass --alias admin-cli");

        assertExitCodeAndStreamSizes(exe, 1, 0, 2);
        Assert.assertEquals("login message", "Logging into " + serverUrl + " as user user1 of realm test", exe.stderrLines().get(0));
        Assert.assertTrue("error message", exe.stderrLines().get(exe.stderrLines().size() - 1).startsWith("Failed to load private key:"));


        // try whole CRUD
        testCRUDWithOnTheFlyAuth(serverUrl,
                "--user user1 --password userpass  --client admin-cli-jwt-direct --keystore '" + keystore.getAbsolutePath() + "'" +
                        " --storepass storepass --keypass keypass --alias admin-cli", "",
                "Logging into " + serverUrl + " as user user1 of realm test");

    }

    @Test
    public void testCRUDWithOnTheFlyServiceAccountWithClientSecret() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using only client secret - service account is used.
         */
        testCRUDWithOnTheFlyAuth(serverUrl, "--client admin-cli-secret --secret password", "",
                "Logging into " + serverUrl + " as service-account-admin-cli-secret of realm test");
    }

    @Test
    public void testCRUDWithOnTheFlyServiceAccountWithSignedJwtClient() throws IOException {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using only client JWT signature - service account is used.
         */
        File keystore = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcadm/admin-cli-keystore.p12");
        Assert.assertTrue("admin-cli-keystore.p12 exists", keystore.isFile());

        testCRUDWithOnTheFlyAuth(serverUrl,
                "--client admin-cli-jwt --keystore '" + keystore.getAbsolutePath() + "' --storepass storepass --keypass keypass --alias admin-cli", "",
                "Logging into " + serverUrl + " as service-account-admin-cli-jwt of realm test");
    }

    @Test
    public void testCRUDWithToken() throws Exception {
        /*
         *  Test create, get, update, and delete using on-the-fly authentication - without using any config file.
         *  Login is performed by each operation again, and again using username, password, and client secret.
         */
        //non-TLS endpoint
        oauth.baseUrl(serverUrl);
        oauth.realm("master");
        oauth.client("admin-cli");
        String token = oauth.doPasswordGrantRequest("admin", "admin").getAccessToken();
        testCRUDWithOnTheFlyAuth(serverUrl, " --token " + token, "",
                "");

    }

    @Test
    public void testGetUserNameExact() {
        KcAdmExec.execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");
        KcAdmExec.execute("create realms -s realm=demorealm -s enabled=true");
        KcAdmExec.execute("create users -r demorealm -s username=testuser");
        KcAdmExec.execute("create users -r demorealm -s username=anothertestuser");
        KcAdmExec.execute("create users -r demorealm -s username=onemoretestuser");
        KcAdmExec exec = execute("add-roles --uusername=testuser --rolename offline_access --target-realm=demorealm");
        Assert.assertEquals(0, exec.exitCode());
        exec = execute("add-roles --uusername=anothertestuser --rolename offline_access --target-realm=demorealm");
        Assert.assertEquals(0, exec.exitCode());
        exec = execute("add-roles --uusername=onemoretestuser --rolename offline_access --target-realm=demorealm");
        Assert.assertEquals(0, exec.exitCode());
    }

    @Test
    public void testGetGroupNameExact() {
        KcAdmExec.execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");
        KcAdmExec.execute("create realms -s realm=demorealm -s enabled=true");
        KcAdmExec.execute("create role -r demorealm -s name=testrole");
        KcAdmExec.execute("create groups -r demorealm -s name=test");
        KcAdmExec.execute("create groups -r demorealm -s name=anothertest");
        KcAdmExec.execute("create groups -r demorealm -s name=onemoretest");
        KcAdmExec exec = execute("get-roles -r demorealm --gname test");
        Assert.assertEquals(exec.stderrString(), 0, exec.exitCode());
        exec = execute("get-roles -r demorealm --gname anothertest");
        Assert.assertEquals(exec.stderrString(), 0, exec.exitCode());
        exec = execute("get-roles -r demorealm --gname onemoretest");
        Assert.assertEquals(exec.stderrString(), 0, exec.exitCode());
        exec = execute("get-roles -r demorealm --gname not_there");
        Assert.assertEquals(1, exec.exitCode());
    }

    @Test
    public void testCsvFormat() {
        execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");
        KcAdmExec exec = execute("get realms/master --format csv");
        assertExitCodeAndStreamSizes(exec, 0, 1, 0);
        Assert.assertTrue(exec.stdoutString().startsWith("\""));
    }

    @Test
    public void testCsvFormatWithMissingFields() {
        execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");
        KcAdmExec exec = execute("get realms/master --format csv --fields foo");
        // nothing valid was selected, should be blank
        assertExitCodeAndStreamSizes(exec, 0, 1, 0);
        Assert.assertTrue(exec.stdoutString().isBlank());
    }

    @Test
    public void testCompressedCsv() {
        execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");
        KcAdmExec exec = execute("get realms/master --format csv --compressed");
        // should contain an error message
        assertExitCodeAndStreamSizes(exec, 0, 0, 1);
    }

    @Test
    public void testEnvPasswordWithRegularCommand() {
        execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");
        KcAdmExec exec = KcAdmExec.newBuilder()
                .argsLine("get users --format csv")
                .env("KC_CLI_PASSWORD=ignoreme")
                .execute();
        // should not contain an error message
        assertExitCodeAndStreamSizes(exec, 0, 1, 0);
    }

    @Test
    public void testStatusOptionForConfigCred() {
        execute("config credentials --server " + serverUrl + " --realm master --user admin --password admin");
        KcAdmExec exec = execute("config credentials --status");
        assertExitCodeAndStreamSizes(exec, 0, 1, 0);
        Assert.assertTrue(exec.stdoutString().startsWith("Logged in (server: " + serverUrl + ", realm: master, expired: false, timeToExpiry:"));
        Assert.assertTrue(exec.stdoutString().endsWith("from now)\n"));
        exec = execute("config credentials --status --server " + serverUrl + " --realm master --user admin --password admin");
        assertExitCodeAndStreamSizes(exec, 0, 1, 0);
        Assert.assertTrue(exec.stdoutString().startsWith("Logged in (server: " + serverUrl + ", realm: master, expired: false, timeToExpiry:"));
        Assert.assertTrue(exec.stdoutString().endsWith("from now)\n"));
    }

}

package org.keycloak.testsuite.exportimport;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.dir.DirExportProvider;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.exportimport.zip.ZipExportProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.KeycloakRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportTest {


    // We want data to be persisted among server restarts
    private static ExternalResource hibernateSetupRule = new ExternalResource() {

        private boolean setupDone = false;

        @Override
        protected void before() throws Throwable {
            if (System.getProperty("hibernate.connection.url") == null) {
                String baseExportImportDir = getExportImportTestDirectory();

                File oldDBFile = new File(baseExportImportDir, "keycloakDB.h2.db");
                if (oldDBFile.exists()) {
                    oldDBFile.delete();
                }

                String dbDir = baseExportImportDir + "/keycloakDB";
                System.setProperty("hibernate.connection.url", "jdbc:h2:file:" + dbDir + ";DB_CLOSE_DELAY=-1");
                System.setProperty("hibernate.hbm2ddl.auto", "update");
                setupDone = true;
            }
        }

        @Override
        protected void after() {
            if (setupDone) {
                Properties sysProps = System.getProperties();
                sysProps.remove("hibernate.connection.url");
                sysProps.remove("hibernate.hbm2ddl.auto");
            }
        }
    };

    // We want data to be persisted among server restarts
    private static ExternalResource mongoRule = new ExternalResource() {

        private static final String MONGO_CLEAR_ON_STARTUP_PROP_NAME = "keycloak.connectionsMongo.clearOnStartup";
        private String previousMongoClearOnStartup;

        @Override
        protected void before() throws Throwable {
            previousMongoClearOnStartup = System.getProperty(MONGO_CLEAR_ON_STARTUP_PROP_NAME);
            System.setProperty(MONGO_CLEAR_ON_STARTUP_PROP_NAME, "false");
        }

        @Override
        protected void after() {
            if (previousMongoClearOnStartup != null) {
                System.setProperty(MONGO_CLEAR_ON_STARTUP_PROP_NAME, "false");
            } else {
                System.getProperties().remove(MONGO_CLEAR_ON_STARTUP_PROP_NAME);
            }
        }

    };

    private static KeycloakRule keycloakRule = new KeycloakRule( new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            addUser(manager.getSession().users(), appRealm, "user1", "password");
            addUser(manager.getSession().users(), appRealm, "user2", "password");
            addUser(manager.getSession().users(), appRealm, "user3", "password");
            addUser(manager.getSession().users(), adminstrationRealm, "admin2", "admin2");
        }



    }) {
        @Override
        protected void after() {
            super.after();

            // Clear export/import properties after test
            Properties systemProps = System.getProperties();
            Set<String> propsToRemove = new HashSet<String>();

            for (Object key : systemProps.keySet()) {
                if (key.toString().startsWith(ExportImportConfig.PREFIX)) {
                    propsToRemove.add(key.toString());
                }
            }

            for (String propToRemove : propsToRemove) {
                systemProps.remove(propToRemove);
            }
        }
    };

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(hibernateSetupRule)
            .around(mongoRule)
            .around(keycloakRule);

    //@Test
    public void testDirFullExportImport() throws Throwable {
        ExportImportConfig.setProvider(DirExportProviderFactory.PROVIDER_ID);
        String targetDirPath = getExportImportTestDirectory() + File.separator + "dirExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        ExportImportConfig.setDir(targetDirPath);
        ExportImportConfig.setUsersPerFile(ExportImportConfig.DEFAULT_USERS_PER_FILE);

        testFullExportImport();

        // There should be 4 files in target directory (2 realm, 2 user)
        Assert.assertEquals(4, new File(targetDirPath).listFiles().length);
    }

    @Test
    public void testDirRealmExportImport() throws Throwable {
        ExportImportConfig.setProvider(DirExportProviderFactory.PROVIDER_ID);
        String targetDirPath = getExportImportTestDirectory() + File.separator + "dirRealmExport";
        DirExportProvider.recursiveDeleteDir(new File(targetDirPath));
        ExportImportConfig.setDir(targetDirPath);
        ExportImportConfig.setUsersPerFile(3);

        testRealmExportImport();

        // There should be 3 files in target directory (1 realm, 2 user)
        Assert.assertEquals(3, new File(targetDirPath).listFiles().length);
    }

    @Test
    public void testSingleFileFullExportImport() throws Throwable {
        ExportImportConfig.setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = getExportImportTestDirectory() + File.separator + "singleFile-full.json";
        ExportImportConfig.setFile(targetFilePath);

        testFullExportImport();
    }

    @Test
    public void testSingleFileRealmExportImport() throws Throwable {
        ExportImportConfig.setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = getExportImportTestDirectory() + File.separator + "singleFile-realm.json";
        ExportImportConfig.setFile(targetFilePath);

        testRealmExportImport();
    }

    @Test
    public void testZipFullExportImport() throws Throwable {
        ExportImportConfig.setProvider(ZipExportProviderFactory.PROVIDER_ID);
        String zipFilePath = getExportImportTestDirectory() + File.separator + "export-full.zip";
        new File(zipFilePath).delete();
        ExportImportConfig.setZipFile(zipFilePath);
        ExportImportConfig.setZipPassword("encPassword");
        ExportImportConfig.setUsersPerFile(ExportImportConfig.DEFAULT_USERS_PER_FILE);

        testFullExportImport();
    }

    @Test
    public void testZipRealmExportImport() throws Throwable {
        ExportImportConfig.setProvider(ZipExportProviderFactory.PROVIDER_ID);
        String zipFilePath = getExportImportTestDirectory() + File.separator + "export-realm.zip";
        new File(zipFilePath).delete();
        ExportImportConfig.setZipFile(zipFilePath);
        ExportImportConfig.setZipPassword("encPassword");
        ExportImportConfig.setUsersPerFile(3);

        testRealmExportImport();
    }

    private void testFullExportImport() {
        ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT);
        ExportImportConfig.setRealmName(null);

        // Restart server, which triggers export
        keycloakRule.restartServer();

        // Delete some realm (and some data in admin realm)
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmProvider realmProvider = session.realms();
            UserProvider userProvider = session.users();
            new RealmManager(session).removeRealm(realmProvider.getRealmByName("test"));
            Assert.assertEquals(1, realmProvider.getRealms().size());

            RealmModel master = realmProvider.getRealmByName(Config.getAdminRealm());
            session.users().removeUser(master, "admin2");
            assertNotAuthenticated(userProvider, realmProvider, Config.getAdminRealm(), "admin2", "admin2");
            assertNotAuthenticated(userProvider, realmProvider, "test", "test-user@localhost", "password");
            assertNotAuthenticated(userProvider, realmProvider, "test", "user1", "password");
            assertNotAuthenticated(userProvider, realmProvider, "test", "user2", "password");
            assertNotAuthenticated(userProvider, realmProvider, "test", "user3", "password");
        } finally {
            keycloakRule.stopSession(session, true);
        }

        // Configure import
        ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);

        // Restart server, which triggers import
        keycloakRule.restartServer();

        // Ensure data are imported back
        session = keycloakRule.startSession();
        try {
            RealmProvider model = session.realms();
            UserProvider userProvider = session.users();
            Assert.assertEquals(2, model.getRealms().size());

            assertAuthenticated(userProvider, model, Config.getAdminRealm(), "admin2", "admin2");
            assertAuthenticated(userProvider, model, "test", "test-user@localhost", "password");
            assertAuthenticated(userProvider, model, "test", "user1", "password");
            assertAuthenticated(userProvider, model, "test", "user2", "password");
            assertAuthenticated(userProvider, model, "test", "user3", "password");

        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    private void testRealmExportImport() {
        ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT);
        ExportImportConfig.setRealmName("test");

        // Restart server, which triggers export
        keycloakRule.restartServer();

        // Delete some realm (and some data in admin realm)
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmProvider realmProvider = session.realms();
            UserProvider userProvider = session.users();
            new RealmManager(session).removeRealm(realmProvider.getRealmByName("test"));
            Assert.assertEquals(1, realmProvider.getRealms().size());

            RealmModel master = realmProvider.getRealmByName(Config.getAdminRealm());
            session.users().removeUser(master, "admin2");

            assertNotAuthenticated(userProvider, realmProvider, Config.getAdminRealm(), "admin2", "admin2");
            assertNotAuthenticated(userProvider, realmProvider, "test", "test-user@localhost", "password");
            assertNotAuthenticated(userProvider, realmProvider, "test", "user1", "password");
            assertNotAuthenticated(userProvider, realmProvider, "test", "user2", "password");
            assertNotAuthenticated(userProvider, realmProvider, "test", "user3", "password");
        } finally {
            keycloakRule.stopSession(session, true);
        }

        // Configure import
        ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);

        // Restart server, which triggers import
        keycloakRule.restartServer();

        // Ensure data are imported back, but just for "test" realm
        session = keycloakRule.startSession();
        try {
            RealmProvider realmProvider = session.realms();
            UserProvider userProvider = session.users();
            Assert.assertEquals(2, realmProvider.getRealms().size());

            assertNotAuthenticated(userProvider, realmProvider, Config.getAdminRealm(), "admin2", "admin2");
            assertAuthenticated(userProvider, realmProvider, "test", "test-user@localhost", "password");
            assertAuthenticated(userProvider, realmProvider, "test", "user1", "password");
            assertAuthenticated(userProvider, realmProvider, "test", "user2", "password");
            assertAuthenticated(userProvider, realmProvider, "test", "user3", "password");

            addUser(userProvider, realmProvider.getRealmByName(Config.getAdminRealm()), "admin2", "admin2");
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    private void assertAuthenticated(UserProvider userProvider, RealmProvider realmProvider, String realmName, String username, String password) {
        RealmModel realm = realmProvider.getRealmByName(realmName);
        if (realm == null) {
            Assert.fail("realm " + realmName + " not found");
        }

        UserModel user = userProvider.getUserByUsername(username, realm);
        if (user == null) {
            Assert.fail("user " + username + " not found");
        }

        Assert.assertTrue(userProvider.validCredentials(realm, user, UserCredentialModel.password(password)));
    }

    private void assertNotAuthenticated(UserProvider userProvider, RealmProvider realmProvider, String realmName, String username, String password) {
        RealmModel realm = realmProvider.getRealmByName(realmName);
        if (realm == null) {
            return;
        }

        UserModel user = userProvider.getUserByUsername(username, realm);
        if (user == null) {
            return;
        }

        Assert.assertFalse(userProvider.validCredentials(realm, user, UserCredentialModel.password(password)));
    }

    private static void addUser(UserProvider userProvider, RealmModel appRealm, String username, String password) {
        UserModel user = userProvider.addUser(appRealm, username);
        user.setEmail(username + "@test.com");
        user.setEnabled(true);

        UserCredentialModel creds = new UserCredentialModel();
        creds.setType(CredentialRepresentation.PASSWORD);
        creds.setValue(password);
        user.updateCredential(creds);
    }

    private static String getExportImportTestDirectory() {
        String dirPath = null;
        String relativeDirExportImportPath = "testsuite" + File.separator + "integration" + File.separator + "target" + File.separator + "export-import";

        if (System.getProperties().containsKey("maven.home")) {
            dirPath = System.getProperty("user.dir").replaceFirst("testsuite.integration.*", Matcher.quoteReplacement(relativeDirExportImportPath));
        } else {
            for (String c : System.getProperty("java.class.path").split(File.pathSeparator)) {
                if (c.contains(File.separator + "testsuite" + File.separator + "integration")) {
                    dirPath = c.replaceFirst("testsuite.integration.*", Matcher.quoteReplacement(relativeDirExportImportPath));
                }
            }
        }

        String absolutePath = new File(dirPath).getAbsolutePath();
        return absolutePath;
    }

}

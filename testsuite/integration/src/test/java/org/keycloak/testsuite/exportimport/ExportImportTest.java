package org.keycloak.testsuite.exportimport;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportTest {
    /*

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

        private static final String MONGO_CLEAR_ON_STARTUP_PROP_NAME = "keycloak.model.mongo.clearOnStartup";
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
            {
                KeycloakSession session = server.getSessionFactory().create();
                session.getTransaction().begin();

                try {
                    RealmManager manager = new RealmManager(session);

                    RealmModel adminstrationRealm = manager.getRealm(Config.getAdminRealm());
                    RealmModel appRealm = manager.getRealm("test");
                    boolean removed = session.users().removeUser(appRealm, "user1");
                    removed = session.users().removeUser(appRealm, "user2");
                    removed = session.users().removeUser(appRealm, "user3");
                    removed = session.users().removeUser(adminstrationRealm, "admin2");

                    session.getTransaction().commit();
                } finally {
                    session.close();
                }
            }
            {
                KeycloakSession session = server.getSessionFactory().create();
                session.getTransaction().begin();

                try {
                    RealmManager manager = new RealmManager(session);

                    RealmModel adminstrationRealm = manager.getRealm(Config.getAdminRealm());
                    RealmModel appRealm = manager.getRealm("test");
                    UserModel user1 = session.users().getUserByUsername("user1", appRealm);
                    UserModel user2= session.users().getUserByUsername("user2", appRealm);
                    UserModel user3 = session.users().getUserByUsername("user3", appRealm);
                    UserModel admin2 = session.users().getUserByUsername("admin2", adminstrationRealm);
                    Assert.assertNull(user1);
                    Assert.assertNull(user2);
                    Assert.assertNull(user3);
                    Assert.assertNull(admin2);

                    session.getTransaction().commit();
                } finally {
                    session.close();
                }
            }

            super.after();
        }
    };

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(hibernateSetupRule)
            .around(mongoRule)
            .around(keycloakRule);

    @Test
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
            ModelProvider model = session.model();
            UserProvider userProvider = session.users();
            new RealmManager(session).removeRealm(model.getRealmByName("test"));
            Assert.assertEquals(1, model.getRealms().size());

            RealmModel master = model.getRealmByName(Config.getAdminRealm());
            session.users().removeUser(master, "admin2");
            assertNotAuthenticated(userProvider, model, Config.getAdminRealm(), "admin2", "admin2");
            assertNotAuthenticated(userProvider, model, "test", "test-user@localhost", "password");
            assertNotAuthenticated(userProvider, model, "test", "user1", "password");
            assertNotAuthenticated(userProvider, model, "test", "user2", "password");
            assertNotAuthenticated(userProvider, model, "test", "user3", "password");
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
            ModelProvider model = session.model();
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
            ModelProvider model = session.model();
            UserProvider userProvider = session.users();
            new RealmManager(session).removeRealm(model.getRealmByName("test"));
            Assert.assertEquals(1, model.getRealms().size());

            RealmModel master = model.getRealmByName(Config.getAdminRealm());
            session.users().removeUser(master, "admin2");

            assertNotAuthenticated(userProvider, model, Config.getAdminRealm(), "admin2", "admin2");
            assertNotAuthenticated(userProvider, model, "test", "test-user@localhost", "password");
            assertNotAuthenticated(userProvider, model, "test", "user1", "password");
            assertNotAuthenticated(userProvider, model, "test", "user2", "password");
            assertNotAuthenticated(userProvider, model, "test", "user3", "password");
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
            ModelProvider model = session.model();
            UserProvider userProvider = session.users();
            Assert.assertEquals(2, model.getRealms().size());

            assertNotAuthenticated(userProvider, model, Config.getAdminRealm(), "admin2", "admin2");
            assertAuthenticated(userProvider, model, "test", "test-user@localhost", "password");
            assertAuthenticated(userProvider, model, "test", "user1", "password");
            assertAuthenticated(userProvider, model, "test", "user2", "password");
            assertAuthenticated(userProvider, model, "test", "user3", "password");

            addUser(userProvider, model.getRealmByName(Config.getAdminRealm()), "admin2", "admin2");
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    private void assertAuthenticated(UserProvider userProvider, ModelProvider model, String realmName, String username, String password) {
        RealmModel realm = model.getRealmByName(realmName);
        if (realm == null) {
            Assert.fail("realm " + realmName + " not found");
        }

        UserModel user = userProvider.getUserByUsername(username, realm);
        if (user == null) {
            Assert.fail("user " + username + " not found");
        }

        Assert.assertTrue(realm.validatePassword(user, password));
    }

    private void assertNotAuthenticated(UserProvider userProvider, ModelProvider model, String realmName, String username, String password) {
        RealmModel realm = model.getRealmByName(realmName);
        if (realm == null) {
            return;
        }

        UserModel user = userProvider.getUserByUsername(username, realm);
        if (user == null) {
            return;
        }

        Assert.assertFalse(realm.validatePassword(user, password));
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
            dirPath = System.getProperty("user.dir").replaceFirst("testsuite.integration.*", relativeDirExportImportPath);
        } else {
            for (String c : System.getProperty("java.class.path").split(File.pathSeparator)) {
                if (c.contains(File.separator + "testsuite" + File.separator + "integration")) {
                    dirPath = c.replaceFirst("testsuite.integration.*", relativeDirExportImportPath);
                }
            }
        }

        String absolutePath = new File(dirPath).getAbsolutePath();
        return absolutePath;
    }
    */
}

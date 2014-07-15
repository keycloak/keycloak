package org.keycloak.exportimport;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ExportImportConfig {

    public static final String ACTION = "keycloak.migration.action";
    public static final String ACTION_EXPORT = "export";
    public static final String ACTION_IMPORT = "import";

    public static final String PROVIDER = "keycloak.migration.provider";
    public static final String PROVIDER_DEFAULT = "zip";

    // Name of the realm to export. If null, then full export will be triggered
    public static final String REALM_NAME = "keycloak.migration.realmName";

    // used for "dir" provider
    public static final String DIR = "keycloak.migration.dir";
    // used for "zip" provider
    public static final String ZIP_FILE = "keycloak.migration.zipFile";
    public static final String ZIP_PASSWORD = "keycloak.migration.zipPassword";
    // used for "singleFile" provider
    public static final String FILE = "keycloak.migration.file";

    // Number of users per file used in "dir" and "zip" providers. -1 means adding users to same file with realm. 0 means adding to separate file with unlimited page number
    public static final String USERS_PER_FILE = "keycloak.migration.usersPerFile";
    public static final Integer DEFAULT_USERS_PER_FILE = 5000;

    // Strategy used during import data
    public static final String STRATEGY = "keycloak.migration.strategy";
    public static final Strategy DEFAULT_STRATEGY = Strategy.OVERWRITE_EXISTING;

    public static String getAction() {
        return System.getProperty(ACTION);
    }

    public static void setAction(String exportImportAction) {
        System.setProperty(ACTION, exportImportAction);
    }

    public static String getProvider() {
        return System.getProperty(PROVIDER, PROVIDER_DEFAULT);
    }

    public static void setProvider(String exportImportProvider) {
        System.setProperty(PROVIDER, exportImportProvider);
    }

    public static String getRealmName() {
        return System.getProperty(REALM_NAME);
    }

    public static void setRealmName(String realmName) {
        if (realmName != null) {
            System.setProperty(REALM_NAME, realmName);
        } else {
            System.getProperties().remove(REALM_NAME);
        }
    }

    public static String getDir() {
        return System.getProperty(DIR);
    }

    public static String setDir(String dir) {
        return System.setProperty(DIR, dir);
    }

    public static String getZipFile() {
        return System.getProperty(ZIP_FILE);
    }

    public static void setZipFile(String exportImportZipFile) {
        System.setProperty(ZIP_FILE, exportImportZipFile);
    }

    public static String getZipPassword() {
        return System.getProperty(ZIP_PASSWORD);
    }

    public static void setZipPassword(String exportImportZipPassword) {
        System.setProperty(ZIP_PASSWORD, exportImportZipPassword);
    }

    public static String getFile() {
        return System.getProperty(FILE);
    }

    public static void setFile(String file) {
        System.setProperty(FILE, file);
    }

    public static Integer getUsersPerFile() {
        String usersPerFile = System.getProperty(USERS_PER_FILE, String.valueOf(DEFAULT_USERS_PER_FILE));
        return Integer.parseInt(usersPerFile.trim());
    }

    public static void setUsersPerFile(Integer usersPerFile) {
        System.setProperty(USERS_PER_FILE, String.valueOf(usersPerFile));
    }

    public static Strategy getStrategy() {
        String strategy = System.getProperty(STRATEGY, DEFAULT_STRATEGY.toString());
        return Enum.valueOf(Strategy.class, strategy);
    }
}

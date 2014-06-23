package org.keycloak.exportimport;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ExportImportConfig {

    public static final String ACTION = "keycloak.migration.action";
    public static final String PROVIDER = "keycloak.migration.provider";
    public static final String PROVIDER_DEFAULT = "zip";

    // used for "directory" provider
    public static final String DIR = "keycloak.migration.dir";
    // used for "zip" provider
    public static final String FILE = "keycloak.migration.zipFile";
    public static final String PASSWORD = "keycloak.migration.zipPassword";

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

    public static String getDir() {
        return System.getProperty(DIR);
    }

    public static String setDir(String dir) {
        return System.setProperty(DIR, dir);
    }

    public static String getZipFile() {
        return System.getProperty(FILE);
    }

    public static void setZipFile(String exportImportZipFile) {
        System.setProperty(FILE, exportImportZipFile);
    }

    public static String getZipPassword() {
        return System.getProperty(PASSWORD);
    }

    public static void setZipPassword(String exportImportZipPassword) {
        System.setProperty(PASSWORD, exportImportZipPassword);
    }

}

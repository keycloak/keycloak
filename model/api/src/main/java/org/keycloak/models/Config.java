package org.keycloak.models;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Config {

    public static final String ADMIN_REALM_KEY = "keycloak.admin.realm";
    public static final String ADMIN_REALM_DEFAULT = "keycloak-admin";

    public static final String MODEL_PROVIDER_KEY = "keycloak.model";

    public static final String AUDIT_PROVIDER_KEY = "keycloak.audit";
    public static final String AUDIT_PROVIDER_DEFAULT = "jpa";
    public static final String AUDIT_EXPIRATION_SCHEDULE_KEY = "keycloak.audit.expirationSchedule";
    public static final String AUDIT_EXPIRATION_SCHEDULE_DEFAULT = String.valueOf(TimeUnit.MINUTES.toMillis(15));

    public static final String PICKETLINK_PROVIDER_KEY = "keycloak.picketlink";

    public static final String THEME_BASE_KEY = "keycloak.theme.base";
    public static final String THEME_BASE_DEFAULT = "base";
    public static final String THEME_DEFAULT_KEY = "keycloak.theme.default";
    public static final String THEME_DEFAULT_DEFAULT = "keycloak";
    public static final String THEME_DIR_KEY = "keycloak.theme.dir";
    public static final String THEME_ADMIN_KEY = "keycloak.theme.admin";
    public static final String THEME_ADMIN_DEFAULT  = "keycloak";

    public static final String JBOSS_SERVER_CONFIG_DIR_KEY = "jboss.server.config.dir";

    public static final String TIMER_PROVIDER_KEY = "keycloak.timer";
    public static final String TIMER_PROVIDER_DEFAULT = "basic";

    public static final String EXPORT_IMPORT_ACTION = "keycloak.migration.action";
    public static final String EXPORT_IMPORT_PROVIDER = "keycloak.migration.provider";
    public static final String EXPORT_IMPORT_PROVIDER_DEFAULT = "zip";
    // used for "directory" provider
    public static final String EXPORT_IMPORT_DIR = "keycloak.migration.dir";
    // used for "zip" provider
    public static final String EXPORT_IMPORT_ZIP_FILE = "keycloak.migration.zipFile";
    public static final String EXPORT_IMPORT_ZIP_PASSWORD = "keycloak.migration.zipPassword";


    public static String getAdminRealm() {
        return System.getProperty(ADMIN_REALM_KEY, ADMIN_REALM_DEFAULT);
    }

    public static void setAdminRealm(String realm) {
        System.setProperty(ADMIN_REALM_KEY, realm);
    }

    public static String getAuditProvider() {
        return System.getProperty(AUDIT_PROVIDER_KEY, AUDIT_PROVIDER_DEFAULT);
    }

    public static void setAuditProvider(String provider) {
        System.setProperty(AUDIT_PROVIDER_KEY, provider);
    }

    public static String getAuditExpirationSchedule() {
        return System.getProperty(AUDIT_EXPIRATION_SCHEDULE_KEY, AUDIT_EXPIRATION_SCHEDULE_DEFAULT);
    }

    public static void setAuditExpirationSchedule(String schedule) {
        System.setProperty(AUDIT_EXPIRATION_SCHEDULE_KEY, schedule);
    }

    public static String getModelProvider() {
        return System.getProperty(MODEL_PROVIDER_KEY);
    }

    public static void setModelProvider(String provider) {
        System.setProperty(MODEL_PROVIDER_KEY, provider);
    }

    public static String getTimerProvider() {
        return System.getProperty(TIMER_PROVIDER_KEY, TIMER_PROVIDER_DEFAULT);
    }

    public static void setTimerProvider(String provider) {
        System.setProperty(TIMER_PROVIDER_KEY, provider);
    }

    public static String getIdentityManagerProvider() {
        return System.getProperty(PICKETLINK_PROVIDER_KEY, "realm");
    }

    public static void setIdentityManagerProvider(String provider) {
        System.setProperty(PICKETLINK_PROVIDER_KEY, provider);
    }

    public static String getThemeDir() {
        String themeDir = System.getProperty(THEME_DIR_KEY);
        if (themeDir == null && System.getProperties().containsKey(JBOSS_SERVER_CONFIG_DIR_KEY)) {
            themeDir = System.getProperty(JBOSS_SERVER_CONFIG_DIR_KEY) + File.separator + "themes";
        }
        return themeDir;
    }

    public static void setThemeDir(String dir) {
        System.setProperty(THEME_DIR_KEY, dir);
    }

    public static String getThemeBase() {
        return System.getProperty(THEME_BASE_KEY, THEME_BASE_DEFAULT);
    }

    public static void setThemeBase(String baseTheme) {
        System.setProperty(THEME_BASE_KEY, baseTheme);
    }

    public static String getThemeDefault() {
        return System.getProperty(THEME_DEFAULT_KEY, THEME_DEFAULT_DEFAULT);
    }

    public static void setThemeDefault(String defaultTheme) {
        System.setProperty(THEME_DEFAULT_KEY, defaultTheme);
    }

    public static String getThemeAdmin() {
        return System.getProperty(THEME_ADMIN_KEY, THEME_ADMIN_DEFAULT);
    }

    public static void setThemeAdmin(String adminTheme) {
        System.setProperty(THEME_ADMIN_KEY, adminTheme);
    }

    // EXPORT + IMPORT

    public static String getExportImportAction() {
        return System.getProperty(EXPORT_IMPORT_ACTION);
    }

    public static void setExportImportAction(String exportImportAction) {
        System.setProperty(EXPORT_IMPORT_ACTION, exportImportAction);
    }

    public static String getExportImportProvider() {
        return System.getProperty(EXPORT_IMPORT_PROVIDER, EXPORT_IMPORT_PROVIDER_DEFAULT);
    }

    public static void setExportImportProvider(String exportImportProvider) {
        System.setProperty(EXPORT_IMPORT_PROVIDER, exportImportProvider);
    }

    public static String getExportImportDir() {
        return System.getProperty(EXPORT_IMPORT_DIR);
    }

    public static void setExportImportDir(String exportImportDir) {
        System.setProperty(EXPORT_IMPORT_DIR, exportImportDir);
    }

    public static String getExportImportZipFile() {
        return System.getProperty(EXPORT_IMPORT_ZIP_FILE);
    }

    public static void setExportImportZipFile(String exportImportZipFile) {
        System.setProperty(EXPORT_IMPORT_ZIP_FILE, exportImportZipFile);
    }

    public static String getExportImportZipPassword() {
        return System.getProperty(EXPORT_IMPORT_ZIP_PASSWORD);
    }

    public static void setExportImportZipPassword(String exportImportZipPassword) {
        System.setProperty(EXPORT_IMPORT_ZIP_PASSWORD, exportImportZipPassword);
    }
}

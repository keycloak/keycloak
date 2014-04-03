package org.keycloak.models;

import java.io.File;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Config {

    public static final String ADMIN_REALM_KEY = "keycloak.admin.realm";
    public static final String ADMIN_REALM_DEFAULT = "keycloak-admin";

    public static final String MODEL_PROVIDER_KEY = "keycloak.model";

    public static final String MODEL_AUDIT_KEY = "keycloak.audit";

    public static final String THEME_BASE_KEY = "keycloak.theme.base";
    public static final String THEME_BASE_DEFAULT = "base";
    public static final String THEME_DEFAULT_KEY = "keycloak.theme.default";
    public static final String THEME_DEFAULT_DEFAULT = "keycloak";
    public static final String THEME_DIR_KEY = "keycloak.theme.dir";
    public static final String JBOSS_SERVER_CONFIG_DIR_KEY = "jboss.server.config.dir";

    public static String getAdminRealm() {
        return System.getProperty(ADMIN_REALM_KEY, ADMIN_REALM_DEFAULT);
    }

    public static void setAdminRealm(String realm) {
        System.setProperty(ADMIN_REALM_KEY, realm);
    }

    public static String getAuditProvider() {
        return System.getProperty(MODEL_PROVIDER_KEY);
    }

    public static void setAuditProvider(String provider) {
        System.setProperty(MODEL_PROVIDER_KEY, provider);
    }

    public static String getModelProvider() {
        return System.getProperty(MODEL_PROVIDER_KEY);
    }

    public static void setModelProvider(String provider) {
        System.setProperty(MODEL_PROVIDER_KEY, provider);
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

}

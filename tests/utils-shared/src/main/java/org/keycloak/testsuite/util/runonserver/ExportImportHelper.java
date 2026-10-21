package org.keycloak.testsuite.util.runonserver;

import java.nio.file.Files;

import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.Strategy;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;

import static org.keycloak.exportimport.ExportImportConfig.ACTION;
import static org.keycloak.exportimport.ExportImportConfig.DIR;
import static org.keycloak.exportimport.ExportImportConfig.FILE;
import static org.keycloak.exportimport.ExportImportConfig.PROVIDER;
import static org.keycloak.exportimport.ExportImportConfig.REALM_NAME;
import static org.keycloak.exportimport.ExportImportConfig.STRATEGY;
import static org.keycloak.exportimport.ExportImportConfig.USERS_PER_FILE;

public final class ExportImportHelper {

    private static String tempDir;

    public static RunOnServer runImport() {
        return session -> new ExportImportManager(session).runImport();
    }

    public static RunOnServer runExport() {
        return session -> new ExportImportManager(session).runExport();
    }

    public static RunOnServer setUsersPerFile(int usersPerFile) {
        return session -> System.setProperty(USERS_PER_FILE, String.valueOf(usersPerFile));
    }

    public static RunOnServer setDir(String dir) {
        return session -> System.setProperty(DIR, dir);
    }

    public static RunOnServer setStrategy(Strategy strategy) {
        return session -> System.setProperty(STRATEGY, strategy.name());
    }

    public static RunOnServer setProvider(String exportImportProvider) {
        return session -> System.setProperty(PROVIDER, exportImportProvider);
    }

    public static RunOnServer setFile(String file) {
        return session -> System.setProperty(FILE, file);
    }

    public static RunOnServer setAction(String exportImportAction) {
        return session -> System.setProperty(ACTION, exportImportAction);
    }

    public static RunOnServer setRealmName(String realmName) {
        return session -> {
            if (realmName != null && !realmName.isEmpty()) {
                System.setProperty(REALM_NAME, realmName);
            } else {
                System.getProperties().remove(REALM_NAME);
            }
        };
    }

    public static FetchOnServer getExportImportTestDirectory() {
        return session -> {
            if (tempDir == null) {
                tempDir = Files.createTempDirectory("kc-tests").toAbsolutePath().toString();
            }
            return tempDir;
        };
    }

    public static RunOnServer clear() {
        return session -> {
            System.clearProperty(REALM_NAME);
            System.clearProperty(PROVIDER);
            System.clearProperty(ACTION);
            System.clearProperty(FILE);
        };
    }
}

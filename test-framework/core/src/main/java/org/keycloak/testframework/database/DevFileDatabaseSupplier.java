package org.keycloak.testframework.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.keycloak.testframework.util.TmpDir;

import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class DevFileDatabaseSupplier extends AbstractDatabaseSupplier {

    private static final File DB_DIR = Path.of(TmpDir.resolveTmpDir().getAbsolutePath(), "kc-test-framework", "h2").toFile();

    @ConfigProperty(name = "reuse", defaultValue = "false")
    boolean reuse;

    @Override
    public String getAlias() {
        return "dev-file";
    }

    @Override
    TestDatabase getTestDatabase() {
        return new DevFileTestDatabase(reuse);
    }

    private static class DevFileTestDatabase implements TestDatabase {

        private final boolean reuse;

        public DevFileTestDatabase(boolean reuse) {
            this.reuse = reuse;
        }

        @Override
        public void start(DatabaseConfiguration config) {
            deleteDatabase();
            if (config.getInitScript() != null) {
                throw new IllegalArgumentException("init script not supported, configure h2 properties via --db-url-properties");
            }
        }

        @Override
        public void stop() {
            deleteDatabase();
        }

        private void deleteDatabase() {
            if (!reuse && DB_DIR.exists()) {
                try {
                    FileUtils.deleteDirectory(DB_DIR);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete directory: " + DB_DIR.getAbsolutePath(), e);
                }
            }
        }

        @Override
        public Map<String, String> serverConfig() {
            return Map.of(
                    "db", "dev-file",
                    "db-url", "jdbc:h2:file:" + DB_DIR + "/keycloak.db;DB_CLOSE_ON_EXIT=true;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=0"
                );
        }
    }

}

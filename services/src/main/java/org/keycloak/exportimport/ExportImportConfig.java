/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.exportimport;

import java.io.Closeable;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ExportImportConfig {

    public static final String PREFIX = "keycloak.migration.";
    public static final String ACTION = PREFIX + "action";
    public static final String ACTION_EXPORT = "export";
    public static final String ACTION_IMPORT = "import";

    public static final String SINGLE_TRANSACTION = PREFIX + "single-transaction";

    public static final String PROVIDER = PREFIX + "provider";
    public static final String PROVIDER_DEFAULT = "dir";

    // Name of the realm to export. If null, then full export will be triggered
    public static final String REALM_NAME = PREFIX + "realmName";

    // used for "dir" provider
    public static final String DIR = PREFIX + "dir";

    // used for "singleFile" provider
    public static final String FILE = PREFIX + "file";

    // used for replacing placeholders
    public static final String REPLACE_PLACEHOLDERS = PREFIX + "replace-placeholders";

    // How to export users when realm export is requested for "dir" provider
    public static final String USERS_EXPORT_STRATEGY = PREFIX + "usersExportStrategy";
    public static final UsersExportStrategy DEFAULT_USERS_EXPORT_STRATEGY = UsersExportStrategy.DIFFERENT_FILES;

    // Number of users per file used in "dir" provider. Used if usersExportStrategy is DIFFERENT_FILES
    public static final String USERS_PER_FILE = PREFIX + "usersPerFile";
    public static final Integer DEFAULT_USERS_PER_FILE = 50;

    // Strategy used during import data
    public static final String STRATEGY = PREFIX + "strategy";
    public static final Strategy DEFAULT_STRATEGY = Strategy.OVERWRITE_EXISTING;

    public static String getAction() {
        return System.getProperty(ACTION);
    }

    public static String getStrategy() {
        return System.getProperty(STRATEGY);
    }

    public static String setStrategy(Strategy strategy) {
        return System.setProperty(STRATEGY, strategy.toString());
    }

    public static Optional<String> getDir() {
        return Optional.ofNullable(System.getProperty(DIR));
    }

    public static Closeable setAction(String exportImportAction) {
        System.setProperty(ACTION, exportImportAction);
        return () -> System.getProperties().remove(ACTION);
    }

    public static void setProvider(String exportImportProvider) {
        System.setProperty(PROVIDER, exportImportProvider);
    }

    public static void setRealmName(String realmName) {
        if (realmName != null) {
            System.setProperty(REALM_NAME, realmName);
        } else {
            System.getProperties().remove(REALM_NAME);
        }
    }

    public static void setDir(String dir) {
        System.setProperty(DIR, dir);
    }

    public static void setFile(String file) {
        System.setProperty(FILE, file);
    }

    public static boolean isReplacePlaceholders() {
        return Boolean.getBoolean(REPLACE_PLACEHOLDERS);
    }

    public static void setReplacePlaceholders(boolean replacePlaceholders) {
        System.setProperty(REPLACE_PLACEHOLDERS, String.valueOf(replacePlaceholders));
    }

    public static void reset() {
        Stream.of(FILE, DIR, ACTION, STRATEGY, REPLACE_PLACEHOLDERS)
                .forEach(prop -> System.getProperties().remove(prop));
    }

    public static void setSingleTransaction(boolean b) {
        System.setProperty(SINGLE_TRANSACTION, String.valueOf(b));
    }

    public static boolean isSingleTransaction() {
        return Optional.ofNullable(System.getProperty(SINGLE_TRANSACTION)).map(Boolean::valueOf).orElse(Boolean.TRUE);
    }

}

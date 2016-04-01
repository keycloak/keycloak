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

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ExportImportConfig {

    public static final String PREFIX = "keycloak.migration.";
    public static final String ACTION = PREFIX + "action";
    public static final String ACTION_EXPORT = "export";
    public static final String ACTION_IMPORT = "import";

    public static final String PROVIDER = PREFIX + "provider";
    public static final String PROVIDER_DEFAULT = "dir";

    // Name of the realm to export. If null, then full export will be triggered
    public static final String REALM_NAME = PREFIX + "realmName";

    // used for "dir" provider
    public static final String DIR = PREFIX + "dir";

    // used for "singleFile" provider
    public static final String FILE = PREFIX + "file";

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

    public static String getFile() {
        return System.getProperty(FILE);
    }

    public static void setFile(String file) {
        System.setProperty(FILE, file);
    }

    public static UsersExportStrategy getUsersExportStrategy() {
        String usersExportStrategy = System.getProperty(USERS_EXPORT_STRATEGY, DEFAULT_USERS_EXPORT_STRATEGY.toString());
        return Enum.valueOf(UsersExportStrategy.class, usersExportStrategy);
    }

    public static void setUsersExportStrategy(UsersExportStrategy usersExportStrategy) {
        System.setProperty(USERS_EXPORT_STRATEGY, usersExportStrategy.toString());
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

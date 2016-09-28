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
package org.keycloak.testsuite.migration;

import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.testsuite.client.KeycloakTestingClient;

import java.io.File;

import static org.keycloak.testsuite.arquillian.migration.MigrationTestExecutionDecider.MIGRATED_AUTH_SERVER_VERSION_PROPERTY;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class MigrationUtil {

    private static final String migratedAuthServerVersion = System.getProperty(MIGRATED_AUTH_SERVER_VERSION_PROPERTY);
    
    static void executeImport(KeycloakTestingClient testingClient) {
        String realmJsonName = "migration-realm-" + migratedAuthServerVersion + ".json";
        
        File file = getRealmFilePath(realmJsonName, "src", "test", "resources", "migration-test");
        
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        testingClient.testing().exportImport().setFile(file.getAbsolutePath());
        
        // Configure import
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);
        testingClient.testing().exportImport().runImport();
    }
    
    private static File getRealmFilePath(String fileName, String... path) {
        StringBuilder builder = new StringBuilder();
        for (String dir : path) {
            builder.append(dir).append(File.separator);
        }
        builder.append(fileName);
        return new File(builder.toString());
    }
}

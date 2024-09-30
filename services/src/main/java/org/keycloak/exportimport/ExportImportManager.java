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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.keycloak.exportimport.ExportImportConfig.PROVIDER;
import static org.keycloak.exportimport.ExportImportConfig.PROVIDER_DEFAULT;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportManager {

    private static final Logger logger = Logger.getLogger(ExportImportManager.class);

    private final KeycloakSessionFactory sessionFactory;
    private final KeycloakSession session;

    private ExportProvider exportProvider;
    private ImportProvider importProvider;

    public ExportImportManager(KeycloakSession session) {
        this.sessionFactory = session.getKeycloakSessionFactory();
        this.session = session;

        String exportImportAction = ExportImportConfig.getAction();

        if (ExportImportConfig.ACTION_EXPORT.equals(exportImportAction)) {
            // Future Refactoring: If the system properties are no longer needed for integration tests, refactor to use
            // a default provider in its standard way.
            // Setting this to "provider" doesn't work yet when instrumenting Keycloak with Quarkus as it leads to
            // "java.lang.NullPointerException: Cannot invoke "String.indexOf(String)" because "value" is null"
            // when calling "Config.getProvider()" from "KeycloakProcessor.loadFactories()"
            String providerId = System.getProperty(PROVIDER, Config.scope("export").get("exporter", PROVIDER_DEFAULT));
            exportProvider = session.getProvider(ExportProvider.class, providerId);
            if (exportProvider == null) {
                throw new RuntimeException("Export provider '" + providerId + "' not found");
            }
        } else if (ExportImportConfig.ACTION_IMPORT.equals(exportImportAction)) {
            String providerId = System.getProperty(PROVIDER, Config.scope("import").get("importer", PROVIDER_DEFAULT));
            importProvider = session.getProvider(ImportProvider.class, providerId);
            if (importProvider == null) {
                throw new RuntimeException("Import provider '" + providerId + "' not found");
            }
        }
    }

    public boolean isRunImport() {
        return importProvider != null;
    }

    public boolean isImportMasterIncluded() {
        if (!isRunImport()) {
            throw new IllegalStateException("Import not enabled");
        }
        try {
            return importProvider.isMasterRealmExported();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean isImportMasterIncludedAtStartup(String dir) {
        if (dir == null) {
            throw new IllegalStateException("Import not enabled");
        }
        
        return getStartupImportProviders(dir).map(Supplier::get).anyMatch(provider -> {
            try {
                return provider.isMasterRealmExported();
            } catch (IOException e) {
                throw new RuntimeException("Failed to run import", e);
            }
        });
    }

    public boolean isRunExport() {
        return exportProvider != null;
    }

    public void runImport() {
        try {
            importProvider.importModel();
        } catch (IOException e) {
            throw new RuntimeException("Failed to run import", e);
        }
    }
    
    public void runImportAtStartup(String dir) throws IOException {
        System.setProperty(ExportImportConfig.STRATEGY, Strategy.IGNORE_EXISTING.toString());
        ExportImportConfig.setReplacePlaceholders(true);
        // enables logging of what is imported
        ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);
        
        // TODO: ideally the static setting above should be unset after this is run 
        
        getStartupImportProviders(dir).map(Supplier::get).forEach(ip -> {
            try {
                ip.importModel();
            } catch (IOException e) {
                throw new RuntimeException("Failed to run import", e);
            }
        });
    }

    private Stream<Supplier<ImportProvider>> getStartupImportProviders(String dir) {
        Stream<ProviderFactory> factories = sessionFactory.getProviderFactoriesStream(ImportProvider.class);

        return factories.flatMap(factory -> {
            String providerId = factory.getId();

            if ("dir".equals(providerId)) {
                Supplier<ImportProvider> func = () -> {
                    ExportImportConfig.setDir(dir);
                    return session.getProvider(ImportProvider.class, providerId);
                };
                return Stream.of(func);
            }
            if ("singleFile".equals(providerId)) {
                Set<String> filesToImport = new HashSet<>();

                File[] files = Paths.get(dir).toFile().listFiles();
                Objects.requireNonNull(files, "directory not found");
                for (File file : files) {
                    Path filePath = file.toPath();

                    if (!(Files.exists(filePath) && Files.isRegularFile(filePath) && filePath.toString().endsWith(".json"))) {
                        logger.debugf("Ignoring import file because it is not a valid file: %s", file);
                        continue;
                    }

                    String fileName = file.getName();

                    if (fileName.contains("-realm.json") || fileName.contains("-users-")) {
                        continue;
                    }

                    filesToImport.add(file.getAbsolutePath());
                }
                
                return filesToImport.stream().map(file -> () -> {
                    ExportImportConfig.setFile(file);
                    return session.getProvider(ImportProvider.class, providerId);
                });
            }
            return Stream.empty();
        });
    }

    public void runExport() {
        try {
            exportProvider.exportModel();
        } catch (IOException e) {
            throw new RuntimeException("Failed to run export", e);
        }
    }

}

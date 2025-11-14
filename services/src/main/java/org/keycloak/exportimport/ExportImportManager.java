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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

import org.jboss.logging.Logger;

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
    private List<ImportProvider> importProviders = List.of();

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
            ImportProvider importProvider = session.getProvider(ImportProvider.class, providerId);
            if (importProvider == null) {
                throw new RuntimeException("Import provider '" + providerId + "' not found");
            }
            importProviders = List.of(importProvider);
        } else if (ExportImportConfig.getDir().isPresent()) { // import at startup
            ExportImportConfig.setStrategy(Strategy.IGNORE_EXISTING);
            ExportImportConfig.setReplacePlaceholders(true);
            // enables logging of what is imported
            ExportImportConfig.setAction(ExportImportConfig.ACTION_IMPORT);
            importProviders = getStartupImportProviders();
        }
    }

    public boolean isImportMasterIncluded() {
        return importProviders.stream().anyMatch(provider -> {
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
        importProviders.forEach(ip -> {
            try {
                ip.importModel();
            } catch (IOException e) {
                throw new RuntimeException("Failed to run import", e);
            }
        });
    }

    private List<ImportProvider> getStartupImportProviders() {
        var dirProp = ExportImportConfig.getDir();
        if (dirProp.isEmpty()) {
            return List.of();
        }
        String dir = dirProp.get();

        Stream<ProviderFactory> factories = sessionFactory.getProviderFactoriesStream(ImportProvider.class);

        return factories.flatMap(factory -> {
            String providerId = factory.getId();

            if ("dir".equals(providerId)) {
                return Stream.of(session.getProvider(ImportProvider.class, providerId));
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

                if (factory instanceof ImportProviderFactory) {
                    return filesToImport.stream().map(file -> ((ImportProviderFactory)factory).create(session, Map.of(ExportImportConfig.FILE, file)));
                }
            }
            return Stream.empty();
        }).toList();
    }

    public void runExport() {
        try {
            exportProvider.exportModel();
        } catch (IOException e) {
            throw new RuntimeException("Failed to run export", e);
        }
    }

}

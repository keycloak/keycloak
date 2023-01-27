/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.file;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import java.io.File;
import java.nio.file.Path;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import static org.keycloak.models.map.storage.ModelEntityUtil.getModelName;
import static org.keycloak.models.map.storage.ModelEntityUtil.getModelNames;

/**
 * A {@link MapStorageProviderFactory} that creates file-based {@link MapStorageProvider}s.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FileMapStorageProviderFactory implements AmphibianProviderFactory<MapStorageProvider>,
        MapStorageProviderFactory,
        EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "file";
    private Path rootRealmsDirectory;
    private final Map<String, Function<String, Path>> rootAreaDirectories = new HashMap<>();    // Function: (realmId) -> path
    private final Map<Class<?>, FileMapStorage<?, ?>> storages = new HashMap<>();

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        return new FileMapStorageProvider(this);
    }

    @Override
    public String getHelpText() {
        return "File Map Storage";
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

    @Override
    public void init(Config.Scope config) {
        final String dir = config.get("dir");
        rootRealmsDirectory = dir == null ? null : Path.of(dir);
        getModelNames().stream()
          .filter(n -> ! Objects.equals(n, getModelName(RealmModel.class)))
          .forEach(n -> rootAreaDirectories.put(n, getRootDir(rootRealmsDirectory, n, config.get("dir." + n))));

        if (rootAreaDirectories != null) {
            rootAreaDirectories.put(getModelName(RealmModel.class), realmId -> rootRealmsDirectory);
        }
    }

    private static final Pattern FORBIDDEN_CHARACTERS = Pattern.compile("[\\.\\" + File.separator + "]");

    private static Function<String, Path> getRootDir(Path rootRealmsDirectory, String areaName, String dirFromConfig) {
        if (dirFromConfig != null) {
            if (rootRealmsDirectory == null) {
                return p -> { throw new IllegalStateException("Directory for " + areaName + " area not configured."); };
            }

            Path p = Path.of(dirFromConfig);
            return realmId -> p;
        } else {
            return realmId -> {
              if (realmId == null || FORBIDDEN_CHARACTERS.matcher(realmId).find()) {
                  throw new IllegalArgumentException("Realm needed for constructing the path to " + areaName + " but not known");
              }

              final Path path = rootRealmsDirectory
                .resolve(realmId)
                .resolve(areaName);

              return path;
            };
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    public <V extends AbstractEntity & UpdatableEntity, M> FileMapStorage<V, M> initFileStorage(Class<M> modelType) {
        String name = getModelName(modelType, modelType.getSimpleName());
        FileMapStorage<V, M> res = new FileMapStorage<>(ModelEntityUtil.getEntityType(modelType), rootAreaDirectories.get(name));
        return res;
    }

    <M> FileMapStorage getStorage(Class<M> modelType, Flag[] flags) {
        try {
            return storages.computeIfAbsent(modelType, n -> initFileStorage(modelType));
        } catch (ConcurrentModificationException ex) {
            return storages.get(modelType);
        }
    }
}

/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.clientscope.MapClientScopeEntity;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import static java.util.Map.entry;
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

    private static final Map<Class<?>, Function<?, String[]>> UNIQUE_HUMAN_READABLE_NAME_FIELD = Map.ofEntries(
      entry(MapClientEntity.class,          ((Function<MapClientEntity, String[]>) v -> new String[] { v.getClientId() })),
      entry(MapClientScopeEntity.class,     ((Function<MapClientScopeEntity, String[]>) v -> new String[] { v.getName() })),
      entry(MapGroupEntity.class,           ((Function<MapGroupEntity, String[]>) v -> v.getParentId() == null 
                                                                                         ? new String[] { v.getName() }
                                                                                         : new String[] { v.getParentId(), v.getName() })),
      entry(MapRealmEntity.class,           ((Function<MapRealmEntity, String[]>) v -> new String[] { v.getName()})),
      entry(MapRoleEntity.class,            ((Function<MapRoleEntity, String[]>) (v -> v.getClientId() == null
                                                                                         ? new String[] { v.getName() }
                                                                                         : new String[] { v.getClientId(), v.getName() }))),
      entry(MapUserEntity.class,            ((Function<MapUserEntity, String[]>) v -> new String[] { v.getUsername() })),

      // authz
      entry(MapResourceServerEntity.class,  ((Function<MapResourceServerEntity, String[]>) v -> new String[] { v.getClientId() })),
      entry(MapPolicyEntity.class,          ((Function<MapPolicyEntity, String[]>) v -> new String[] { v.getResourceServerId(), v.getName() })),
      entry(MapPermissionTicketEntity.class,((Function<MapPermissionTicketEntity, String[]>) v -> new String[] { v.getResourceServerId(), v.getId()})),
      entry(MapResourceEntity.class,        ((Function<MapResourceEntity, String[]>) v -> new String[] { v.getResourceServerId(), v.getName() })),
      entry(MapScopeEntity.class,           ((Function<MapScopeEntity, String[]>) v -> new String[] { v.getResourceServerId(), v.getName() }))
    );

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
            rootAreaDirectories.put(getModelName(RealmModel.class), realmId -> realmId == null ? rootRealmsDirectory : rootRealmsDirectory.resolve(realmId) );
        }
    }

    private static final Pattern FORBIDDEN_CHARACTERS = Pattern.compile("[\\.\\" + File.separator + "]");

    private static Function<String, Path> getRootDir(Path rootRealmsDirectory, String areaName, String dirFromConfig) {
        if (dirFromConfig != null) {
            Path p = Path.of(dirFromConfig);
            return realmId -> p;
        } else {
            if (rootRealmsDirectory == null) {
                return p -> { throw new IllegalStateException("Directory for " + areaName + " area not configured."); };
            }

            Path a = areaName.startsWith("authz-") ? Path.of("authz", areaName.substring(6)) : Path.of(areaName);

            return realmId -> {
              if (realmId == null || FORBIDDEN_CHARACTERS.matcher(realmId).find()) {
                  throw new IllegalArgumentException("Realm needed for constructing the path to " + areaName + " but not known or invalid: " + realmId);
              }

              final Path path = rootRealmsDirectory
                .resolve(realmId)
                .resolve(a);

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
        final Class<V> et = ModelEntityUtil.getEntityType(modelType);
        @SuppressWarnings("unchecked")
        FileMapStorage<V, M> res = new FileMapStorage<>(et, (Function<V, String[]>) UNIQUE_HUMAN_READABLE_NAME_FIELD.get(et), rootAreaDirectories.get(name));
        return res;
    }

    <M> FileMapStorage getStorage(Class<M> modelType, Flag[] flags) {
        try {
            if (modelType == SingleUseObjectValueModel.class) {
                throw new IllegalArgumentException("Unsupported file storage: " + ModelEntityUtil.getModelName(modelType));
            }
            return storages.computeIfAbsent(modelType, n -> initFileStorage(modelType));
        } catch (ConcurrentModificationException ex) {
            return storages.get(modelType);
        }
    }
}

/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.chm;

import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntityImpl;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntityImpl;
import org.keycloak.models.map.authorization.entity.MapResourceEntityImpl;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntityImpl;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntityImpl;
import org.keycloak.models.map.common.StringKeyConvertor;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.client.MapClientEntityImpl;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntityImpl;
import org.keycloak.models.map.clientscope.MapClientScopeEntityImpl;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.Serialization;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.group.MapGroupEntityImpl;
import org.keycloak.models.map.role.MapRoleEntityImpl;
import com.fasterxml.jackson.databind.JavaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.user.MapUserConsentEntityImpl;
import org.keycloak.models.map.user.MapUserCredentialEntityImpl;
import org.keycloak.models.map.user.MapUserEntityImpl;
import org.keycloak.models.map.user.MapUserFederatedIdentityEntityImpl;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.keycloak.models.map.storage.ModelEntityUtil.getModelName;
import static org.keycloak.models.map.storage.ModelEntityUtil.getModelNames;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorageProviderFactory implements AmphibianProviderFactory<MapStorageProvider>, MapStorageProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "concurrenthashmap";

    private static final Logger LOG = Logger.getLogger(ConcurrentHashMapStorageProviderFactory.class);

    private final ConcurrentHashMap<String, ConcurrentHashMapStorage<?,?,?>> storages = new ConcurrentHashMap<>();

    private final Map<String, StringKeyConvertor> keyConvertors = new HashMap<>();

    private File storageDirectory;

    private String suffix;

    private StringKeyConvertor defaultKeyConvertor;

    private final static DeepCloner CLONER = new DeepCloner.Builder()
      .genericCloner(Serialization::from)
      .constructor(MapClientEntityImpl.class,                       MapClientEntityImpl::new)
      .constructor(MapProtocolMapperEntity.class,                   MapProtocolMapperEntityImpl::new)
      .constructor(MapGroupEntityImpl.class,                        MapGroupEntityImpl::new)
      .constructor(MapRoleEntityImpl.class,                         MapRoleEntityImpl::new)
      .constructor(MapUserEntityImpl.class,                         MapUserEntityImpl::new)
      .constructor(MapUserCredentialEntityImpl.class,               MapUserCredentialEntityImpl::new)
      .constructor(MapUserFederatedIdentityEntityImpl.class,        MapUserFederatedIdentityEntityImpl::new)
      .constructor(MapUserConsentEntityImpl.class,                  MapUserConsentEntityImpl::new)
      .constructor(MapClientScopeEntityImpl.class,                  MapClientScopeEntityImpl::new)
      .constructor(MapResourceServerEntityImpl.class,               MapResourceServerEntityImpl::new)
      .constructor(MapResourceEntityImpl.class,                     MapResourceEntityImpl::new)
      .constructor(MapScopeEntity.class,                            MapScopeEntityImpl::new)
      .constructor(MapPolicyEntity.class,                           MapPolicyEntityImpl::new)
      .constructor(MapPermissionTicketEntity.class,                 MapPermissionTicketEntityImpl::new)
      .build();

    private static final Map<String, StringKeyConvertor> KEY_CONVERTORS = new HashMap<>();
    static {
        KEY_CONVERTORS.put("uuid", StringKeyConvertor.UUIDKey.INSTANCE);
        KEY_CONVERTORS.put("string", StringKeyConvertor.StringKey.INSTANCE);
        KEY_CONVERTORS.put("ulong", StringKeyConvertor.ULongKey.INSTANCE);
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        return new ConcurrentHashMapStorageProvider(this);
    }


    @Override
    public void init(Scope config) {
        if (config instanceof ComponentModelScope) {
            this.suffix = "-" + ((ComponentModelScope) config).getComponentId();
        } else {
            this.suffix = "";
        }
    
        final String keyType = config.get("keyType", "uuid");
        defaultKeyConvertor = getKeyConvertor(keyType);
        for (String name : getModelNames()) {
            keyConvertors.put(name, getKeyConvertor(config.get("keyType." + name, keyType)));
        }

        final String dir = config.get("dir");
        try {
            if (dir == null || dir.trim().isEmpty()) {
                LOG.warn("No directory set, created objects will not survive server restart");
                this.storageDirectory = null;
            } else {
                File f = new File(dir);
                Files.createDirectories(f.toPath());
                if (f.exists()) {
                    this.storageDirectory = f;
                } else {
                    LOG.warnf("Directory cannot be used, created objects will not survive server restart: %s", dir);
                    this.storageDirectory = null;
                }
            }
        } catch (IOException ex) {
            LOG.warnf("Directory cannot be used, created objects will not survive server restart: %s", dir);
            this.storageDirectory = null;
        }
    }

    private StringKeyConvertor getKeyConvertor(final String keyType) throws IllegalArgumentException {
        StringKeyConvertor res = KEY_CONVERTORS.get(keyType);
        if (res == null) {
            throw new IllegalArgumentException("Unknown key type: " + keyType);
        }
        return res;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        storages.forEach(this::storeMap);
    }

    @SuppressWarnings("unchecked")
    private void storeMap(String mapName, ConcurrentHashMapStorage<?, ?, ?> store) {
        if (mapName != null) {
            File f = getFile(mapName);
            try {
                if (storageDirectory != null) {
                    LOG.debugf("Storing contents to %s", f.getCanonicalPath());
                    final DefaultModelCriteria readAllCriteria = criteria();
                    Serialization.MAPPER.writeValue(f, store.read(withCriteria(readAllCriteria)));
                } else {
                    LOG.debugf("Not storing contents of %s because directory not set", mapName);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <K, V extends AbstractEntity & UpdatableEntity, M> ConcurrentHashMapStorage<K, V, M> loadMap(String mapName,
      Class<M> modelType, EnumSet<Flag> flags) {
        final StringKeyConvertor kc = keyConvertors.getOrDefault(mapName, defaultKeyConvertor);
        Class<?> valueType = ModelEntityUtil.getEntityType(modelType);
        LOG.debugf("Initializing new map storage: %s", mapName);

        ConcurrentHashMapStorage<K, V, M> store;
        if (modelType == UserSessionModel.class) {
            ConcurrentHashMapStorage clientSessionStore = getStorage(AuthenticatedClientSessionModel.class);
            store = new UserSessionConcurrentHashMapStorage(clientSessionStore, kc, CLONER) {
                @Override
                public String toString() {
                    return "ConcurrentHashMapStorage(" + mapName + suffix + ")";
                }
            };
        } else {
            store = new ConcurrentHashMapStorage(modelType, kc, CLONER) {
                @Override
                public String toString() {
                    return "ConcurrentHashMapStorage(" + mapName + suffix + ")";
                }
            };
        }

        if (! flags.contains(Flag.INITIALIZE_EMPTY)) {
            final File f = getFile(mapName);
            if (f != null && f.exists()) {
                try {
                    LOG.debugf("Restoring contents from %s", f.getCanonicalPath());
                    Class<?> valueImplType = CLONER.newInstanceType(valueType);
                    if (valueImplType == null) {
                        valueImplType = valueType;
                    }
                    JavaType type = Serialization.MAPPER.getTypeFactory().constructCollectionType(LinkedList.class, valueImplType);

                    List<V> values = Serialization.MAPPER.readValue(f, type);
                    values.forEach((V mce) -> store.create(mce));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return store;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @SuppressWarnings("unchecked")
    public <K, V extends AbstractEntity & UpdatableEntity, M> ConcurrentHashMapStorage<K, V, M> getStorage(
      Class<M> modelType, Flag... flags) {
        EnumSet<Flag> f = flags == null || flags.length == 0 ? EnumSet.noneOf(Flag.class) : EnumSet.of(flags[0], flags);
        String name = getModelName(modelType, modelType.getSimpleName());
        /* From ConcurrentHashMapStorage.computeIfAbsent javadoc:
         *
         *   "... the computation [...] must not attempt to update any other mappings of this map."
         *
         * For UserSessionModel, there is a separate clientSessionStore in this CHM implementation. Thus
         * we cannot guarantee that this won't be the case e.g. for user and client sessions. Hence we need
         * to prepare clientSessionStore outside computeIfAbsent, otherwise deadlock occurs.
         */
        if (modelType == UserSessionModel.class) {
            getStorage(AuthenticatedClientSessionModel.class, flags);
        }
        return (ConcurrentHashMapStorage<K, V, M>) storages.computeIfAbsent(name, n -> loadMap(name, modelType, f));
    }

    private File getFile(String fileName) {
        return storageDirectory == null
          ? null
          : new File(storageDirectory, "map-" + fileName + suffix + ".json");
    }

    @Override
    public String getHelpText() {
        return "In-memory ConcurrentHashMap storage";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }
}

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

import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.Config.Scope;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityImpl;
import org.keycloak.models.map.clientscope.MapClientScopeEntity;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.Serialization;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.loginFailure.MapUserLoginFailureEntity;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.role.MapRoleEntity;
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
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.storage.StringKeyConvertor;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.keycloak.models.map.storage.QueryParameters.withCriteria;

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

    public static final Map<Class<?>, String> MODEL_TO_NAME = new HashMap<>();
    static {
        MODEL_TO_NAME.put(AuthenticatedClientSessionModel.class, "client-sessions");
        MODEL_TO_NAME.put(ClientScopeModel.class, "client-scopes");
        MODEL_TO_NAME.put(ClientModel.class, "clients");
        MODEL_TO_NAME.put(GroupModel.class, "groups");
        MODEL_TO_NAME.put(RealmModel.class, "realms");
        MODEL_TO_NAME.put(RoleModel.class, "roles");
        MODEL_TO_NAME.put(RootAuthenticationSessionModel.class, "auth-sessions");
        MODEL_TO_NAME.put(UserLoginFailureModel.class, "user-login-failures");
        MODEL_TO_NAME.put(UserModel.class, "users");
        MODEL_TO_NAME.put(UserSessionModel.class, "user-sessions");

        // authz
        MODEL_TO_NAME.put(PermissionTicket.class, "authz-permission-tickets");
        MODEL_TO_NAME.put(Policy.class, "authz-policies");
        MODEL_TO_NAME.put(ResourceServer.class, "authz-resource-servers");
        MODEL_TO_NAME.put(Resource.class, "authz-resources");
        MODEL_TO_NAME.put(org.keycloak.authorization.model.Scope.class, "authz-scopes");
    }

    public static final Map<Class<?>, Class<?>> INTERFACE_TO_IMPL = new HashMap<>();
    static {
        INTERFACE_TO_IMPL.put(MapClientEntity.class, MapClientEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapClientScopeEntity.class, MapClientScopeEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapClientEntity.class, MapClientEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapGroupEntity.class, MapGroupEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapRealmEntity.class, MapRealmEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapRoleEntity.class, MapRoleEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapRootAuthenticationSessionEntity.class, MapRootAuthenticationSessionEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapUserLoginFailureEntity.class, MapUserLoginFailureEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapUserEntity.class, MapUserEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapUserSessionEntity.class, MapUserSessionEntityImpl.class);
//
//        // authz
//        INTERFACE_TO_IMPL.put(MapPermissionTicketEntity.class, MapPermissionTicketEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapPolicyEntity.class, MapPolicyEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapResourceServerEntity.class, MapResourceServerEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapResourceEntity.class, MapResourceEntityImpl.class);
//        INTERFACE_TO_IMPL.put(MapScopeEntity.class, MapScopeEntityImpl.class);
    }

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
        for (String name : MODEL_TO_NAME.values()) {
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

    private void storeMap(String mapName, ConcurrentHashMapStorage<?, ?, ?> store) {
        if (mapName != null) {
            File f = getFile(mapName);
            try {
                if (storageDirectory != null) {
                    LOG.debugf("Storing contents to %s", f.getCanonicalPath());
                    @SuppressWarnings("unchecked")
                    final ModelCriteriaBuilder readAllCriteria = store.createCriteriaBuilder();
                    Serialization.MAPPER.writeValue(f, store.read(withCriteria(readAllCriteria)));
                } else {
                    LOG.debugf("Not storing contents of %s because directory not set", mapName);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private <K, V extends AbstractEntity<K>, M> ConcurrentHashMapStorage<K, V, M> loadMap(String mapName,
      Class<V> valueType, Class<M> modelType, EnumSet<Flag> flags) {
        final StringKeyConvertor kc = keyConvertors.getOrDefault(mapName, defaultKeyConvertor);

        LOG.debugf("Initializing new map storage: %s", mapName);

        @SuppressWarnings("unchecked")
        ConcurrentHashMapStorage<K, V, M> store;
        if (modelType == UserSessionModel.class) {
            ConcurrentHashMapStorage clientSessionStore = getStorage(MapAuthenticatedClientSessionEntity.class, AuthenticatedClientSessionModel.class);
            store = new UserSessionConcurrentHashMapStorage(clientSessionStore, kc) {
                @Override
                public String toString() {
                    return "ConcurrentHashMapStorage(" + mapName + suffix + ")";
                }
            };
        } else {
            store = new ConcurrentHashMapStorage(modelType, kc) {
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
                    Class<?> valueImplType = INTERFACE_TO_IMPL.getOrDefault(valueType, valueType);
                    JavaType type = Serialization.MAPPER.getTypeFactory().constructCollectionType(List.class, valueImplType);

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
    public <K, V extends AbstractEntity<K>, M> ConcurrentHashMapStorage<K, V, M> getStorage(
      Class<V> valueType, Class<M> modelType, Flag... flags) {
        EnumSet<Flag> f = flags == null || flags.length == 0 ? EnumSet.noneOf(Flag.class) : EnumSet.of(flags[0], flags);
        String name = MODEL_TO_NAME.getOrDefault(modelType, modelType.getSimpleName());
        /* From ConcurrentHashMapStorage.computeIfAbsent javadoc:
         *
         *   "... the computation [...] must not attempt to update any other mappings of this map."
         *
         * For UserSessionModel, there is a separate clientSessionStore in this CHM implementation. Thus
         * we cannot guarantee that this won't be the case e.g. for user and client sessions. Hence we need
         * to prepare clientSessionStore outside computeIfAbsent, otherwise deadlock occurs.
         */
        if (modelType == UserSessionModel.class) {
            getStorage(MapAuthenticatedClientSessionEntity.class, AuthenticatedClientSessionModel.class);
        }
        return (ConcurrentHashMapStorage<K, V, M>) storages.computeIfAbsent(name, n -> loadMap(name, valueType, modelType, f));
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

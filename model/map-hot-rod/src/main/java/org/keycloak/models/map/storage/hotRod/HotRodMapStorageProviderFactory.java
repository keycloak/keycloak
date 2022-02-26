/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.clientscope.MapClientScopeEntity;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.storage.hotRod.role.HotRodRoleEntity;
import org.keycloak.models.map.storage.hotRod.role.HotRodRoleEntityDelegate;
import org.keycloak.models.map.storage.hotRod.client.HotRodClientEntity;
import org.keycloak.models.map.storage.hotRod.client.HotRodClientEntityDelegate;
import org.keycloak.models.map.storage.hotRod.client.HotRodProtocolMapperEntityDelegate;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.storage.hotRod.clientscope.HotRodClientScopeEntity;
import org.keycloak.models.map.storage.hotRod.clientscope.HotRodClientScopeEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDescriptor;
import org.keycloak.models.map.storage.hotRod.connections.HotRodConnectionProvider;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.hotRod.group.HotRodGroupEntity;
import org.keycloak.models.map.storage.hotRod.group.HotRodGroupEntityDelegate;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserConsentEntityDelegate;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserCredentialEntityDelegate;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserEntity;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserEntityDelegate;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserFederatedIdentityEntityDelegate;
import org.keycloak.models.map.user.MapUserConsentEntity;
import org.keycloak.models.map.user.MapUserCredentialEntity;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.user.MapUserFederatedIdentityEntity;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.HashMap;
import java.util.Map;

public class HotRodMapStorageProviderFactory implements AmphibianProviderFactory<MapStorageProvider>, MapStorageProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "hotrod";
    private static final Logger LOG = Logger.getLogger(HotRodMapStorageProviderFactory.class);

    private final static DeepCloner CLONER = new DeepCloner.Builder()
            .constructor(MapClientEntity.class,                     HotRodClientEntityDelegate::new)
            .constructor(MapProtocolMapperEntity.class,             HotRodProtocolMapperEntityDelegate::new)
            .constructor(MapClientScopeEntity.class,                HotRodClientScopeEntityDelegate::new)
            .constructor(MapGroupEntity.class,                      HotRodGroupEntityDelegate::new)
            .constructor(MapRoleEntity.class,                       HotRodRoleEntityDelegate::new)
            .constructor(MapUserEntity.class,                       HotRodUserEntityDelegate::new)
            .constructor(MapUserCredentialEntity.class,             HotRodUserCredentialEntityDelegate::new)
            .constructor(MapUserFederatedIdentityEntity.class,      HotRodUserFederatedIdentityEntityDelegate::new)
            .constructor(MapUserConsentEntity.class,                HotRodUserConsentEntityDelegate::new)
            .build();

    public static final Map<Class<?>, HotRodEntityDescriptor<?, ?>> ENTITY_DESCRIPTOR_MAP = new HashMap<>();
    static {
        // Clients descriptor
        ENTITY_DESCRIPTOR_MAP.put(ClientModel.class,
                new HotRodEntityDescriptor<>(ClientModel.class,
                        HotRodClientEntity.class,
                        HotRodClientEntityDelegate::new));

        ENTITY_DESCRIPTOR_MAP.put(ClientScopeModel.class,
                new HotRodEntityDescriptor<>(ClientScopeModel.class,
                        HotRodClientScopeEntity.class,
                        HotRodClientScopeEntityDelegate::new));

        // Groups descriptor
        ENTITY_DESCRIPTOR_MAP.put(GroupModel.class,
                new HotRodEntityDescriptor<>(GroupModel.class,
                        HotRodGroupEntity.class,
                        HotRodGroupEntityDelegate::new));

        // Roles descriptor
        ENTITY_DESCRIPTOR_MAP.put(RoleModel.class,
                new HotRodEntityDescriptor<>(RoleModel.class,
                        HotRodRoleEntity.class,
                        HotRodRoleEntityDelegate::new));

        // Users descriptor
        ENTITY_DESCRIPTOR_MAP.put(UserModel.class,
                new HotRodEntityDescriptor<>(UserModel.class,
                        HotRodUserEntity.class,
                        HotRodUserEntityDelegate::new));
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        HotRodConnectionProvider cacheProvider = session.getProvider(HotRodConnectionProvider.class);
        
        if (cacheProvider == null) {
            throw new IllegalStateException("Cannot find HotRodConnectionProvider interface implementation");
        }
        
        return new HotRodMapStorageProvider(this, cacheProvider, CLONER);
    }

    public HotRodEntityDescriptor<?, ?> getEntityDescriptor(Class<?> c) {
        return ENTITY_DESCRIPTOR_MAP.get(c);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

    @Override
    public String getHelpText() {
        return "HotRod map storage";
    }
}

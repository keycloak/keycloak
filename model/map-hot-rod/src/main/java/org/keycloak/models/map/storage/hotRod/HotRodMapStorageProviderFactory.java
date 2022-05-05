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
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.common.Profile;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.models.ActionTokenValueModel;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
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
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.authSession.MapAuthenticationSessionEntity;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity;
import org.keycloak.models.map.clientscope.MapClientScopeEntity;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.events.MapAdminEventEntity;
import org.keycloak.models.map.events.MapAuthEventEntity;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.loginFailure.MapUserLoginFailureEntity;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationExecutionEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationFlowEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticatorConfigEntity;
import org.keycloak.models.map.realm.entity.MapClientInitialAccessEntity;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderMapperEntity;
import org.keycloak.models.map.realm.entity.MapOTPPolicyEntity;
import org.keycloak.models.map.realm.entity.MapRequiredActionProviderEntity;
import org.keycloak.models.map.realm.entity.MapRequiredCredentialEntity;
import org.keycloak.models.map.realm.entity.MapWebAuthnPolicyEntity;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.models.map.storage.hotRod.authSession.HotRodAuthenticationSessionEntityDelegate;
import org.keycloak.models.map.storage.hotRod.authSession.HotRodRootAuthenticationSessionEntity;
import org.keycloak.models.map.storage.hotRod.authSession.HotRodRootAuthenticationSessionEntityDelegate;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodPermissionTicketEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodPermissionTicketEntityDelegate;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodPolicyEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodPolicyEntityDelegate;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodResourceEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodResourceEntityDelegate;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodResourceServerEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodResourceServerEntityDelegate;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodScopeEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodScopeEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDelegate;
import org.keycloak.models.map.storage.hotRod.events.HotRodAdminEventEntity;
import org.keycloak.models.map.storage.hotRod.events.HotRodAdminEventEntityDelegate;
import org.keycloak.models.map.storage.hotRod.events.HotRodAuthEventEntity;
import org.keycloak.models.map.storage.hotRod.events.HotRodAuthEventEntityDelegate;
import org.keycloak.models.map.storage.hotRod.loginFailure.HotRodUserLoginFailureEntity;
import org.keycloak.models.map.storage.hotRod.loginFailure.HotRodUserLoginFailureEntityDelegate;
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
import org.keycloak.models.map.storage.hotRod.realm.HotRodRealmEntity;
import org.keycloak.models.map.storage.hotRod.realm.HotRodRealmEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticationExecutionEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticationFlowEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticatorConfigEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodClientInitialAccessEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodComponentEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodIdentityProviderEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodIdentityProviderMapperEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodOTPPolicyEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodRequiredActionProviderEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodRequiredCredentialEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodWebAuthnPolicyEntityDelegate;
import org.keycloak.models.map.storage.hotRod.singleUseObject.HotRodSingleUseObjectEntity;
import org.keycloak.models.map.storage.hotRod.singleUseObject.HotRodSingleUseObjectEntityDelegate;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserConsentEntityDelegate;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserCredentialEntityDelegate;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserEntity;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserEntityDelegate;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserFederatedIdentityEntityDelegate;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodAuthenticatedClientSessionEntity;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodAuthenticatedClientSessionEntityDelegate;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodUserSessionEntity;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodUserSessionEntityDelegate;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodUserSessionMapStorage;
import org.keycloak.models.map.user.MapUserConsentEntity;
import org.keycloak.models.map.user.MapUserCredentialEntity;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.user.MapUserFederatedIdentityEntity;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HotRodMapStorageProviderFactory implements AmphibianProviderFactory<MapStorageProvider>, MapStorageProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "hotrod";
    private static final Logger LOG = Logger.getLogger(HotRodMapStorageProviderFactory.class);
    private final Map<Class<?>, HotRodMapStorage> storages = new ConcurrentHashMap<>();


    private final static DeepCloner CLONER = new DeepCloner.Builder()
            .constructor(MapRootAuthenticationSessionEntity.class,  HotRodRootAuthenticationSessionEntityDelegate::new)
            .constructor(MapAuthenticationSessionEntity.class,      HotRodAuthenticationSessionEntityDelegate::new)

            .constructor(MapClientEntity.class,                     HotRodClientEntityDelegate::new)
            .constructor(MapProtocolMapperEntity.class,             HotRodProtocolMapperEntityDelegate::new)

            .constructor(MapClientScopeEntity.class,                HotRodClientScopeEntityDelegate::new)

            .constructor(MapGroupEntity.class,                      HotRodGroupEntityDelegate::new)

            .constructor(MapRoleEntity.class,                       HotRodRoleEntityDelegate::new)

            .constructor(MapSingleUseObjectEntity.class,            HotRodSingleUseObjectEntityDelegate::new)

            .constructor(MapUserEntity.class,                       HotRodUserEntityDelegate::new)
            .constructor(MapUserCredentialEntity.class,             HotRodUserCredentialEntityDelegate::new)
            .constructor(MapUserFederatedIdentityEntity.class,      HotRodUserFederatedIdentityEntityDelegate::new)
            .constructor(MapUserConsentEntity.class,                HotRodUserConsentEntityDelegate::new)

            .constructor(MapUserLoginFailureEntity.class,           HotRodUserLoginFailureEntityDelegate::new)

            .constructor(MapRealmEntity.class,                      HotRodRealmEntityDelegate::new)
            .constructor(MapAuthenticationExecutionEntity.class,    HotRodAuthenticationExecutionEntityDelegate::new)
            .constructor(MapAuthenticationFlowEntity.class,         HotRodAuthenticationFlowEntityDelegate::new)
            .constructor(MapAuthenticatorConfigEntity.class,        HotRodAuthenticatorConfigEntityDelegate::new)
            .constructor(MapClientInitialAccessEntity.class,        HotRodClientInitialAccessEntityDelegate::new)
            .constructor(MapComponentEntity.class,                  HotRodComponentEntityDelegate::new)
            .constructor(MapIdentityProviderEntity.class,           HotRodIdentityProviderEntityDelegate::new)
            .constructor(MapIdentityProviderMapperEntity.class,     HotRodIdentityProviderMapperEntityDelegate::new)
            .constructor(MapOTPPolicyEntity.class,                  HotRodOTPPolicyEntityDelegate::new)
            .constructor(MapRequiredActionProviderEntity.class,     HotRodRequiredActionProviderEntityDelegate::new)
            .constructor(MapRequiredCredentialEntity.class,         HotRodRequiredCredentialEntityDelegate::new)
            .constructor(MapWebAuthnPolicyEntity.class,             HotRodWebAuthnPolicyEntityDelegate::new)

            .constructor(MapUserSessionEntity.class,                HotRodUserSessionEntityDelegate::new)
            .constructor(MapAuthenticatedClientSessionEntity.class, HotRodAuthenticatedClientSessionEntityDelegate::new)

            .constructor(MapResourceServerEntity.class,             HotRodResourceServerEntityDelegate::new)
            .constructor(MapResourceEntity.class,                   HotRodResourceEntityDelegate::new)
            .constructor(MapScopeEntity.class,                      HotRodScopeEntityDelegate::new)
            .constructor(MapPolicyEntity.class,                     HotRodPolicyEntityDelegate::new)
            .constructor(MapPermissionTicketEntity.class,           HotRodPermissionTicketEntityDelegate::new)

            .constructor(MapAuthEventEntity.class,                  HotRodAuthEventEntityDelegate::new)
            .constructor(MapAdminEventEntity.class,                 HotRodAdminEventEntityDelegate::new)

            .build();

    public static final Map<Class<?>, HotRodEntityDescriptor<?, ?>> ENTITY_DESCRIPTOR_MAP = new HashMap<>();
    static {
        // Authentication sessions descriptor
        ENTITY_DESCRIPTOR_MAP.put(RootAuthenticationSessionModel.class,
                new HotRodEntityDescriptor<>(RootAuthenticationSessionModel.class,
                        HotRodRootAuthenticationSessionEntity.class,
                        HotRodRootAuthenticationSessionEntityDelegate::new));

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

        // Login failure descriptor
        ENTITY_DESCRIPTOR_MAP.put(UserLoginFailureModel.class,
                new HotRodEntityDescriptor<>(UserLoginFailureModel.class,
                        HotRodUserLoginFailureEntity.class,
                        HotRodUserLoginFailureEntityDelegate::new));

        // Realm descriptor
        ENTITY_DESCRIPTOR_MAP.put(RealmModel.class,
                new HotRodEntityDescriptor<>(RealmModel.class,
                        HotRodRealmEntity.class,
                        HotRodRealmEntityDelegate::new));

        // single-use object storage descriptor
        ENTITY_DESCRIPTOR_MAP.put(ActionTokenValueModel.class,
                new HotRodEntityDescriptor<>(ActionTokenValueModel.class,
                        HotRodSingleUseObjectEntity.class,
                        HotRodSingleUseObjectEntityDelegate::new));

       // User sessions descriptor
        ENTITY_DESCRIPTOR_MAP.put(UserSessionModel.class,
                new HotRodEntityDescriptor<>(UserSessionModel.class,
                        HotRodUserSessionEntity.class,
                        HotRodUserSessionEntityDelegate::new));

        // Client sessions descriptor
        ENTITY_DESCRIPTOR_MAP.put(AuthenticatedClientSessionModel.class,
                new HotRodEntityDescriptor<>(AuthenticatedClientSessionModel.class,
                        HotRodAuthenticatedClientSessionEntity.class,
                        HotRodAuthenticatedClientSessionEntityDelegate::new));

        // authz
        ENTITY_DESCRIPTOR_MAP.put(ResourceServer.class,
                new HotRodEntityDescriptor<HotRodResourceServerEntity, HotRodResourceServerEntityDelegate>(ResourceServer.class,
                        HotRodResourceServerEntity.class,
                        HotRodResourceServerEntityDelegate::new) {
                    @Override
                    public String getCacheName() {
                        return "authz";
                    }
                });

        ENTITY_DESCRIPTOR_MAP.put(Resource.class,
                new HotRodEntityDescriptor<HotRodResourceEntity, HotRodResourceEntityDelegate>(Resource.class,
                        HotRodResourceEntity.class,
                        HotRodResourceEntityDelegate::new){
                    @Override
                    public String getCacheName() {
                        return "authz";
                    }
                });

        ENTITY_DESCRIPTOR_MAP.put(Scope.class,
                new HotRodEntityDescriptor<HotRodScopeEntity, HotRodScopeEntityDelegate>(Scope.class,
                        HotRodScopeEntity.class,
                        HotRodScopeEntityDelegate::new){
                    @Override
                    public String getCacheName() {
                        return "authz";
                    }
                });

        ENTITY_DESCRIPTOR_MAP.put(Policy.class,
                new HotRodEntityDescriptor<HotRodPolicyEntity, HotRodPolicyEntityDelegate>(Policy.class,
                        HotRodPolicyEntity.class,
                        HotRodPolicyEntityDelegate::new){
                    @Override
                    public String getCacheName() {
                        return "authz";
                    }
                });

        ENTITY_DESCRIPTOR_MAP.put(PermissionTicket.class,
                new HotRodEntityDescriptor<HotRodPermissionTicketEntity, HotRodPermissionTicketEntityDelegate>(PermissionTicket.class,
                        HotRodPermissionTicketEntity.class,
                        HotRodPermissionTicketEntityDelegate::new){
                    @Override
                    public String getCacheName() {
                        return "authz";
                    }
                });

        // Events
        ENTITY_DESCRIPTOR_MAP.put(Event.class,
                new HotRodEntityDescriptor<>(Event.class,
                        HotRodAuthEventEntity.class,
                        HotRodAuthEventEntityDelegate::new));

        ENTITY_DESCRIPTOR_MAP.put(AdminEvent.class,
                new HotRodEntityDescriptor<>(AdminEvent.class,
                        HotRodAdminEventEntity.class,
                        HotRodAdminEventEntityDelegate::new));
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        return new HotRodMapStorageProvider(session, this);
    }

    public HotRodEntityDescriptor<?, ?> getEntityDescriptor(Class<?> c) {
        return ENTITY_DESCRIPTOR_MAP.get(c);
    }

    public <E extends AbstractHotRodEntity, V extends HotRodEntityDelegate<E> & AbstractEntity, M> HotRodMapStorage<String, E, V, M> getHotRodStorage(KeycloakSession session, Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        if (modelType == UserSessionModel.class) getHotRodStorage(session, AuthenticatedClientSessionModel.class, flags);
        return storages.computeIfAbsent(modelType, c -> createHotRodStorage(session, modelType, flags));
    }

    private <E extends AbstractHotRodEntity, V extends HotRodEntityDelegate<E> & AbstractEntity, M> HotRodMapStorage<String, E, V, M> createHotRodStorage(KeycloakSession session, Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        HotRodConnectionProvider connectionProvider = session.getProvider(HotRodConnectionProvider.class);
        HotRodEntityDescriptor<E, V> entityDescriptor = (HotRodEntityDescriptor<E, V>) getEntityDescriptor(modelType);

        if (modelType == UserSessionModel.class) {
            HotRodMapStorage clientSessionStore = getHotRodStorage(session, AuthenticatedClientSessionModel.class);
            return new HotRodUserSessionMapStorage(clientSessionStore, connectionProvider.getRemoteCache(entityDescriptor.getCacheName()), StringKeyConverter.StringKey.INSTANCE, entityDescriptor, CLONER);

        } else if (modelType == ActionTokenValueModel.class) {
            return new SingleUseObjectHotRodMapStorage(connectionProvider.getRemoteCache(entityDescriptor.getCacheName()), StringKeyConverter.StringKey.INSTANCE, entityDescriptor, CLONER);
        }
        return new HotRodMapStorage<>(connectionProvider.getRemoteCache(entityDescriptor.getCacheName()), StringKeyConverter.StringKey.INSTANCE, entityDescriptor, CLONER);
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

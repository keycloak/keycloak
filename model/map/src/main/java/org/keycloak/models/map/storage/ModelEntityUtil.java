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
package org.keycloak.models.map.storage;

import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.ActionTokenValueModel;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.clientscope.MapClientScopeEntity;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.events.MapAdminEventEntity;
import org.keycloak.models.map.events.MapAuthEventEntity;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.loginFailure.MapUserLoginFailureEntity;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class covering various aspects of relationship between model and entity classes.
 * @author hmlnarik
 */
public class ModelEntityUtil {

    private static final Map<Class<?>, String> MODEL_TO_NAME = new HashMap<>();
    static {
        MODEL_TO_NAME.put(ActionTokenValueModel.class, "single-use-objects");
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

        // events
        MODEL_TO_NAME.put(AdminEvent.class, "admin-events");
        MODEL_TO_NAME.put(Event.class, "auth-events");
    }
    private static final Map<String, Class<?>> NAME_TO_MODEL = MODEL_TO_NAME.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));

    private static final Map<Class<?>, Class<? extends AbstractEntity>> MODEL_TO_ENTITY_TYPE = new HashMap<>();
    static {
        MODEL_TO_ENTITY_TYPE.put(ActionTokenValueModel.class, MapSingleUseObjectEntity.class);
        MODEL_TO_ENTITY_TYPE.put(ClientScopeModel.class, MapClientScopeEntity.class);
        MODEL_TO_ENTITY_TYPE.put(ClientModel.class, MapClientEntity.class);
        MODEL_TO_ENTITY_TYPE.put(GroupModel.class, MapGroupEntity.class);
        MODEL_TO_ENTITY_TYPE.put(RealmModel.class, MapRealmEntity.class);
        MODEL_TO_ENTITY_TYPE.put(RoleModel.class, MapRoleEntity.class);
        MODEL_TO_ENTITY_TYPE.put(RootAuthenticationSessionModel.class, MapRootAuthenticationSessionEntity.class);
        MODEL_TO_ENTITY_TYPE.put(UserLoginFailureModel.class, MapUserLoginFailureEntity.class);
        MODEL_TO_ENTITY_TYPE.put(UserModel.class, MapUserEntity.class);
        MODEL_TO_ENTITY_TYPE.put(UserSessionModel.class, MapUserSessionEntity.class);
        MODEL_TO_ENTITY_TYPE.put(AuthenticatedClientSessionModel.class, MapAuthenticatedClientSessionEntity.class);

        // authz
        MODEL_TO_ENTITY_TYPE.put(PermissionTicket.class, MapPermissionTicketEntity.class);
        MODEL_TO_ENTITY_TYPE.put(Policy.class, MapPolicyEntity.class);
        MODEL_TO_ENTITY_TYPE.put(ResourceServer.class, MapResourceServerEntity.class);
        MODEL_TO_ENTITY_TYPE.put(Resource.class, MapResourceEntity.class);
        MODEL_TO_ENTITY_TYPE.put(org.keycloak.authorization.model.Scope.class, MapScopeEntity.class);

        // events
        MODEL_TO_ENTITY_TYPE.put(AdminEvent.class, MapAdminEventEntity.class);
        MODEL_TO_ENTITY_TYPE.put(Event.class, MapAuthEventEntity.class);
    }
    private static final Map<Class<?>, Class<?>> ENTITY_TO_MODEL_TYPE = MODEL_TO_ENTITY_TYPE.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> Class<V> getEntityType(Class<M> modelClass) {
        return (Class<V>) MODEL_TO_ENTITY_TYPE.get(modelClass);
    }

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> Class<V> getEntityType(Class<M> modelClass, Class<? extends AbstractEntity> defaultClass) {
        return (Class<V>) MODEL_TO_ENTITY_TYPE.getOrDefault(modelClass, defaultClass);
    }

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> Class<M> getModelType(Class<V> entityClass) {
        return (Class<M>) ENTITY_TO_MODEL_TYPE.get(entityClass);
    }

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> Class<M> getModelType(Class<V> entityClass, Class<M> defaultClass) {
        return (Class<M>) ENTITY_TO_MODEL_TYPE.getOrDefault(entityClass, defaultClass);
    }

    public static String getModelName(Class<?> key, String defaultValue) {
        return MODEL_TO_NAME.getOrDefault(key, defaultValue);
    }

    public static String getModelName(Class<?> key) {
        return MODEL_TO_NAME.get(key);
    }

    public static Set<String> getModelNames() {
        return NAME_TO_MODEL.keySet();
    }

    @SuppressWarnings("unchecked")
    public static <M> Class<M> getModelClass(String key) {
        return (Class<M>) NAME_TO_MODEL.get(key);
    }



}

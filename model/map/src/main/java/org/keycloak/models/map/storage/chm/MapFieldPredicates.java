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
package org.keycloak.models.map.storage.chm;

import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
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
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.user.MapUserConsentEntity;
import org.keycloak.storage.SearchableModelField;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.storage.StorageId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.keycloak.models.map.storage.CriterionNotSupportedException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import static org.keycloak.models.UserSessionModel.CORRESPONDING_SESSION_ID;

/**
 *
 * @author hmlnarik
 */
public class MapFieldPredicates {

    public static final Map<SearchableModelField<ClientModel>, UpdatePredicatesFunc<Object, MapClientEntity, ClientModel>> CLIENT_PREDICATES = basePredicates(ClientModel.SearchableFields.ID);
    public static final Map<SearchableModelField<ClientScopeModel>, UpdatePredicatesFunc<Object, MapClientScopeEntity, ClientScopeModel>> CLIENT_SCOPE_PREDICATES = basePredicates(ClientScopeModel.SearchableFields.ID);
    public static final Map<SearchableModelField<GroupModel>, UpdatePredicatesFunc<Object, MapGroupEntity, GroupModel>> GROUP_PREDICATES = basePredicates(GroupModel.SearchableFields.ID);
    public static final Map<SearchableModelField<RoleModel>, UpdatePredicatesFunc<Object, MapRoleEntity, RoleModel>> ROLE_PREDICATES = basePredicates(RoleModel.SearchableFields.ID);
    public static final Map<SearchableModelField<RootAuthenticationSessionModel>, UpdatePredicatesFunc<Object, MapRootAuthenticationSessionEntity, RootAuthenticationSessionModel>> AUTHENTICATION_SESSION_PREDICATES = basePredicates(RootAuthenticationSessionModel.SearchableFields.ID);
    public static final Map<SearchableModelField<RealmModel>, UpdatePredicatesFunc<Object, MapRealmEntity, RealmModel>> REALM_PREDICATES = basePredicates(RealmModel.SearchableFields.ID);
    public static final Map<SearchableModelField<ResourceServer>, UpdatePredicatesFunc<Object, MapResourceServerEntity, ResourceServer>> AUTHZ_RESOURCE_SERVER_PREDICATES = basePredicates(ResourceServer.SearchableFields.ID);
    public static final Map<SearchableModelField<Resource>, UpdatePredicatesFunc<Object, MapResourceEntity, Resource>> AUTHZ_RESOURCE_PREDICATES = basePredicates(Resource.SearchableFields.ID);
    public static final Map<SearchableModelField<Scope>, UpdatePredicatesFunc<Object, MapScopeEntity, Scope>> AUTHZ_SCOPE_PREDICATES = basePredicates(Scope.SearchableFields.ID);
    public static final Map<SearchableModelField<PermissionTicket>, UpdatePredicatesFunc<Object, MapPermissionTicketEntity, PermissionTicket>> AUTHZ_PERMISSION_TICKET_PREDICATES = basePredicates(PermissionTicket.SearchableFields.ID);
    public static final Map<SearchableModelField<Policy>, UpdatePredicatesFunc<Object, MapPolicyEntity, Policy>> AUTHZ_POLICY_PREDICATES = basePredicates(Policy.SearchableFields.ID);
    public static final Map<SearchableModelField<UserLoginFailureModel>, UpdatePredicatesFunc<Object, MapUserLoginFailureEntity, UserLoginFailureModel>> USER_LOGIN_FAILURE_PREDICATES = basePredicates(UserLoginFailureModel.SearchableFields.ID);
    public static final Map<SearchableModelField<UserModel>, UpdatePredicatesFunc<Object, MapUserEntity, UserModel>> USER_PREDICATES = basePredicates(UserModel.SearchableFields.ID);
    public static final Map<SearchableModelField<UserSessionModel>, UpdatePredicatesFunc<Object, MapUserSessionEntity, UserSessionModel>> USER_SESSION_PREDICATES = basePredicates(UserSessionModel.SearchableFields.ID);
    public static final Map<SearchableModelField<Event>, UpdatePredicatesFunc<Object, MapAuthEventEntity, Event>> AUTH_EVENTS_PREDICATES = basePredicates(Event.SearchableFields.ID);
    public static final Map<SearchableModelField<AdminEvent>, UpdatePredicatesFunc<Object, MapAdminEventEntity, AdminEvent>> ADMIN_EVENTS_PREDICATES = basePredicates(AdminEvent.SearchableFields.ID);
    public static final Map<SearchableModelField<ActionTokenValueModel>, UpdatePredicatesFunc<Object, MapSingleUseObjectEntity, ActionTokenValueModel>> ACTION_TOKEN_PREDICATES = basePredicates(ActionTokenValueModel.SearchableFields.ID);

    @SuppressWarnings("unchecked")
    private static final Map<Class<?>, Map> PREDICATES = new HashMap<>();
    private static final Map<SearchableModelField<?>, Comparator<?>> COMPARATORS = new IdentityHashMap<>();

    static {
        put(REALM_PREDICATES, RealmModel.SearchableFields.NAME,                   MapRealmEntity::getName);
        putIncomparable(REALM_PREDICATES, RealmModel.SearchableFields.CLIENT_INITIAL_ACCESS,  MapRealmEntity::getClientInitialAccesses);
        put(REALM_PREDICATES, RealmModel.SearchableFields.COMPONENT_PROVIDER_TYPE, MapFieldPredicates::checkRealmsWithComponentType);

        put(CLIENT_PREDICATES, ClientModel.SearchableFields.REALM_ID,             MapClientEntity::getRealmId);
        put(CLIENT_PREDICATES, ClientModel.SearchableFields.CLIENT_ID,            MapClientEntity::getClientId);
        put(CLIENT_PREDICATES, ClientModel.SearchableFields.SCOPE_MAPPING_ROLE,   MapFieldPredicates::checkScopeMappingRole);
        put(CLIENT_PREDICATES, ClientModel.SearchableFields.ENABLED,              MapClientEntity::isEnabled);
        put(CLIENT_PREDICATES, ClientModel.SearchableFields.ALWAYS_DISPLAY_IN_CONSOLE, MapClientEntity::isAlwaysDisplayInConsole);
        put(CLIENT_PREDICATES, ClientModel.SearchableFields.ATTRIBUTE,            MapFieldPredicates::checkClientAttributes);

        put(CLIENT_SCOPE_PREDICATES, ClientScopeModel.SearchableFields.REALM_ID,  MapClientScopeEntity::getRealmId);
        put(CLIENT_SCOPE_PREDICATES, ClientScopeModel.SearchableFields.NAME,      MapClientScopeEntity::getName);

        put(GROUP_PREDICATES, GroupModel.SearchableFields.REALM_ID,               MapGroupEntity::getRealmId);
        put(GROUP_PREDICATES, GroupModel.SearchableFields.NAME,                   MapGroupEntity::getName);
        put(GROUP_PREDICATES, GroupModel.SearchableFields.PARENT_ID,              MapGroupEntity::getParentId);
        put(GROUP_PREDICATES, GroupModel.SearchableFields.ASSIGNED_ROLE,          MapFieldPredicates::checkGrantedGroupRole);

        put(ROLE_PREDICATES, RoleModel.SearchableFields.REALM_ID,                 MapRoleEntity::getRealmId);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.CLIENT_ID,                MapRoleEntity::getClientId);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.DESCRIPTION,              MapRoleEntity::getDescription);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.NAME,                     MapRoleEntity::getName);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.IS_CLIENT_ROLE,           MapRoleEntity::isClientRole);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.COMPOSITE_ROLE,           MapFieldPredicates::checkCompositeRoles);

        put(USER_PREDICATES, UserModel.SearchableFields.REALM_ID,                 MapUserEntity::getRealmId);
        put(USER_PREDICATES, UserModel.SearchableFields.USERNAME,                 MapUserEntity::getUsername);
        put(USER_PREDICATES, UserModel.SearchableFields.FIRST_NAME,               MapUserEntity::getFirstName);
        put(USER_PREDICATES, UserModel.SearchableFields.LAST_NAME,                MapUserEntity::getLastName);
        put(USER_PREDICATES, UserModel.SearchableFields.EMAIL,                    MapUserEntity::getEmail);
        put(USER_PREDICATES, UserModel.SearchableFields.ENABLED,                  MapUserEntity::isEnabled);
        put(USER_PREDICATES, UserModel.SearchableFields.EMAIL_VERIFIED,           MapUserEntity::isEmailVerified);
        put(USER_PREDICATES, UserModel.SearchableFields.FEDERATION_LINK,          MapUserEntity::getFederationLink);
        put(USER_PREDICATES, UserModel.SearchableFields.ATTRIBUTE,                MapFieldPredicates::checkUserAttributes);
        put(USER_PREDICATES, UserModel.SearchableFields.IDP_AND_USER,             MapFieldPredicates::getUserIdpAliasAtIdentityProviderPredicate);
        put(USER_PREDICATES, UserModel.SearchableFields.ASSIGNED_ROLE,            MapFieldPredicates::checkGrantedUserRole);
        put(USER_PREDICATES, UserModel.SearchableFields.ASSIGNED_GROUP,           MapFieldPredicates::checkUserGroup);
        put(USER_PREDICATES, UserModel.SearchableFields.CONSENT_FOR_CLIENT,       MapFieldPredicates::checkUserClientConsent);
        put(USER_PREDICATES, UserModel.SearchableFields.CONSENT_WITH_CLIENT_SCOPE, MapFieldPredicates::checkUserConsentsWithClientScope);
        put(USER_PREDICATES, UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, MapFieldPredicates::getUserConsentClientFederationLink);
        put(USER_PREDICATES, UserModel.SearchableFields.SERVICE_ACCOUNT_CLIENT,   MapUserEntity::getServiceAccountClientLink);

        put(AUTHENTICATION_SESSION_PREDICATES, RootAuthenticationSessionModel.SearchableFields.REALM_ID,    MapRootAuthenticationSessionEntity::getRealmId);

        put(AUTHZ_RESOURCE_SERVER_PREDICATES, ResourceServer.SearchableFields.ID, predicateForKeyField(MapResourceServerEntity::getId));
        put(AUTHZ_RESOURCE_SERVER_PREDICATES, ResourceServer.SearchableFields.CLIENT_ID, MapResourceServerEntity::getClientId);
        put(AUTHZ_RESOURCE_SERVER_PREDICATES, ResourceServer.SearchableFields.REALM_ID, MapResourceServerEntity::getRealmId);

        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.ID, predicateForKeyField(MapResourceEntity::getId));
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.NAME, MapResourceEntity::getName);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.RESOURCE_SERVER_ID, MapResourceEntity::getResourceServerId);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.OWNER, MapResourceEntity::getOwner);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.TYPE, MapResourceEntity::getType);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.URI, MapFieldPredicates::checkResourceUri);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.SCOPE_ID, MapFieldPredicates::checkResourceScopes);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.OWNER_MANAGED_ACCESS, MapResourceEntity::isOwnerManagedAccess);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.REALM_ID, MapResourceEntity::getRealmId);

        put(AUTHZ_SCOPE_PREDICATES, Scope.SearchableFields.ID, predicateForKeyField(MapScopeEntity::getId));
        put(AUTHZ_SCOPE_PREDICATES, Scope.SearchableFields.RESOURCE_SERVER_ID, MapScopeEntity::getResourceServerId);
        put(AUTHZ_SCOPE_PREDICATES, Scope.SearchableFields.NAME, MapScopeEntity::getName);
        put(AUTHZ_SCOPE_PREDICATES, Scope.SearchableFields.REALM_ID, MapScopeEntity::getRealmId);

        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.ID, predicateForKeyField(MapPermissionTicketEntity::getId));
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.OWNER, MapPermissionTicketEntity::getOwner);
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.REQUESTER, MapPermissionTicketEntity::getRequester);
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.RESOURCE_SERVER_ID, MapPermissionTicketEntity::getResourceServerId);
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.RESOURCE_ID, predicateForKeyField(MapPermissionTicketEntity::getResourceId));
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.SCOPE_ID, predicateForKeyField(MapPermissionTicketEntity::getScopeId));
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.POLICY_ID, predicateForKeyField(MapPermissionTicketEntity::getPolicyId));
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.GRANTED_TIMESTAMP, MapPermissionTicketEntity::getGrantedTimestamp);
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.REALM_ID, MapPermissionTicketEntity::getRealmId);

        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.ID, predicateForKeyField(MapPolicyEntity::getId));
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.NAME, MapPolicyEntity::getName);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.OWNER, MapPolicyEntity::getOwner);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.TYPE, MapPolicyEntity::getType);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.RESOURCE_SERVER_ID, MapPolicyEntity::getResourceServerId);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.REALM_ID, MapPolicyEntity::getRealmId);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.RESOURCE_ID, MapFieldPredicates::checkPolicyResources);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.SCOPE_ID, MapFieldPredicates::checkPolicyScopes);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.CONFIG, MapFieldPredicates::checkPolicyConfig);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.ASSOCIATED_POLICY_ID, MapFieldPredicates::checkAssociatedPolicy);

        put(USER_SESSION_PREDICATES, UserSessionModel.SearchableFields.CORRESPONDING_SESSION_ID,  use -> use.getNote(CORRESPONDING_SESSION_ID));
        put(USER_SESSION_PREDICATES, UserSessionModel.SearchableFields.REALM_ID,                  MapUserSessionEntity::getRealmId);
        put(USER_SESSION_PREDICATES, UserSessionModel.SearchableFields.USER_ID,                   MapUserSessionEntity::getUserId);
        put(USER_SESSION_PREDICATES, UserSessionModel.SearchableFields.CLIENT_ID,                 MapFieldPredicates::checkUserSessionContainsAuthenticatedClientSession);
        put(USER_SESSION_PREDICATES, UserSessionModel.SearchableFields.BROKER_SESSION_ID,         MapUserSessionEntity::getBrokerSessionId);
        put(USER_SESSION_PREDICATES, UserSessionModel.SearchableFields.BROKER_USER_ID,            MapUserSessionEntity::getBrokerUserId);
        put(USER_SESSION_PREDICATES, UserSessionModel.SearchableFields.IS_OFFLINE,                MapUserSessionEntity::isOffline);
        put(USER_SESSION_PREDICATES, UserSessionModel.SearchableFields.LAST_SESSION_REFRESH,      MapUserSessionEntity::getLastSessionRefresh);

        put(USER_LOGIN_FAILURE_PREDICATES, UserLoginFailureModel.SearchableFields.REALM_ID,  MapUserLoginFailureEntity::getRealmId);
        put(USER_LOGIN_FAILURE_PREDICATES, UserLoginFailureModel.SearchableFields.USER_ID,   MapUserLoginFailureEntity::getUserId);

        put(AUTH_EVENTS_PREDICATES, Event.SearchableFields.REALM_ID, MapAuthEventEntity::getRealmId);
        put(AUTH_EVENTS_PREDICATES, Event.SearchableFields.CLIENT_ID, MapAuthEventEntity::getClientId);
        put(AUTH_EVENTS_PREDICATES, Event.SearchableFields.USER_ID, MapAuthEventEntity::getUserId);
        put(AUTH_EVENTS_PREDICATES, Event.SearchableFields.TIMESTAMP, MapAuthEventEntity::getTimestamp);
        put(AUTH_EVENTS_PREDICATES, Event.SearchableFields.IP_ADDRESS, MapAuthEventEntity::getIpAddress);
        put(AUTH_EVENTS_PREDICATES, Event.SearchableFields.EVENT_TYPE, MapAuthEventEntity::getType);

        put(ADMIN_EVENTS_PREDICATES, AdminEvent.SearchableFields.REALM_ID, MapAdminEventEntity::getRealmId);
        put(ADMIN_EVENTS_PREDICATES, AdminEvent.SearchableFields.TIMESTAMP, MapAdminEventEntity::getTimestamp);
        put(ADMIN_EVENTS_PREDICATES, AdminEvent.SearchableFields.AUTH_REALM_ID, MapAdminEventEntity::getAuthRealmId);
        put(ADMIN_EVENTS_PREDICATES, AdminEvent.SearchableFields.AUTH_CLIENT_ID, MapAdminEventEntity::getAuthClientId);
        put(ADMIN_EVENTS_PREDICATES, AdminEvent.SearchableFields.AUTH_USER_ID, MapAdminEventEntity::getAuthUserId);
        put(ADMIN_EVENTS_PREDICATES, AdminEvent.SearchableFields.AUTH_IP_ADDRESS, MapAdminEventEntity::getAuthIpAddress);
        put(ADMIN_EVENTS_PREDICATES, AdminEvent.SearchableFields.OPERATION_TYPE, MapAdminEventEntity::getOperationType);
        put(ADMIN_EVENTS_PREDICATES, AdminEvent.SearchableFields.RESOURCE_TYPE, MapAdminEventEntity::getResourceType);
        put(ADMIN_EVENTS_PREDICATES, AdminEvent.SearchableFields.RESOURCE_PATH, MapAdminEventEntity::getResourcePath);

        put(ACTION_TOKEN_PREDICATES, ActionTokenValueModel.SearchableFields.USER_ID,                    MapSingleUseObjectEntity::getUserId);
        put(ACTION_TOKEN_PREDICATES, ActionTokenValueModel.SearchableFields.ACTION_ID,                  MapSingleUseObjectEntity::getActionId);
        put(ACTION_TOKEN_PREDICATES, ActionTokenValueModel.SearchableFields.ACTION_VERIFICATION_NONCE,  MapSingleUseObjectEntity::getActionVerificationNonce);
        put(ACTION_TOKEN_PREDICATES, ActionTokenValueModel.SearchableFields.OBJECT_KEY,                 MapSingleUseObjectEntity::getObjectKey);
    }

    static {
        PREDICATES.put(RealmModel.class,                        REALM_PREDICATES);
        PREDICATES.put(ClientModel.class,                       CLIENT_PREDICATES);
        PREDICATES.put(ClientScopeModel.class,                  CLIENT_SCOPE_PREDICATES);
        PREDICATES.put(RoleModel.class,                         ROLE_PREDICATES);
        PREDICATES.put(GroupModel.class,                        GROUP_PREDICATES);
        PREDICATES.put(UserModel.class,                         USER_PREDICATES);
        PREDICATES.put(RootAuthenticationSessionModel.class,    AUTHENTICATION_SESSION_PREDICATES);
        PREDICATES.put(ResourceServer.class,                    AUTHZ_RESOURCE_SERVER_PREDICATES);
        PREDICATES.put(Resource.class,                          AUTHZ_RESOURCE_PREDICATES);
        PREDICATES.put(Scope.class,                             AUTHZ_SCOPE_PREDICATES);
        PREDICATES.put(PermissionTicket.class,                  AUTHZ_PERMISSION_TICKET_PREDICATES);
        PREDICATES.put(Policy.class,                            AUTHZ_POLICY_PREDICATES);
        PREDICATES.put(UserSessionModel.class,                  USER_SESSION_PREDICATES);
        PREDICATES.put(UserLoginFailureModel.class,             USER_LOGIN_FAILURE_PREDICATES);
        PREDICATES.put(Event.class,                             AUTH_EVENTS_PREDICATES);
        PREDICATES.put(AdminEvent.class,                        ADMIN_EVENTS_PREDICATES);
        PREDICATES.put(ActionTokenValueModel.class,             ACTION_TOKEN_PREDICATES);
    }

    private static <K, V extends AbstractEntity, M, L extends Comparable<L>> void put(
      Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> map,
      SearchableModelField<M> field, Function<V, L> extractor) {
        COMPARATORS.put(field, Comparator.comparing(extractor));
        map.put(field, (mcb, op, values) -> mcb.fieldCompare(op, extractor, values));
    }

    private static <K, V extends AbstractEntity, M> void putIncomparable(
            Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> map,
            SearchableModelField<M> field, Function<V, Object> extractor) {
        map.put(field, (mcb, op, values) -> mcb.fieldCompare(op, extractor, values));
    }

    private static <K, V extends AbstractEntity, M> void put(
      Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> map,
      SearchableModelField<M> field, UpdatePredicatesFunc<K, V, M> function) {
        map.put(field, function);
    }

    private static <V extends AbstractEntity> Function<V, String> predicateForKeyField(Function<V, Object> extractor) {
        return entity -> {
            Object o = extractor.apply(entity);
            return o == null ? null : o.toString();
        };
    }

    private static String ensureEqSingleValue(SearchableModelField<?> field, String parameterName, Operator op, Object[] values) throws CriterionNotSupportedException {
        return ensureEqSingleValue(field, parameterName, op, values, String.class);
    }

    private static <T> T ensureEqSingleValue(SearchableModelField<?> field, String parameterName, Operator op, Object[] values, Class<T> expectedType) throws CriterionNotSupportedException {
        if (op != Operator.EQ) {
            throw new CriterionNotSupportedException(field, op);
        }
        if (values == null || values.length != 1) {
            throw new CriterionNotSupportedException(field, op, "Invalid arguments, expected (" + parameterName + "), got: " + Arrays.toString(values));
        }

        final Object ob = values[0];
        if (!expectedType.isAssignableFrom(ob.getClass())) {
            throw new CriterionNotSupportedException(field, op, "Invalid arguments, expected (" + expectedType.getName() + "), got: " + Arrays.toString(values));
        }

        return expectedType.cast(ob);
    }

    private static MapModelCriteriaBuilder<Object, MapClientEntity, ClientModel> checkScopeMappingRole(MapModelCriteriaBuilder<Object, MapClientEntity, ClientModel> mcb, Operator op, Object[] values) {
        String roleIdS = ensureEqSingleValue(ClientModel.SearchableFields.SCOPE_MAPPING_ROLE, "role_id", op, values);
        Function<MapClientEntity, ?> getter;
        getter = ce -> Optional.ofNullable(ce.getScopeMappings()).orElse(Collections.emptyList()).contains(roleIdS);
        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapGroupEntity, GroupModel> checkGrantedGroupRole(MapModelCriteriaBuilder<Object, MapGroupEntity, GroupModel> mcb, Operator op, Object[] values) {
        String roleIdS = ensureEqSingleValue(GroupModel.SearchableFields.ASSIGNED_ROLE, "role_id", op, values);
        Function<MapGroupEntity, ?> getter;
        getter = ge -> Optional.ofNullable(ge.getGrantedRoles()).orElse(Collections.emptySet()).contains(roleIdS);
        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> getUserConsentClientFederationLink(MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> mcb, Operator op, Object[] values) {
        String providerId = ensureEqSingleValue(UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, "provider_id", op, values);
        String providerIdS = new StorageId((String) providerId, "").getId();
        Function<MapUserEntity, ?> getter;
        getter = ue -> Optional.ofNullable(ue.getUserConsents()).orElseGet(Collections::emptySet).stream().map(MapUserConsentEntity::getClientId).anyMatch(v -> v != null && v.startsWith(providerIdS));

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> checkUserAttributes(MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> mcb, Operator op, Object[] values) {
        if (values == null || values.length <= 1) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (attribute_name, ...), got: " + Arrays.toString(values));
        }

        final Object attrName = values[0];
        if (! (attrName instanceof String)) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (String attribute_name), got: " + Arrays.toString(values));
        }
        String attrNameS = (String) attrName;
        Function<MapUserEntity, ?> getter;
        Object[] realValues = new Object[values.length - 1];
        System.arraycopy(values, 1, realValues, 0, values.length - 1);
        Predicate<Object> valueComparator = CriteriaOperator.predicateFor(op, realValues);
        getter = ue -> {
            final List<String> attrs = ue.getAttribute(attrNameS);
            return attrs != null && attrs.stream().anyMatch(valueComparator);
        };

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapClientEntity, ClientModel> checkClientAttributes(MapModelCriteriaBuilder<Object, MapClientEntity, ClientModel> mcb, Operator op, Object[] values) {
        if (values == null || values.length != 2) {
            throw new CriterionNotSupportedException(ClientModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected attribute_name-value pair, got: " + Arrays.toString(values));
        }

        final Object attrName = values[0];
        if (! (attrName instanceof String)) {
            throw new CriterionNotSupportedException(ClientModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (String attribute_name), got: " + Arrays.toString(values));
        }
        String attrNameS = (String) attrName;
        Object[] realValues = new Object[values.length - 1];
        System.arraycopy(values, 1, realValues, 0, values.length - 1);
        Predicate<Object> valueComparator = CriteriaOperator.predicateFor(op, realValues);
        Function<MapClientEntity, ?> getter = ue -> {
            final List<String> attrs = ue.getAttribute(attrNameS);
            return attrs != null && attrs.stream().anyMatch(valueComparator);
        };

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapRoleEntity, RoleModel> checkCompositeRoles(MapModelCriteriaBuilder<Object, MapRoleEntity, RoleModel> mcb, Operator op, Object[] values) {
        String roleIdS = ensureEqSingleValue(RoleModel.SearchableFields.COMPOSITE_ROLE, "composite_role_id", op, values);
        Function<MapRoleEntity, ?> getter;
        getter = re -> Optional.ofNullable(re.getCompositeRoles()).orElseGet(Collections::emptySet).contains(roleIdS);

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> checkGrantedUserRole(MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> mcb, Operator op, Object[] values) {
        String roleIdS = ensureEqSingleValue(UserModel.SearchableFields.ASSIGNED_ROLE, "role_id", op, values);
        Function<MapUserEntity, ?> getter;
        getter = ue -> Optional.ofNullable(ue.getRolesMembership()).orElseGet(Collections::emptySet).contains(roleIdS);

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapResourceEntity, Resource> checkResourceUri(MapModelCriteriaBuilder<Object, MapResourceEntity, Resource> mcb, Operator op, Object[] values) {
        Function<MapResourceEntity, ?> getter;

        if (Operator.EXISTS.equals(op)) {
            getter = re -> re.getUris() != null && !re.getUris().isEmpty();
        } else if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> Optional.ofNullable(re.getUris()).orElseGet(Collections::emptySet).stream().anyMatch(c::contains);
        } else {
            String uri = ensureEqSingleValue(Resource.SearchableFields.URI, "uri", op, values);
            getter = re -> Optional.ofNullable(re.getUris()).orElseGet(Collections::emptySet).contains(uri);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapResourceEntity, Resource> checkResourceScopes(MapModelCriteriaBuilder<Object, MapResourceEntity, Resource> mcb, Operator op, Object[] values) {
        Function<MapResourceEntity, ?> getter;

        if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> Optional.ofNullable(re.getScopeIds()).orElseGet(Collections::emptySet).stream().map(Object::toString).anyMatch(c::contains);
        } else {
            String scope = ensureEqSingleValue(Resource.SearchableFields.URI, "scope_id", op, values);
            getter = re -> Optional.ofNullable(re.getScopeIds()).orElseGet(Collections::emptySet).stream().map(Object::toString).anyMatch(scope::equals);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapPolicyEntity, Policy> checkPolicyResources(MapModelCriteriaBuilder<Object, MapPolicyEntity, Policy> mcb, Operator op, Object[] values) {
        Function<MapPolicyEntity, ?> getter;

        if (op == Operator.NOT_EXISTS) {
            getter = re -> re.getResourceIds() == null || re.getResourceIds().isEmpty();
        } else if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> Optional.ofNullable(re.getResourceIds()).orElseGet(Collections::emptySet).stream().map(Object::toString).anyMatch(c::contains);
        } else {
            String scope = ensureEqSingleValue(Policy.SearchableFields.RESOURCE_ID, "resource_id", op, values, String.class);
            getter = re -> Optional.ofNullable(re.getResourceIds()).orElseGet(Collections::emptySet).stream().map(Object::toString).anyMatch(scope::equals);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapPolicyEntity, Policy> checkPolicyScopes(MapModelCriteriaBuilder<Object, MapPolicyEntity, Policy> mcb, Operator op, Object[] values) {
        Function<MapPolicyEntity, ?> getter;

        if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> Optional.ofNullable(re.getScopeIds()).orElseGet(Collections::emptySet).stream().map(Object::toString).anyMatch(c::contains); // TODO: Use KeyConverter
        } else {
            String scope = ensureEqSingleValue(Policy.SearchableFields.CONFIG, "scope_id", op, values);
            getter = re -> Optional.ofNullable(re.getScopeIds()).orElseGet(Collections::emptySet).stream().map(Object::toString).anyMatch(scope::equals);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapPolicyEntity, Policy> checkPolicyConfig(MapModelCriteriaBuilder<Object, MapPolicyEntity, Policy> mcb, Operator op, Object[] values) {
        Function<MapPolicyEntity, ?> getter;

        final Object attrName = values[0];
        if (!(attrName instanceof String)) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (String attribute_name), got: " + Arrays.toString(values));
        }

        String attrNameS = (String) attrName;
        Object[] realValues = new Object[values.length - 1];
        System.arraycopy(values, 1, realValues, 0, values.length - 1);
        Predicate<Object> valueComparator = CriteriaOperator.predicateFor(op, realValues);
        getter = pe -> {
            final String configValue = pe.getConfig(attrNameS);
            return valueComparator.test(configValue);
        };

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapPolicyEntity, Policy> checkAssociatedPolicy(MapModelCriteriaBuilder<Object, MapPolicyEntity, Policy> mcb, Operator op, Object[] values) {
        Function<MapPolicyEntity, ?> getter;

        if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> Optional.ofNullable(re.getAssociatedPolicyIds()).orElseGet(Collections::emptySet).stream().map(Object::toString).anyMatch(c::contains);
        } else {
            String policyId = ensureEqSingleValue(Policy.SearchableFields.ASSOCIATED_POLICY_ID, "associated_policy_id", op, values);
            getter = re -> Optional.ofNullable(re.getAssociatedPolicyIds()).orElseGet(Collections::emptySet).stream().map(Object::toString).anyMatch(policyId::equals);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> checkUserGroup(MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> mcb, Operator op, Object[] values) {
        Function<MapUserEntity, ?> getter;
        if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = ue -> Optional.ofNullable(ue.getGroupsMembership()).orElseGet(Collections::emptySet).stream().anyMatch(c::contains);
        } else {
            String groupIdS = ensureEqSingleValue(UserModel.SearchableFields.ASSIGNED_GROUP, "group_id", op, values);
            getter = ue -> Optional.ofNullable(ue.getGroupsMembership()).orElseGet(Collections::emptySet).contains(groupIdS);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> checkUserClientConsent(MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> mcb, Operator op, Object[] values) {
        String clientIdS = ensureEqSingleValue(UserModel.SearchableFields.CONSENT_FOR_CLIENT, "client_id", op, values);
        Function<MapUserEntity, ?> getter;
        getter = ue -> ue.getUserConsent(clientIdS).orElse(null);

        return mcb.fieldCompare(Operator.EXISTS, getter, null);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> checkUserConsentsWithClientScope(MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> mcb, Operator op, Object[] values) {
        String clientScopeIdS = ensureEqSingleValue(UserModel.SearchableFields.CONSENT_FOR_CLIENT, "client_scope_id", op, values);
        Function<MapUserEntity, ?> getter;
        getter = ue -> Optional.ofNullable(ue.getUserConsents()).orElseGet(Collections::emptySet).stream().anyMatch(consent -> Optional.ofNullable(consent.getGrantedClientScopesIds()).orElseGet(Collections::emptySet).contains(clientScopeIdS));

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> getUserIdpAliasAtIdentityProviderPredicate(MapModelCriteriaBuilder<Object, MapUserEntity, UserModel> mcb, Operator op, Object[] values) {
        if (op != Operator.EQ) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.IDP_AND_USER, op);
        }
        if (values == null || values.length == 0 || values.length > 2) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.IDP_AND_USER, op, "Invalid arguments, expected (idp_alias) or (idp_alias, idp_user), got: " + Arrays.toString(values));
        }

        final Object idpAlias = values[0];
        Function<MapUserEntity, ?> getter;
        if (values.length == 1) {
            getter = ue -> Optional.ofNullable(ue.getFederatedIdentities()).orElseGet(Collections::emptySet).stream()
              .anyMatch(aue -> Objects.equals(idpAlias, aue.getIdentityProvider()));
        } else if (idpAlias == null) {
            final Object idpUserId = values[1];
            getter = ue -> Optional.ofNullable(ue.getFederatedIdentities()).orElseGet(Collections::emptySet).stream()
              .anyMatch(aue -> Objects.equals(idpUserId, aue.getUserId()));
        } else {
            final Object idpUserId = values[1];
            getter = ue -> Optional.ofNullable(ue.getFederatedIdentities()).orElseGet(Collections::emptySet).stream()
              .anyMatch(aue -> Objects.equals(idpAlias, aue.getIdentityProvider()) && Objects.equals(idpUserId, aue.getUserId()));
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapRealmEntity, RealmModel> checkRealmsWithComponentType(MapModelCriteriaBuilder<Object, MapRealmEntity, RealmModel> mcb, Operator op, Object[] values) {
        String providerType = ensureEqSingleValue(RealmModel.SearchableFields.COMPONENT_PROVIDER_TYPE, "component_provider_type", op, values);
        Function<MapRealmEntity, ?> getter = realmEntity -> Optional.ofNullable(realmEntity.getComponents()).orElseGet(Collections::emptySet).stream().anyMatch(component -> component.getProviderType().equals(providerType));
        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserSessionEntity, UserSessionModel> checkUserSessionContainsAuthenticatedClientSession(MapModelCriteriaBuilder<Object, MapUserSessionEntity, UserSessionModel> mcb, Operator op, Object[] values) {
        String clientId = ensureEqSingleValue(UserSessionModel.SearchableFields.CLIENT_ID, "client_id", op, values);
        Function<MapUserSessionEntity, ?> getter = use -> (use.getAuthenticatedClientSession(clientId).isPresent());
        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    public static <K, V extends AbstractEntity, M> Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> basePredicates(SearchableModelField<M> idField) {
        Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates = new HashMap<>();
        fieldPredicates.put(idField, MapModelCriteriaBuilder::idCompare);
        return fieldPredicates;
    }

    public static <K, V extends AbstractEntity, M> Comparator<V> getComparator(QueryParameters.OrderBy<M> orderBy) {
        SearchableModelField<M> searchableModelField = orderBy.getModelField();
        QueryParameters.Order order = orderBy.getOrder();

        @SuppressWarnings("unchecked")
        Comparator<V> comparator = (Comparator<V>) COMPARATORS.get(searchableModelField);

        if (comparator == null) {
            throw new IllegalArgumentException("Comparator for field " + searchableModelField.getName() + " is not configured.");
        }

        if (order == QueryParameters.Order.DESCENDING) {
            return comparator.reversed();
        }

        return comparator;
    }

    @SuppressWarnings("unchecked")
    public static <K, V extends AbstractEntity, M> Comparator<V> getComparator(Stream<QueryParameters.OrderBy<M>> ordering) {
        return (Comparator<V>) ordering.map(MapFieldPredicates::getComparator)
                .reduce(Comparator::thenComparing)
                .orElseThrow(() -> new IllegalArgumentException("Cannot create comparator for " + ordering));
    }

    @SuppressWarnings("unchecked")
    public static <K, V extends AbstractEntity, M> Map<SearchableModelField<? super M>, UpdatePredicatesFunc<K, V, M>> getPredicates(Class<M> clazz) {
        return PREDICATES.get(clazz);
    }
}

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
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.authSession.AbstractRootAuthenticationSessionEntity;
import org.keycloak.models.map.authorization.entity.AbstractPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.AbstractPolicyEntity;
import org.keycloak.models.map.authorization.entity.AbstractResourceEntity;
import org.keycloak.models.map.authorization.entity.AbstractResourceServerEntity;
import org.keycloak.models.map.authorization.entity.AbstractScopeEntity;
import org.keycloak.models.map.client.AbstractClientEntity;
import org.keycloak.models.map.clientscope.AbstractClientScopeEntity;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.group.AbstractGroupEntity;
import org.keycloak.models.map.realm.AbstractRealmEntity;
import org.keycloak.models.map.role.AbstractRoleEntity;
import org.keycloak.storage.SearchableModelField;
import java.util.HashMap;
import java.util.Map;
import org.keycloak.models.map.storage.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.user.AbstractUserEntity;
import org.keycloak.models.map.user.UserConsentEntity;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.storage.StorageId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author hmlnarik
 */
public class MapFieldPredicates {

    public static final Map<SearchableModelField<RealmModel>, UpdatePredicatesFunc<Object, AbstractRealmEntity<Object>, RealmModel>> REALM_PREDICATES = basePredicates(RealmModel.SearchableFields.ID);
    public static final Map<SearchableModelField<ClientModel>, UpdatePredicatesFunc<Object, AbstractClientEntity<Object>, ClientModel>> CLIENT_PREDICATES = basePredicates(ClientModel.SearchableFields.ID);
    public static final Map<SearchableModelField<ClientScopeModel>, UpdatePredicatesFunc<Object, AbstractClientScopeEntity<Object>, ClientScopeModel>> CLIENT_SCOPE_PREDICATES = basePredicates(ClientScopeModel.SearchableFields.ID);
    public static final Map<SearchableModelField<GroupModel>, UpdatePredicatesFunc<Object, AbstractGroupEntity<Object>, GroupModel>> GROUP_PREDICATES = basePredicates(GroupModel.SearchableFields.ID);
    public static final Map<SearchableModelField<RoleModel>, UpdatePredicatesFunc<Object, AbstractRoleEntity<Object>, RoleModel>> ROLE_PREDICATES = basePredicates(RoleModel.SearchableFields.ID);
    public static final Map<SearchableModelField<UserModel>, UpdatePredicatesFunc<Object, AbstractUserEntity<Object>, UserModel>> USER_PREDICATES = basePredicates(UserModel.SearchableFields.ID);
    public static final Map<SearchableModelField<RootAuthenticationSessionModel>, UpdatePredicatesFunc<Object, AbstractRootAuthenticationSessionEntity<Object>, RootAuthenticationSessionModel>> AUTHENTICATION_SESSION_PREDICATES = basePredicates(RootAuthenticationSessionModel.SearchableFields.ID);
    public static final Map<SearchableModelField<ResourceServer>, UpdatePredicatesFunc<Object, AbstractResourceServerEntity<Object>, ResourceServer>> AUTHZ_RESOURCE_SERVER_PREDICATES = basePredicates(ResourceServer.SearchableFields.ID);
    public static final Map<SearchableModelField<Resource>, UpdatePredicatesFunc<Object, AbstractResourceEntity<Object>, Resource>> AUTHZ_RESOURCE_PREDICATES = basePredicates(Resource.SearchableFields.ID);
    public static final Map<SearchableModelField<Scope>, UpdatePredicatesFunc<Object, AbstractScopeEntity<Object>, Scope>> AUTHZ_SCOPE_PREDICATES = basePredicates(Scope.SearchableFields.ID);
    public static final Map<SearchableModelField<PermissionTicket>, UpdatePredicatesFunc<Object, AbstractPermissionTicketEntity<Object>, PermissionTicket>> AUTHZ_PERMISSION_TICKET_PREDICATES = basePredicates(PermissionTicket.SearchableFields.ID);
    public static final Map<SearchableModelField<Policy>, UpdatePredicatesFunc<Object, AbstractPolicyEntity<Object>, Policy>> AUTHZ_POLICY_PREDICATES = basePredicates(Policy.SearchableFields.ID);

    @SuppressWarnings("unchecked")
    private static final Map<Class<?>, Map> PREDICATES = new HashMap<>();

    static {
        put(REALM_PREDICATES, RealmModel.SearchableFields.NAME,                   AbstractRealmEntity::getName);
        put(REALM_PREDICATES, RealmModel.SearchableFields.CLIENT_INITIAL_ACCESS,  MapFieldPredicates::checkRealmsWithClientInitialAccess);
        put(REALM_PREDICATES, RealmModel.SearchableFields.COMPONENT_PROVIDER_TYPE, MapFieldPredicates::checkRealmsWithComponentType);

        put(CLIENT_PREDICATES, ClientModel.SearchableFields.REALM_ID,             AbstractClientEntity::getRealmId);
        put(CLIENT_PREDICATES, ClientModel.SearchableFields.CLIENT_ID,            AbstractClientEntity::getClientId);
        put(CLIENT_PREDICATES, ClientModel.SearchableFields.SCOPE_MAPPING_ROLE,   MapFieldPredicates::checkScopeMappingRole);

        put(CLIENT_SCOPE_PREDICATES, ClientScopeModel.SearchableFields.REALM_ID,  AbstractClientScopeEntity::getRealmId);
        put(CLIENT_SCOPE_PREDICATES, ClientScopeModel.SearchableFields.NAME,      AbstractClientScopeEntity::getName);

        put(GROUP_PREDICATES, GroupModel.SearchableFields.REALM_ID,               AbstractGroupEntity::getRealmId);
        put(GROUP_PREDICATES, GroupModel.SearchableFields.NAME,                   AbstractGroupEntity::getName);
        put(GROUP_PREDICATES, GroupModel.SearchableFields.PARENT_ID,              AbstractGroupEntity::getParentId);
        put(GROUP_PREDICATES, GroupModel.SearchableFields.ASSIGNED_ROLE,          MapFieldPredicates::checkGrantedGroupRole);

        put(ROLE_PREDICATES, RoleModel.SearchableFields.REALM_ID,                 AbstractRoleEntity::getRealmId);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.CLIENT_ID,                AbstractRoleEntity::getClientId);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.DESCRIPTION,              AbstractRoleEntity::getDescription);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.NAME,                     AbstractRoleEntity::getName);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.IS_CLIENT_ROLE,           AbstractRoleEntity::isClientRole);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.IS_COMPOSITE_ROLE,        AbstractRoleEntity::isComposite);

        put(USER_PREDICATES, UserModel.SearchableFields.REALM_ID,                 AbstractUserEntity::getRealmId);
        put(USER_PREDICATES, UserModel.SearchableFields.USERNAME,                 AbstractUserEntity::getUsername);
        put(USER_PREDICATES, UserModel.SearchableFields.FIRST_NAME,               AbstractUserEntity::getFirstName);
        put(USER_PREDICATES, UserModel.SearchableFields.LAST_NAME,                AbstractUserEntity::getLastName);
        put(USER_PREDICATES, UserModel.SearchableFields.EMAIL,                    AbstractUserEntity::getEmail);
        put(USER_PREDICATES, UserModel.SearchableFields.ENABLED,                  AbstractUserEntity::isEnabled);
        put(USER_PREDICATES, UserModel.SearchableFields.EMAIL_VERIFIED,           AbstractUserEntity::isEmailVerified);
        put(USER_PREDICATES, UserModel.SearchableFields.FEDERATION_LINK,          AbstractUserEntity::getFederationLink);
        put(USER_PREDICATES, UserModel.SearchableFields.ATTRIBUTE,                MapFieldPredicates::checkUserAttributes);
        put(USER_PREDICATES, UserModel.SearchableFields.IDP_AND_USER,             MapFieldPredicates::getUserIdpAliasAtIdentityProviderPredicate);
        put(USER_PREDICATES, UserModel.SearchableFields.ASSIGNED_ROLE,            MapFieldPredicates::checkGrantedUserRole);
        put(USER_PREDICATES, UserModel.SearchableFields.ASSIGNED_GROUP,           MapFieldPredicates::checkUserGroup);
        put(USER_PREDICATES, UserModel.SearchableFields.CONSENT_FOR_CLIENT,       MapFieldPredicates::checkUserClientConsent);
        put(USER_PREDICATES, UserModel.SearchableFields.CONSENT_WITH_CLIENT_SCOPE, MapFieldPredicates::checkUserConsentsWithClientScope);
        put(USER_PREDICATES, UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, MapFieldPredicates::getUserConsentClientFederationLink);
        put(USER_PREDICATES, UserModel.SearchableFields.SERVICE_ACCOUNT_CLIENT,   AbstractUserEntity::getServiceAccountClientLink);

        put(AUTHENTICATION_SESSION_PREDICATES, RootAuthenticationSessionModel.SearchableFields.REALM_ID,    AbstractRootAuthenticationSessionEntity::getRealmId);
        put(AUTHENTICATION_SESSION_PREDICATES, RootAuthenticationSessionModel.SearchableFields.TIMESTAMP,   AbstractRootAuthenticationSessionEntity::getTimestamp);

        put(AUTHZ_RESOURCE_SERVER_PREDICATES, ResourceServer.SearchableFields.ID, AbstractResourceServerEntity::getId);

        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.ID, predicateForKeyField(AbstractResourceEntity::getId));
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.NAME, AbstractResourceEntity::getName);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.RESOURCE_SERVER_ID, AbstractResourceEntity::getResourceServerId);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.OWNER, AbstractResourceEntity::getOwner);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.TYPE, AbstractResourceEntity::getType);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.URI, MapFieldPredicates::checkResourceUri);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.SCOPE_ID, MapFieldPredicates::checkResourceScopes);
        put(AUTHZ_RESOURCE_PREDICATES, Resource.SearchableFields.OWNER_MANAGED_ACCESS, AbstractResourceEntity::isOwnerManagedAccess);

        put(AUTHZ_SCOPE_PREDICATES, Scope.SearchableFields.ID, predicateForKeyField(AbstractScopeEntity::getId));
        put(AUTHZ_SCOPE_PREDICATES, Scope.SearchableFields.RESOURCE_SERVER_ID, AbstractScopeEntity::getResourceServerId);
        put(AUTHZ_SCOPE_PREDICATES, Scope.SearchableFields.NAME, AbstractScopeEntity::getName);

        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.ID, predicateForKeyField(AbstractPermissionTicketEntity::getId));
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.OWNER, AbstractPermissionTicketEntity::getOwner);
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.REQUESTER, AbstractPermissionTicketEntity::getRequester);
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.RESOURCE_SERVER_ID, AbstractPermissionTicketEntity::getResourceServerId);
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.RESOURCE_ID, predicateForKeyField(AbstractPermissionTicketEntity::getResourceId));
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.SCOPE_ID, predicateForKeyField(AbstractPermissionTicketEntity::getScopeId));
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.POLICY_ID, predicateForKeyField(AbstractPermissionTicketEntity::getPolicyId));
        put(AUTHZ_PERMISSION_TICKET_PREDICATES, PermissionTicket.SearchableFields.GRANTED_TIMESTAMP, AbstractPermissionTicketEntity::getGrantedTimestamp);

        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.ID, predicateForKeyField(AbstractPolicyEntity::getId));
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.NAME, AbstractPolicyEntity::getName);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.OWNER, AbstractPolicyEntity::getOwner);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.TYPE, AbstractPolicyEntity::getType);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.RESOURCE_SERVER_ID, AbstractPolicyEntity::getResourceServerId);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.RESOURCE_ID, MapFieldPredicates::checkPolicyResources);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.SCOPE_ID, MapFieldPredicates::checkPolicyScopes);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.CONFIG, MapFieldPredicates::checkPolicyConfig);
        put(AUTHZ_POLICY_PREDICATES, Policy.SearchableFields.ASSOCIATED_POLICY_ID, MapFieldPredicates::checkAssociatedPolicy);
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
    }

    private static <K, V extends AbstractEntity<K>, M> void put(
      Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> map,
      SearchableModelField<M> field, Function<V, Object> extractor) {
        map.put(field, (mcb, op, values) -> mcb.fieldCompare(op, extractor, values));
    }

    private static <K, V extends AbstractEntity<K>, M> void put(
      Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> map,
      SearchableModelField<M> field, UpdatePredicatesFunc<K, V, M> function) {
        map.put(field, function);
    }
    
    private static <V extends AbstractEntity<?>> Function<V, Object> predicateForKeyField(Function<V, Object> extractor) {
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

    private static MapModelCriteriaBuilder<Object, AbstractClientEntity<Object>, ClientModel> checkScopeMappingRole(MapModelCriteriaBuilder<Object, AbstractClientEntity<Object>, ClientModel> mcb, Operator op, Object[] values) {
        String roleIdS = ensureEqSingleValue(ClientModel.SearchableFields.SCOPE_MAPPING_ROLE, "role_id", op, values);
        Function<AbstractClientEntity<Object>, ?> getter;
        getter = ce -> ce.getScopeMappings().contains(roleIdS);
        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractGroupEntity<Object>, GroupModel> checkGrantedGroupRole(MapModelCriteriaBuilder<Object, AbstractGroupEntity<Object>, GroupModel> mcb, Operator op, Object[] values) {
        String roleIdS = ensureEqSingleValue(GroupModel.SearchableFields.ASSIGNED_ROLE, "role_id", op, values);
        Function<AbstractGroupEntity<Object>, ?> getter;
        getter = ge -> ge.getGrantedRoles().contains(roleIdS);
        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> getUserConsentClientFederationLink(MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        String providerId = ensureEqSingleValue(UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, "provider_id", op, values);
        String providerIdS = new StorageId((String) providerId, "").getId();
        Function<AbstractUserEntity<Object>, ?> getter;
        getter = ue -> ue.getUserConsents().map(UserConsentEntity::getClientId).anyMatch(v -> v != null && v.startsWith(providerIdS));

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> checkUserAttributes(MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        if (values == null || values.length <= 1) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (attribute_name, ...), got: " + Arrays.toString(values));
        }

        final Object attrName = values[0];
        if (! (attrName instanceof String)) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (String attribute_name), got: " + Arrays.toString(values));
        }
        String attrNameS = (String) attrName;
        Function<AbstractUserEntity<Object>, ?> getter;
        Object[] realValues = new Object[values.length - 1];
        System.arraycopy(values, 1, realValues, 0, values.length - 1);
        Predicate<Object> valueComparator = CriteriaOperator.predicateFor(op, realValues);
        getter = ue -> {
            final List<String> attrs = ue.getAttribute(attrNameS);
            return attrs != null && attrs.stream().anyMatch(valueComparator);
        };

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> checkGrantedUserRole(MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        String roleIdS = ensureEqSingleValue(UserModel.SearchableFields.ASSIGNED_ROLE, "role_id", op, values);
        Function<AbstractUserEntity<Object>, ?> getter;
        getter = ue -> ue.getRolesMembership().contains(roleIdS);

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractResourceEntity<Object>, Resource> checkResourceUri(MapModelCriteriaBuilder<Object, AbstractResourceEntity<Object>, Resource> mcb, Operator op, Object[] values) {
        Function<AbstractResourceEntity<Object>, ?> getter;

        if (Operator.EXISTS.equals(op)) {
            getter = re -> re.getUris() != null && !re.getUris().isEmpty();
        } else if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> re.getUris().stream().anyMatch(c::contains);
        } else {
            String uri = ensureEqSingleValue(Resource.SearchableFields.URI, "uri", op, values);
            getter = re -> re.getUris().contains(uri);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractResourceEntity<Object>, Resource> checkResourceScopes(MapModelCriteriaBuilder<Object, AbstractResourceEntity<Object>, Resource> mcb, Operator op, Object[] values) {
        Function<AbstractResourceEntity<Object>, ?> getter;

        if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> re.getScopeIds().stream().map(Object::toString).anyMatch(c::contains);
        } else {
            String scope = ensureEqSingleValue(Resource.SearchableFields.URI, "scope_id", op, values);
            getter = re -> re.getScopeIds().stream().map(Object::toString).anyMatch(scope::equals);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractPolicyEntity<Object>, Policy> checkPolicyResources(MapModelCriteriaBuilder<Object, AbstractPolicyEntity<Object>, Policy> mcb, Operator op, Object[] values) {
        Function<AbstractPolicyEntity<Object>, ?> getter;

        if (op == Operator.NOT_EXISTS) {
            getter = re -> re.getResourceIds().isEmpty();
        } else if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> re.getResourceIds().stream().map(Object::toString).anyMatch(c::contains);
        } else {
            String scope = ensureEqSingleValue(Policy.SearchableFields.RESOURCE_ID, "resource_id", op, values, String.class);
            getter = re -> re.getResourceIds().stream().map(Object::toString).anyMatch(scope::equals);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractPolicyEntity<Object>, Policy> checkPolicyScopes(MapModelCriteriaBuilder<Object, AbstractPolicyEntity<Object>, Policy> mcb, Operator op, Object[] values) {
        Function<AbstractPolicyEntity<Object>, ?> getter;

        if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> re.getScopeIds().stream().map(Object::toString).anyMatch(c::contains); // TODO: Use KeyConverter
        } else {
            String scope = ensureEqSingleValue(Policy.SearchableFields.CONFIG, "scope_id", op, values);
            getter = re -> re.getScopeIds().stream().map(Object::toString).anyMatch(scope::equals);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractPolicyEntity<Object>, Policy> checkPolicyConfig(MapModelCriteriaBuilder<Object, AbstractPolicyEntity<Object>, Policy> mcb, Operator op, Object[] values) {
        Function<AbstractPolicyEntity<Object>, ?> getter;

        final Object attrName = values[0];
        if (!(attrName instanceof String)) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (String attribute_name), got: " + Arrays.toString(values));
        }

        String attrNameS = (String) attrName;
        Object[] realValues = new Object[values.length - 1];
        System.arraycopy(values, 1, realValues, 0, values.length - 1);
        Predicate<Object> valueComparator = CriteriaOperator.predicateFor(op, realValues);
        getter = pe -> {
            final String configValue = pe.getConfigValue(attrNameS);
            return valueComparator.test(configValue);
        };

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractPolicyEntity<Object>, Policy> checkAssociatedPolicy(MapModelCriteriaBuilder<Object, AbstractPolicyEntity<Object>, Policy> mcb, Operator op, Object[] values) {
        Function<AbstractPolicyEntity<Object>, ?> getter;

        if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = re -> re.getAssociatedPoliciesIds().stream().map(Object::toString).anyMatch(c::contains);
        } else {
            String policyId = ensureEqSingleValue(Policy.SearchableFields.ASSOCIATED_POLICY_ID, "associated_policy_id", op, values);
            getter = re -> re.getAssociatedPoliciesIds().stream().map(Object::toString).anyMatch(policyId::equals);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> checkUserGroup(MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        Function<AbstractUserEntity<Object>, ?> getter;
        if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = ue -> ue.getGroupsMembership().stream().anyMatch(c::contains);
        } else {
            String groupIdS = ensureEqSingleValue(UserModel.SearchableFields.ASSIGNED_GROUP, "group_id", op, values);
            getter = ue -> ue.getGroupsMembership().contains(groupIdS);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> checkUserClientConsent(MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        String clientIdS = ensureEqSingleValue(UserModel.SearchableFields.CONSENT_FOR_CLIENT, "client_id", op, values);
        Function<AbstractUserEntity<Object>, ?> getter;
        getter = ue -> ue.getUserConsent(clientIdS);

        return mcb.fieldCompare(Operator.EXISTS, getter, null);
    }

    private static MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> checkUserConsentsWithClientScope(MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        String clientScopeIdS = ensureEqSingleValue(UserModel.SearchableFields.CONSENT_FOR_CLIENT, "client_scope_id", op, values);
        Function<AbstractUserEntity<Object>, ?> getter;
        getter = ue -> ue.getUserConsents().anyMatch(consent -> consent.getGrantedClientScopesIds().contains(clientScopeIdS));

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> getUserIdpAliasAtIdentityProviderPredicate(MapModelCriteriaBuilder<Object, AbstractUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        if (op != Operator.EQ) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.IDP_AND_USER, op);
        }
        if (values == null || values.length == 0 || values.length > 2) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.IDP_AND_USER, op, "Invalid arguments, expected (idp_alias) or (idp_alias, idp_user), got: " + Arrays.toString(values));
        }

        final Object idpAlias = values[0];
        Function<AbstractUserEntity<Object>, ?> getter;
        if (values.length == 1) {
            getter = ue -> ue.getFederatedIdentities()
              .anyMatch(aue -> Objects.equals(idpAlias, aue.getIdentityProvider()));
        } else if (idpAlias == null) {
            final Object idpUserId = values[1];
            getter = ue -> ue.getFederatedIdentities()
              .anyMatch(aue -> Objects.equals(idpUserId, aue.getUserId()));
        } else {
            final Object idpUserId = values[1];
            getter = ue -> ue.getFederatedIdentities()
              .anyMatch(aue -> Objects.equals(idpAlias, aue.getIdentityProvider()) && Objects.equals(idpUserId, aue.getUserId()));
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractRealmEntity<Object>, RealmModel> checkRealmsWithClientInitialAccess(MapModelCriteriaBuilder<Object, AbstractRealmEntity<Object>, RealmModel> mcb, Operator op, Object[] values) {
        if (op != Operator.EXISTS) {
            throw new CriterionNotSupportedException(RealmModel.SearchableFields.CLIENT_INITIAL_ACCESS, op);
        }
        Function<AbstractRealmEntity<Object>, ?> getter = AbstractRealmEntity::hasClientInitialAccess;
        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, AbstractRealmEntity<Object>, RealmModel> checkRealmsWithComponentType(MapModelCriteriaBuilder<Object, AbstractRealmEntity<Object>, RealmModel> mcb, Operator op, Object[] values) {
        String providerType = ensureEqSingleValue(RealmModel.SearchableFields.COMPONENT_PROVIDER_TYPE, "component_provider_type", op, values);
        Function<AbstractRealmEntity<Object>, ?> getter = realmEntity -> realmEntity.getComponents().anyMatch(component -> component.getProviderType().equals(providerType));

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    protected static <K, V extends AbstractEntity<K>, M> Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> basePredicates(SearchableModelField<M> idField) {
        Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates = new HashMap<>();
        fieldPredicates.put(idField, (o, op, values) -> o.idCompare(op, values));
        return fieldPredicates;
    }

    @SuppressWarnings("unchecked")
    public static <K, V extends AbstractEntity<K>, M> Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> getPredicates(Class<M> clazz) {
        return PREDICATES.get(clazz);
    }
}

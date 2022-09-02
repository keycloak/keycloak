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

package org.keycloak.models.map.user;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialAuthentication;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.SearchableFields;
import org.keycloak.models.UserProvider;
import org.keycloak.models.map.common.TimeAdapter;
import org.keycloak.models.map.credential.MapUserCredentialManager;
import org.keycloak.models.map.storage.MapKeycloakTransactionWithAuth;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.UserModel.EMAIL;
import static org.keycloak.models.UserModel.EMAIL_VERIFIED;
import static org.keycloak.models.UserModel.FIRST_NAME;
import static org.keycloak.models.UserModel.LAST_NAME;
import static org.keycloak.models.UserModel.USERNAME;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.USER_AFTER_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.USER_BEFORE_REMOVE;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapUserProvider implements UserProvider.Streams {

    private static final Logger LOG = Logger.getLogger(MapUserProvider.class);
    private final KeycloakSession session;
    final MapKeycloakTransaction<MapUserEntity, UserModel> tx;

    public MapUserProvider(KeycloakSession session, MapStorage<MapUserEntity, UserModel> store) {
        this.session = session;
        this.tx = store.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private Function<MapUserEntity, UserModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapUserAdapter(session, realm, origEntity) {
            @Override
            public boolean checkEmailUniqueness(RealmModel realm, String email) {
                return getUserByEmail(realm, email) != null;
            }

            @Override
            public boolean checkUsernameUniqueness(RealmModel realm, String username) {
                return getUserByUsername(realm, username) != null;
            }

            @Override
            public SubjectCredentialManager credentialManager() {
                return new MapUserCredentialManager(session, realm, this, entity);
            }
        };
    }

    private Predicate<MapUserEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return c -> false;
        }
        String realmId = realm.getId();
        return entity -> entity.getRealmId() == null || Objects.equals(realmId, entity.getRealmId());
    }

    private ModelException userDoesntExistException() {
        return new ModelException("Specified user doesn't exist.");
    }

    private Optional<MapUserEntity> getEntityById(RealmModel realm, String id) {
        try {
            MapUserEntity mapUserEntity = tx.read(id);
            if (mapUserEntity != null && entityRealmFilter(realm).test(mapUserEntity)) {
                return Optional.of(mapUserEntity);
            }

            return Optional.empty();
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private MapUserEntity getEntityByIdOrThrow(RealmModel realm, String id) {
        return getEntityById(realm, id)
                .orElseThrow(this::userDoesntExistException);
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        if (user == null || user.getId() == null) {
            return;
        }
        LOG.tracef("addFederatedIdentity(%s, %s, %s)%s", realm, user.getId(), socialLink.getIdentityProvider(), getShortStackTrace());

        getEntityById(realm, user.getId())
                .ifPresent(userEntity ->
                        userEntity.addFederatedIdentity(MapUserFederatedIdentityEntity.fromModel(socialLink)));
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        LOG.tracef("removeFederatedIdentity(%s, %s, %s)%s", realm, user.getId(), socialProvider, getShortStackTrace());

        Optional<MapUserEntity> entityById = getEntityById(realm, user.getId());
        if (!entityById.isPresent()) return false;

        Boolean result = entityById.get().removeFederatedIdentity(socialProvider);
        return result == null ? true : result; // TODO: make removeFederatedIdentity return Boolean so the caller can correctly handle "I don't know" null answer
    }

    @Override
    public void preRemove(RealmModel realm, IdentityProviderModel provider) {
        String socialProvider = provider.getAlias();
        LOG.tracef("preRemove[RealmModel realm, IdentityProviderModel provider](%s, %s)%s", realm, socialProvider, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.IDP_AND_USER, Operator.EQ, socialProvider);

        tx.read(withCriteria(mcb))
                .forEach(userEntity -> userEntity.removeFederatedIdentity(socialProvider));
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        LOG.tracef("updateFederatedIdentity(%s, %s, %s)%s", realm, federatedUser.getId(), federatedIdentityModel.getIdentityProvider(), getShortStackTrace());
        getEntityById(realm, federatedUser.getId())
                .flatMap(u -> u.getFederatedIdentity(federatedIdentityModel.getIdentityProvider()))
                .ifPresent(fi -> {
                    fi.setUserId(federatedIdentityModel.getUserId());
                    fi.setUserName(federatedIdentityModel.getUserName());
                    fi.setToken(federatedIdentityModel.getToken());
                });
    }

    @Override
    public Stream<FederatedIdentityModel> getFederatedIdentitiesStream(RealmModel realm, UserModel user) {
        LOG.tracef("getFederatedIdentitiesStream(%s, %s)%s", realm, user.getId(), getShortStackTrace());
        return getEntityById(realm, user.getId())
                .map(MapUserEntity::getFederatedIdentities)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(MapUserFederatedIdentityEntity::toModel);
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        LOG.tracef("getFederatedIdentity(%s, %s, %s)%s", realm, user.getId(), socialProvider, getShortStackTrace());
        return getEntityById(realm, user.getId())
                .flatMap(userEntity -> userEntity.getFederatedIdentity(socialProvider))
                .map(MapUserFederatedIdentityEntity::toModel)
                .orElse(null);
    }

    @Override
    public UserModel getUserByFederatedIdentity(RealmModel realm, FederatedIdentityModel socialLink) {
        LOG.tracef("getUserByFederatedIdentity(%s, %s)%s", realm, socialLink, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.IDP_AND_USER, Operator.EQ, socialLink.getIdentityProvider(), socialLink.getUserId());

        return tx.read(withCriteria(mcb))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            if (list.isEmpty()) {
                                return null;
                            } else if (list.size() != 1) {
                                throw new IllegalStateException("More results found for identityProvider=" + socialLink.getIdentityProvider() +
                                        ", userId=" + socialLink.getUserId() + ", results=" + list);
                            }

                            return entityToAdapterFunc(realm).apply(list.get(0));
                        }));
    }

    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        LOG.tracef("addConsent(%s, %s, %s)%s", realm, userId, consent, getShortStackTrace());

        getEntityByIdOrThrow(realm, userId)
                .addUserConsent(MapUserConsentEntity.fromModel(consent));
    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientInternalId) {
        LOG.tracef("getConsentByClient(%s, %s, %s)%s", realm, userId, clientInternalId, getShortStackTrace());
        return getEntityById(realm, userId)
                .flatMap(userEntity -> userEntity.getUserConsent(clientInternalId))
                .map(consent -> MapUserConsentEntity.toModel(realm, consent))
                .orElse(null);
    }

    @Override
    public Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId) {
        LOG.tracef("getConsentByClientStream(%s, %s)%s", realm, userId, getShortStackTrace());
        return getEntityById(realm, userId)
                .map(MapUserEntity::getUserConsents)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(consent -> MapUserConsentEntity.toModel(realm, consent));
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        LOG.tracef("updateConsent(%s, %s, %s)%s", realm, userId, consent, getShortStackTrace());

        MapUserEntity user = getEntityByIdOrThrow(realm, userId);
        MapUserConsentEntity userConsentEntity = user.getUserConsent(consent.getClient().getId())
                .orElseThrow(() -> new ModelException("Consent not found for client [" + consent.getClient().getId() + "] and user [" + userId + "]"));

        userConsentEntity.setGrantedClientScopesIds(
                consent.getGrantedClientScopes().stream()
                        .map(ClientScopeModel::getId)
                        .collect(Collectors.toSet())
        );

        userConsentEntity.setLastUpdatedDate(Time.currentTimeMillis());
    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId) {
        LOG.tracef("revokeConsentForClient(%s, %s, %s)%s", realm, userId, clientInternalId, getShortStackTrace());

        Optional<MapUserEntity> entityById = getEntityById(realm, userId);
        if (!entityById.isPresent()) return false;

        Boolean result = entityById.get().removeUserConsent(clientInternalId);
        return result == null ? true : result; // TODO: make revokeConsentForClient return Boolean so the caller can correctly handle "I don't know" null answer
    }

    @Override
    public void setNotBeforeForUser(RealmModel realm, UserModel user, int notBefore) {
        LOG.tracef("setNotBeforeForUser(%s, %s, %d)%s", realm, user.getId(), notBefore, getShortStackTrace());
        getEntityByIdOrThrow(realm, user.getId()).setNotBefore(TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(notBefore));
    }

    @Override
    public int getNotBeforeOfUser(RealmModel realm, UserModel user) {
        LOG.tracef("getNotBeforeOfUser(%s, %s)%s", realm, user.getId(), getShortStackTrace());
        Long notBefore = getEntityById(realm, user.getId())
                .orElseThrow(this::userDoesntExistException)
                .getNotBefore();

        return notBefore == null ? 0 : TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(notBefore);
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        LOG.tracef("getServiceAccount(%s)%s", client.getId(), getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, client.getRealm().getId())
          .compare(SearchableFields.SERVICE_ACCOUNT_CLIENT, Operator.EQ, client.getId());

        return tx.read(withCriteria(mcb))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            if (list.isEmpty()) {
                                return null;
                            } else if (list.size() != 1) {
                                throw new IllegalStateException("More service account linked users found for client=" + client.getClientId() +
                                        ", results=" + list);
                            }

                            return entityToAdapterFunc(client.getRealm()).apply(list.get(0));
                        }
                ));
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        LOG.tracef("addUser(%s, %s, %s, %s, %s)%s", realm, id, username, addDefaultRoles, addDefaultRequiredActions, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.USERNAME, Operator.EQ, username);

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("User with username '" + username + "' in realm " + realm.getName() + " already exists" );
        }
        
        if (id != null && tx.read(id) != null) {
            throw new ModelDuplicateException("User exists: " + id);
        }

        MapUserEntity entity = new MapUserEntityImpl();
        entity.setId(id);
        entity.setRealmId(realm.getId());
        entity.setEmailConstraint(KeycloakModelUtils.generateId());
        entity.setUsername(username.toLowerCase());
        entity.setCreatedTimestamp(Time.currentTimeMillis());

        entity = tx.create(entity);
        final UserModel userModel = entityToAdapterFunc(realm).apply(entity);

        if (addDefaultRoles) {
            userModel.grantRole(realm.getDefaultRole());

            // No need to check if user has group as it's new user
            realm.getDefaultGroupsStream().forEach(userModel::joinGroup);
        }

        if (addDefaultRequiredActions){
            realm.getRequiredActionProvidersStream()
                    .filter(RequiredActionProviderModel::isEnabled)
                    .filter(RequiredActionProviderModel::isDefaultAction)
                    .map(RequiredActionProviderModel::getAlias)
                    .forEach(userModel::addRequiredAction);
        }

        return userModel;
    }

    @Override
    public void preRemove(RealmModel realm) {
        LOG.tracef("preRemove[RealmModel](%s)%s", realm, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        tx.delete(withCriteria(mcb));
    }

    @Override
    public void removeImportedUsers(RealmModel realm, String storageProviderId) {
        LOG.tracef("removeImportedUsers(%s, %s)%s", realm, storageProviderId, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.FEDERATION_LINK, Operator.EQ, storageProviderId);

        tx.delete(withCriteria(mcb));
    }

    @Override
    public void unlinkUsers(RealmModel realm, String storageProviderId) {
        LOG.tracef("unlinkUsers(%s, %s)%s", realm, storageProviderId, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.FEDERATION_LINK, Operator.EQ, storageProviderId);

        try (Stream<MapUserEntity> s = tx.read(withCriteria(mcb))) {
            s.forEach(userEntity -> userEntity.setFederationLink(null));
        }
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        String roleId = role.getId();
        LOG.tracef("preRemove[RoleModel](%s, %s)%s", realm, roleId, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_ROLE, Operator.EQ, roleId);

        try (Stream<MapUserEntity> s = tx.read(withCriteria(mcb))) {
            s.forEach(userEntity -> userEntity.removeRolesMembership(roleId));
        }
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        String groupId = group.getId();
        LOG.tracef("preRemove[GroupModel](%s, %s)%s", realm, groupId, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_GROUP, Operator.EQ, groupId);

        try (Stream<MapUserEntity> s = tx.read(withCriteria(mcb))) {
            s.forEach(userEntity -> userEntity.removeGroupsMembership(groupId));
        }
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        String clientId = client.getId();
        LOG.tracef("preRemove[ClientModel](%s, %s)%s", realm, clientId, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.CONSENT_FOR_CLIENT, Operator.EQ, clientId);

        try (Stream<MapUserEntity> s = tx.read(withCriteria(mcb))) {
            s.forEach(userEntity -> userEntity.removeUserConsent(clientId));
        }
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        // No-op
    }

    @Override
    public void preRemove(ClientScopeModel clientScope) {
        String clientScopeId = clientScope.getId();
        LOG.tracef("preRemove[ClientScopeModel](%s)%s", clientScopeId, getShortStackTrace());

        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, clientScope.getRealm().getId())
          .compare(SearchableFields.CONSENT_WITH_CLIENT_SCOPE, Operator.EQ, clientScopeId);

        try (Stream<MapUserEntity> s = tx.read(withCriteria(mcb))) {
            s.map(MapUserEntity::getUserConsents)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .forEach(consent -> consent.removeGrantedClientScopesId(clientScopeId));
        }
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        String roleId = role.getId();
        LOG.tracef("grantToAllUsers(%s, %s)%s", realm, roleId, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        try (Stream<MapUserEntity> s = tx.read(withCriteria(mcb))) {
            s.forEach(entity -> entity.addRolesMembership(roleId));
        }
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        LOG.tracef("getUserById(%s, %s)%s", realm, id, getShortStackTrace());
        return getEntityById(realm, id).map(entityToAdapterFunc(realm)).orElse(null);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        if (username == null) return null;
        LOG.tracef("getUserByUsername(%s, %s)%s", realm, username, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.USERNAME, Operator.ILIKE, username);

        try (Stream<MapUserEntity> s = tx.read(withCriteria(mcb))) {
            return s.findFirst()
              .map(entityToAdapterFunc(realm)).orElse(null);
        }
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        LOG.tracef("getUserByEmail(%s, %s)%s", realm, email, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.EMAIL, Operator.EQ, email);

        List<MapUserEntity> usersWithEmail = tx.read(withCriteria(mcb))
                .filter(userEntity -> Objects.equals(userEntity.getEmail(), email))
                .collect(Collectors.toList());
        if (usersWithEmail.isEmpty()) return null;
        
        if (usersWithEmail.size() > 1) {
            // Realm settings have been changed from allowing duplicate emails to not allowing them
            // but duplicates haven't been removed.
            throw new ModelDuplicateException("Multiple users with email '" + email + "' exist in Keycloak.");
        }

        MapUserEntity userEntity = usersWithEmail.get(0);
        
        if (!realm.isDuplicateEmailsAllowed()) {
            if (userEntity.getEmail() != null && !userEntity.getEmail().equals(userEntity.getEmailConstraint())) {
                // Realm settings have been changed from allowing duplicate emails to not allowing them.
                // We need to update the email constraint to reflect this change in the user entities.
                userEntity.setEmailConstraint(userEntity.getEmail());
            }
        }
        
        return entityToAdapterFunc(realm).apply(userEntity);
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        LOG.tracef("getUsersCount(%s, %s)%s", realm, includeServiceAccount, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        if (! includeServiceAccount) {
            mcb = mcb.compare(SearchableFields.SERVICE_ACCOUNT_CLIENT, Operator.NOT_EXISTS);
        }

        return (int) tx.getCount(withCriteria(mcb));
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults, boolean includeServiceAccounts) {
        LOG.tracef("getUsersStream(%s, %d, %d, %s)%s", realm, firstResult, maxResults, includeServiceAccounts, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        if (! includeServiceAccounts) {
            mcb = mcb.compare(SearchableFields.SERVICE_ACCOUNT_CLIENT, Operator.NOT_EXISTS);
        }

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.USERNAME))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        LOG.tracef("getUsersStream(%s, %d, %d)%s", realm, firstResult, maxResults, getShortStackTrace());
        return getUsersStream(realm, firstResult, maxResults, false);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        LOG.tracef("searchForUserStream(%s, %s, %d, %d)%s", realm, search, firstResult, maxResults, getShortStackTrace());
        Map<String, String> attributes = new HashMap<>();
        attributes.put(UserModel.SEARCH, search);
        session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, false);
        return searchForUserStream(realm, attributes, firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        LOG.tracef("searchForUserStream(%s, %s, %d, %d)%s", realm, attributes, firstResult, maxResults, getShortStackTrace());

        final DefaultModelCriteria<UserModel> mcb = criteria();
        DefaultModelCriteria<UserModel> criteria = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        if (! session.getAttributeOrDefault(UserModel.INCLUDE_SERVICE_ACCOUNT, true)) {
            criteria = criteria.compare(SearchableFields.SERVICE_ACCOUNT_CLIENT, Operator.NOT_EXISTS);
        }

        final boolean exactSearch = Boolean.parseBoolean(attributes.getOrDefault(UserModel.EXACT, Boolean.FALSE.toString()));

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            value = value.trim();

            final String searchedString = exactSearch ? value : ("%" + value + "%");

            switch (key) {
                case UserModel.SEARCH:
                    DefaultModelCriteria<UserModel> searchCriteria = null;
                    for (String stringToSearch : value.split("\\s+")) {
                        if (searchCriteria == null) {
                            searchCriteria = addSearchToModelCriteria(stringToSearch, mcb);
                        } else {
                            searchCriteria = mcb.and(searchCriteria, addSearchToModelCriteria(stringToSearch, mcb));
                        }
                    }

                    criteria = mcb.and(criteria, searchCriteria);
                    break;
                case USERNAME:
                    criteria = criteria.compare(SearchableFields.USERNAME, Operator.ILIKE, searchedString);
                    break;
                case FIRST_NAME:
                    criteria = criteria.compare(SearchableFields.FIRST_NAME, Operator.ILIKE, searchedString);
                    break;
                case LAST_NAME:
                    criteria = criteria.compare(SearchableFields.LAST_NAME, Operator.ILIKE, searchedString);
                    break;
                case EMAIL:
                    criteria = criteria.compare(SearchableFields.EMAIL, Operator.ILIKE, searchedString);
                    break;
                case EMAIL_VERIFIED: {
                    boolean booleanValue = Boolean.parseBoolean(value);
                    criteria = criteria.compare(SearchableFields.EMAIL_VERIFIED, Operator.EQ, booleanValue);
                    break;
                }
                case UserModel.ENABLED: {
                    boolean booleanValue = Boolean.parseBoolean(value);
                    criteria = criteria.compare(SearchableFields.ENABLED, Operator.EQ, booleanValue);
                    break;
                }
                case UserModel.IDP_ALIAS: {
                    if (!attributes.containsKey(UserModel.IDP_USER_ID)) {
                        criteria = criteria.compare(SearchableFields.IDP_AND_USER, Operator.EQ, value);
                    }
                    break;
                }
                case UserModel.IDP_USER_ID: {
                    criteria = criteria.compare(SearchableFields.IDP_AND_USER, Operator.EQ, attributes.get(UserModel.IDP_ALIAS),
                            value);
                    break;
                }
                case UserModel.EXACT:
                    break;
                default:
                    criteria = criteria.compare(SearchableFields.ATTRIBUTE, Operator.EQ, key, value);
                    break;
            }
        }

        // Only return those results that the current user is authorized to view,
        // i.e. there is an intersection of groups with view permission of the current
        // user (passed in via UserModel.GROUPS attribute), the groups for the returned
        // users, and the respective group resource available from the authorization provider
        @SuppressWarnings("unchecked")
        Set<String> userGroups = (Set<String>) session.getAttribute(UserModel.GROUPS);
        if (userGroups != null) {
            if (userGroups.isEmpty()) {
                return Stream.empty();
            }

            final ResourceStore resourceStore =
                    session.getProvider(AuthorizationProvider.class).getStoreFactory().getResourceStore();

            HashSet<String> authorizedGroups = new HashSet<>(userGroups);
            authorizedGroups.removeIf(id -> {
                Map<Resource.FilterOption, String[]> values = new EnumMap<>(Resource.FilterOption.class);
                values.put(Resource.FilterOption.EXACT_NAME, new String[] {"group.resource." + id});
                return resourceStore.find(realm, null, values, 0, 1).isEmpty();
            });

            criteria = criteria.compare(SearchableFields.ASSIGNED_GROUP, Operator.IN, authorizedGroups);
        }

        return tx.read(withCriteria(criteria).pagination(firstResult, maxResults, SearchableFields.USERNAME))
                .map(entityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        LOG.tracef("getGroupMembersStream(%s, %s, %d, %d)%s", realm, group.getId(), firstResult, maxResults, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_GROUP, Operator.EQ, group.getId());

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.USERNAME))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        LOG.tracef("searchForUserByUserAttributeStream(%s, %s, %s)%s", realm, attrName, attrValue, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ATTRIBUTE, Operator.EQ, attrName, attrValue);

        return tx.read(withCriteria(mcb).orderBy(SearchableFields.USERNAME, ASCENDING))
          .map(entityToAdapterFunc(realm));
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return addUser(realm, null, username.toLowerCase(), true, true);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String userId = user.getId();
        Optional<MapUserEntity> userById = getEntityById(realm, userId);
        if (userById.isPresent()) {
            session.invalidate(USER_BEFORE_REMOVE, realm, user);

            tx.delete(userId);

            session.invalidate(USER_AFTER_REMOVE, realm, user);
            return true;
        }

        return false;
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        LOG.tracef("getRoleMembersStream(%s, %s, %d, %d)%s", realm, role, firstResult, maxResults, getShortStackTrace());
        DefaultModelCriteria<UserModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_ROLE, Operator.EQ, role.getId());

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.USERNAME))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public void close() {

    }

    public static <T> Stream<T> getCredentialProviders(KeycloakSession session, Class<T> type) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(CredentialProvider.class)
                .filter(f -> Types.supports(type, f, CredentialProviderFactory.class))
                .map(f -> (T) session.getProvider(CredentialProvider.class, f.getId()));
    }

    @Override
    public CredentialValidationOutput getUserByCredential(RealmModel realm, CredentialInput input) {
        // TODO: future implementations would narrow down the stream to those provider enabled for the specific realm
        Stream<CredentialAuthentication> credentialAuthenticationStream = getCredentialProviders(session, CredentialAuthentication.class);

        CredentialValidationOutput r = credentialAuthenticationStream
                .filter(credentialAuthentication -> credentialAuthentication.supportsCredentialAuthenticationFor(input.getType()))
                .map(credentialAuthentication -> credentialAuthentication.authenticate(realm, input))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        if (r == null && tx instanceof MapKeycloakTransactionWithAuth) {
            MapCredentialValidationOutput<MapUserEntity> result = ((MapKeycloakTransactionWithAuth<MapUserEntity, UserModel>) tx).authenticate(realm, input);
            if (result != null) {
                UserModel user = null;
                if (result.getAuthenticatedUser() != null) {
                    user = entityToAdapterFunc(realm).apply(result.getAuthenticatedUser());
                }
                r = new CredentialValidationOutput(user, result.getAuthStatus(), result.getState());
            }
        }
        return r;
    }

    private DefaultModelCriteria<UserModel> addSearchToModelCriteria(String value,
            DefaultModelCriteria<UserModel> mcb) {

        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            // exact search
            value = value.substring(1, value.length() - 1);
        } else {
            if (value.length() >= 2 && value.charAt(0) == '*' && value.charAt(value.length() - 1) == '*') {
                // infix search
                value = "%" + value.substring(1, value.length() - 1) + "%";
            } else {
                // default to prefix search
                if (value.length() > 0 && value.charAt(value.length() - 1) == '*') {
                    value = value.substring(0, value.length() - 1);
                }
                value += "%";
            }
        }

        return mcb.or(
                mcb.compare(SearchableFields.USERNAME, Operator.ILIKE, value),
                mcb.compare(SearchableFields.EMAIL, Operator.ILIKE, value),
                mcb.compare(SearchableFields.FIRST_NAME, Operator.ILIKE, value),
                mcb.compare(SearchableFields.LAST_NAME, Operator.ILIKE, value));
    }
}

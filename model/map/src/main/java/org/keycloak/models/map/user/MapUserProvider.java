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

package org.keycloak.models.map.user;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
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
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.SearchableFields;
import org.keycloak.models.UserProvider;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.client.ClientStorageProvider;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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
import static org.keycloak.models.map.common.MapStorageUtils.registerEntityForChanges;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;

public class MapUserProvider<K> implements UserProvider.Streams, UserCredentialStore.Streams {

    private static final Logger LOG = Logger.getLogger(MapUserProvider.class);
    private final KeycloakSession session;
    final MapKeycloakTransaction<K, MapUserEntity<K>, UserModel> tx;
    private final MapStorage<K, MapUserEntity<K>, UserModel> userStore;

    public MapUserProvider(KeycloakSession session, MapStorage<K, MapUserEntity<K>, UserModel> store) {
        this.session = session;
        this.userStore = store;
        this.tx = userStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private Function<MapUserEntity<K>, UserModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapUserAdapter<K>(session, realm, registerEntityForChanges(tx, origEntity)) {
            @Override
            public String getId() {
                return userStore.getKeyConvertor().keyToString(entity.getId());
            }

            @Override
            public boolean checkEmailUniqueness(RealmModel realm, String email) {
                return getUserByEmail(realm, email) != null;
            }

            @Override
            public boolean checkUsernameUniqueness(RealmModel realm, String username) {
                return getUserByUsername(realm, username) != null;
            }
        };
    }

    private Predicate<MapUserEntity<K>> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return c -> false;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    private ModelException userDoesntExistException() {
        return new ModelException("Specified user doesn't exist.");
    }

    private Optional<MapUserEntity<K>> getEntityById(RealmModel realm, String id) {
        try {
            return getEntityById(realm, userStore.getKeyConvertor().fromString(id));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private MapUserEntity<K> getRegisteredEntityByIdOrThrow(RealmModel realm, String id) {
        return getEntityById(realm, id)
                .map(e -> registerEntityForChanges(tx, e))
                .orElseThrow(this::userDoesntExistException);
    }

    private Optional<MapUserEntity<K>> getEntityById(RealmModel realm, K id) {
        MapUserEntity<K> mapUserEntity = tx.read(id);
        if (mapUserEntity != null && entityRealmFilter(realm).test(mapUserEntity)) {
            return Optional.of(mapUserEntity);
        }

        return Optional.empty();
    }

    private Optional<MapUserEntity<K>> getRegisteredEntityById(RealmModel realm, String id) {
        return getEntityById(realm, id).map(e -> registerEntityForChanges(tx, e));
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        if (user == null || user.getId() == null) {
            return;
        }
        LOG.tracef("addFederatedIdentity(%s, %s, %s)%s", realm, user.getId(), socialLink.getIdentityProvider(), getShortStackTrace());

        getRegisteredEntityById(realm, user.getId())
                .ifPresent(userEntity ->
                        userEntity.addFederatedIdentity(UserFederatedIdentityEntity.fromModel(socialLink)));
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        LOG.tracef("removeFederatedIdentity(%s, %s, %s)%s", realm, user.getId(), socialProvider, getShortStackTrace());
        return getRegisteredEntityById(realm, user.getId())
                .map(entity -> entity.removeFederatedIdentity(socialProvider))
                .orElse(false);
    }

    @Override
    public void preRemove(RealmModel realm, IdentityProviderModel provider) {
        String socialProvider = provider.getAlias();
        LOG.tracef("preRemove[RealmModel realm, IdentityProviderModel provider](%s, %s)%s", realm, socialProvider, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.IDP_AND_USER, Operator.EQ, socialProvider);

        tx.read(withCriteria(mcb))
                .map(e -> registerEntityForChanges(tx, e))
                .forEach(userEntity -> userEntity.removeFederatedIdentity(socialProvider));
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        LOG.tracef("updateFederatedIdentity(%s, %s, %s)%s", realm, federatedUser.getId(), federatedIdentityModel.getIdentityProvider(), getShortStackTrace());
        getRegisteredEntityById(realm, federatedUser.getId())
                .ifPresent(entity -> entity.updateFederatedIdentity(UserFederatedIdentityEntity.fromModel(federatedIdentityModel)));
    }

    @Override
    public Stream<FederatedIdentityModel> getFederatedIdentitiesStream(RealmModel realm, UserModel user) {
        LOG.tracef("getFederatedIdentitiesStream(%s, %s)%s", realm, user.getId(), getShortStackTrace());
        return getEntityById(realm, user.getId())
                .map(MapUserEntity::getFederatedIdentities).orElseGet(Stream::empty)
                .map(UserFederatedIdentityEntity::toModel);
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        LOG.tracef("getFederatedIdentity(%s, %s, %s)%s", realm, user.getId(), socialProvider, getShortStackTrace());
        return getEntityById(realm, user.getId())
                .map(userEntity -> userEntity.getFederatedIdentity(socialProvider))
                .map(UserFederatedIdentityEntity::toModel)
                .orElse(null);
    }

    @Override
    public UserModel getUserByFederatedIdentity(RealmModel realm, FederatedIdentityModel socialLink) {
        LOG.tracef("getUserByFederatedIdentity(%s, %s)%s", realm, socialLink, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
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

        getRegisteredEntityByIdOrThrow(realm, userId)
                .addUserConsent(UserConsentEntity.fromModel(consent));
    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientInternalId) {
        LOG.tracef("getConsentByClient(%s, %s, %s)%s", realm, userId, clientInternalId, getShortStackTrace());
        return getEntityById(realm, userId)
                .map(userEntity -> userEntity.getUserConsent(clientInternalId))
                .map(consent -> UserConsentEntity.toModel(realm, consent))
                .orElse(null);
    }

    @Override
    public Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId) {
        LOG.tracef("getConsentByClientStream(%s, %s)%s", realm, userId, getShortStackTrace());
        return getEntityById(realm, userId)
                .map(MapUserEntity::getUserConsents)
                .orElse(Stream.empty())
                .map(consent -> UserConsentEntity.toModel(realm, consent));
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        LOG.tracef("updateConsent(%s, %s, %s)%s", realm, userId, consent, getShortStackTrace());

        MapUserEntity<K> user = getRegisteredEntityByIdOrThrow(realm, userId);
        UserConsentEntity userConsentEntity = user.getUserConsent(consent.getClient().getId());
        if (userConsentEntity == null) {
            throw new ModelException("Consent not found for client [" + consent.getClient().getId() + "] and user [" + userId + "]");
        }

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
        return getRegisteredEntityById(realm, userId)
                .map(userEntity -> userEntity.removeUserConsent(clientInternalId))
                .orElse(false);
    }

    @Override
    public void setNotBeforeForUser(RealmModel realm, UserModel user, int notBefore) {
        LOG.tracef("setNotBeforeForUser(%s, %s, %d)%s", realm, user.getId(), notBefore, getShortStackTrace());
        getRegisteredEntityByIdOrThrow(realm, user.getId()).setNotBefore(notBefore);
    }

    @Override
    public int getNotBeforeOfUser(RealmModel realm, UserModel user) {
        LOG.tracef("getNotBeforeOfUser(%s, %s)%s", realm, user.getId(), getShortStackTrace());
        return getEntityById(realm, user.getId())
                .orElseThrow(this::userDoesntExistException)
                .getNotBefore();
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        LOG.tracef("getServiceAccount(%s)%s", client.getId(), getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, client.getRealm().getId())
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
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.USERNAME, Operator.EQ, username);

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("User with username '" + username + "' in realm " + realm.getName() + " already exists" );
        }
        
        final K entityId = id == null ? userStore.getKeyConvertor().yieldNewUniqueKey() : userStore.getKeyConvertor().fromString(id);

        if (tx.read(entityId) != null) {
            throw new ModelDuplicateException("User exists: " + entityId);
        }

        MapUserEntity<K> entity = new MapUserEntity<>(entityId, realm.getId());
        entity.setUsername(username.toLowerCase());
        entity.setCreatedTimestamp(Time.currentTimeMillis());

        tx.create(entity);
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
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        tx.delete(userStore.getKeyConvertor().yieldNewUniqueKey(), withCriteria(mcb));
    }

    @Override
    public void removeImportedUsers(RealmModel realm, String storageProviderId) {
        LOG.tracef("removeImportedUsers(%s, %s)%s", realm, storageProviderId, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.FEDERATION_LINK, Operator.EQ, storageProviderId);

        tx.delete(userStore.getKeyConvertor().yieldNewUniqueKey(), withCriteria(mcb));
    }

    @Override
    public void unlinkUsers(RealmModel realm, String storageProviderId) {
        LOG.tracef("unlinkUsers(%s, %s)%s", realm, storageProviderId, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.FEDERATION_LINK, Operator.EQ, storageProviderId);

        try (Stream<MapUserEntity<K>> s = tx.read(withCriteria(mcb))) {
            s.map(e -> registerEntityForChanges(tx, e))
              .forEach(userEntity -> userEntity.setFederationLink(null));
        }
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        String roleId = role.getId();
        LOG.tracef("preRemove[RoleModel](%s, %s)%s", realm, roleId, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_ROLE, Operator.EQ, roleId);

        try (Stream<MapUserEntity<K>> s = tx.read(withCriteria(mcb))) {
            s.map(e -> registerEntityForChanges(tx, e))
              .forEach(userEntity -> userEntity.removeRolesMembership(roleId));
        }
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        String groupId = group.getId();
        LOG.tracef("preRemove[GroupModel](%s, %s)%s", realm, groupId, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_GROUP, Operator.EQ, groupId);

        try (Stream<MapUserEntity<K>> s = tx.read(withCriteria(mcb))) {
            s.map(e -> registerEntityForChanges(tx, e))
              .forEach(userEntity -> userEntity.removeGroupsMembership(groupId));
        }
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        String clientId = client.getId();
        LOG.tracef("preRemove[ClientModel](%s, %s)%s", realm, clientId, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.CONSENT_FOR_CLIENT, Operator.EQ, clientId);

        try (Stream<MapUserEntity<K>> s = tx.read(withCriteria(mcb))) {
            s.map(e -> registerEntityForChanges(tx, e))
              .forEach(userEntity -> userEntity.removeUserConsent(clientId));
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

        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, clientScope.getRealm().getId())
          .compare(SearchableFields.CONSENT_WITH_CLIENT_SCOPE, Operator.EQ, clientScopeId);

        try (Stream<MapUserEntity<K>> s = tx.read(withCriteria(mcb))) {
            s.flatMap(MapUserEntity::getUserConsents)
              .forEach(consent -> consent.removeGrantedClientScopesIds(clientScopeId));
        }
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
        String componentId = component.getId();
        LOG.tracef("preRemove[ComponentModel](%s, %s)%s", realm, componentId, getShortStackTrace());
        if (component.getProviderType().equals(UserStorageProvider.class.getName())) {
            removeImportedUsers(realm, componentId);
        }
        if (component.getProviderType().equals(ClientStorageProvider.class.getName())) {
            ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
              .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
              .compare(SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, Operator.EQ, componentId);

            try (Stream<MapUserEntity<K>> s = tx.read(withCriteria(mcb))) {
                String providerIdS = new StorageId(componentId, "").getId();
                s.forEach(removeConsentsForExternalClient(providerIdS));
            }
        }
    }

    private Consumer<MapUserEntity<K>> removeConsentsForExternalClient(String idPrefix) {
        return userEntity -> {
            List<String> consentClientIds = userEntity.getUserConsents()
              .map(UserConsentEntity::getClientId)
              .filter(clientId -> clientId != null && clientId.startsWith(idPrefix))
              .collect(Collectors.toList());

            if (! consentClientIds.isEmpty()) {
                userEntity = registerEntityForChanges(tx, userEntity);
                consentClientIds.forEach(userEntity::removeUserConsent);
            }
        };
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        String roleId = role.getId();
        LOG.tracef("grantToAllUsers(%s, %s)%s", realm, roleId, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        try (Stream<MapUserEntity<K>> s = tx.read(withCriteria(mcb))) {
            s.map(e -> registerEntityForChanges(tx, e))
              .forEach(entity -> entity.addRolesMembership(roleId));
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
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.USERNAME, Operator.ILIKE, username);

        try (Stream<MapUserEntity<K>> s = tx.read(withCriteria(mcb))) {
            return s.findFirst()
              .map(entityToAdapterFunc(realm)).orElse(null);
        }
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        LOG.tracef("getUserByEmail(%s, %s)%s", realm, email, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.EMAIL, Operator.EQ, email);

        List<MapUserEntity<K>> usersWithEmail = tx.read(withCriteria(mcb))
                .filter(userEntity -> Objects.equals(userEntity.getEmail(), email))
                .collect(Collectors.toList());
        if (usersWithEmail.isEmpty()) return null;
        
        if (usersWithEmail.size() > 1) {
            // Realm settings have been changed from allowing duplicate emails to not allowing them
            // but duplicates haven't been removed.
            throw new ModelDuplicateException("Multiple users with email '" + email + "' exist in Keycloak.");
        }

        MapUserEntity<K> userEntity = registerEntityForChanges(tx, usersWithEmail.get(0));
        
        if (!realm.isDuplicateEmailsAllowed()) {
            if (userEntity.getEmail() != null && !userEntity.getEmail().equals(userEntity.getEmailConstraint())) {
                // Realm settings have been changed from allowing duplicate emails to not allowing them.
                // We need to update the email constraint to reflect this change in the user entities.
                userEntity.setEmailConstraint(userEntity.getEmail());
            }
        }
        
        return new MapUserAdapter<K>(session, realm, userEntity) {
            @Override
            public String getId() {
                return userStore.getKeyConvertor().keyToString(userEntity.getId());
            }

            @Override
            public boolean checkEmailUniqueness(RealmModel realm, String email) {
                return getUserByEmail(realm, email) != null;
            }
            @Override
            public boolean checkUsernameUniqueness(RealmModel realm, String username) {
                return getUserByUsername(realm, username) != null;
            }
        };
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        LOG.tracef("getUsersCount(%s, %s)%s", realm, includeServiceAccount, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        if (! includeServiceAccount) {
            mcb = mcb.compare(SearchableFields.SERVICE_ACCOUNT_CLIENT, Operator.NOT_EXISTS);
        }

        return (int) tx.getCount(withCriteria(mcb));
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults, boolean includeServiceAccounts) {
        LOG.tracef("getUsersStream(%s, %d, %d, %s)%s", realm, firstResult, maxResults, includeServiceAccounts, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

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

        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        if (! session.getAttributeOrDefault(UserModel.INCLUDE_SERVICE_ACCOUNT, true)) {
            mcb = mcb.compare(SearchableFields.SERVICE_ACCOUNT_CLIENT, Operator.NOT_EXISTS);
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
                    for (String stringToSearch : value.trim().split("\\s+")) {
                        if (value.isEmpty()) {
                            continue;
                        }
                        final String s = exactSearch ? stringToSearch : ("%" + stringToSearch + "%");
                        mcb = mcb.or(
                          userStore.createCriteriaBuilder().compare(SearchableFields.USERNAME, Operator.ILIKE, s),
                          userStore.createCriteriaBuilder().compare(SearchableFields.EMAIL, Operator.ILIKE, s),
                          userStore.createCriteriaBuilder().compare(SearchableFields.FIRST_NAME, Operator.ILIKE, s),
                          userStore.createCriteriaBuilder().compare(SearchableFields.LAST_NAME, Operator.ILIKE, s)
                        );
                    }
                    break;

                case USERNAME:
                    mcb = mcb.compare(SearchableFields.USERNAME, Operator.ILIKE, searchedString);
                    break;
                case FIRST_NAME:
                    mcb = mcb.compare(SearchableFields.FIRST_NAME, Operator.ILIKE, searchedString);
                    break;
                case LAST_NAME:
                    mcb = mcb.compare(SearchableFields.LAST_NAME, Operator.ILIKE, searchedString);
                    break;
                case EMAIL:
                    mcb = mcb.compare(SearchableFields.EMAIL, Operator.ILIKE, searchedString);
                    break;
                case EMAIL_VERIFIED: {
                    boolean booleanValue = Boolean.parseBoolean(value);
                    mcb = mcb.compare(SearchableFields.EMAIL_VERIFIED, Operator.EQ, booleanValue);
                    break;
                }
                case UserModel.ENABLED: {
                    boolean booleanValue = Boolean.parseBoolean(value);
                    mcb = mcb.compare(SearchableFields.ENABLED, Operator.EQ, booleanValue);
                    break;
                }
                case UserModel.IDP_ALIAS: {
                    if (! attributes.containsKey(UserModel.IDP_USER_ID)) {
                        mcb = mcb.compare(SearchableFields.IDP_AND_USER, Operator.EQ, value);
                    }
                    break;
                }
                case UserModel.IDP_USER_ID: {
                    mcb = mcb.compare(SearchableFields.IDP_AND_USER, Operator.EQ, attributes.get(UserModel.IDP_ALIAS), value);
                    break;
                }
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

            final ResourceStore resourceStore = session.getProvider(AuthorizationProvider.class).getStoreFactory().getResourceStore();

            HashSet<String> authorizedGroups = new HashSet<>(userGroups);
            authorizedGroups.removeIf(id -> {
                Map<Resource.FilterOption, String[]> values = new EnumMap<>(Resource.FilterOption.class);
                values.put(Resource.FilterOption.EXACT_NAME, new String[] { "group.resource." + id });
                return resourceStore.findByResourceServer(values, null, 0, 1).isEmpty();
            });

            mcb = mcb.compare(SearchableFields.ASSIGNED_GROUP, Operator.IN, authorizedGroups);
        }

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.USERNAME))
                .map(entityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        LOG.tracef("getGroupMembersStream(%s, %s, %d, %d)%s", realm, group.getId(), firstResult, maxResults, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_GROUP, Operator.EQ, group.getId());

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.USERNAME))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        LOG.tracef("searchForUserByUserAttributeStream(%s, %s, %s)%s", realm, attrName, attrValue, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
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
        Optional<MapUserEntity<K>> userById = getEntityById(realm, userId);
        if (userById.isPresent()) {
            tx.delete(userStore.getKeyConvertor().fromString(userId));
            return true;
        }

        return false;
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        LOG.tracef("getRoleMembersStream(%s, %s, %d, %d)%s", realm, role, firstResult, maxResults, getShortStackTrace());
        ModelCriteriaBuilder<UserModel> mcb = userStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.ASSIGNED_ROLE, Operator.EQ, role.getId());

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.USERNAME))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        getRegisteredEntityById(realm, user.getId())
                .ifPresent(updateCredential(cred));
    }
    
    private Consumer<MapUserEntity<K>> updateCredential(CredentialModel credentialModel) {
        return user -> {
            UserCredentialEntity credentialEntity = user.getCredential(credentialModel.getId());
            if (credentialEntity == null) return;

            credentialEntity.setCreatedDate(credentialModel.getCreatedDate());
            credentialEntity.setUserLabel(credentialModel.getUserLabel());
            credentialEntity.setType(credentialModel.getType());
            credentialEntity.setSecretData(credentialModel.getSecretData());
            credentialEntity.setCredentialData(credentialModel.getCredentialData());
        };
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        LOG.tracef("createCredential(%s, %s, %s)%s", realm, user.getId(), cred.getId(), getShortStackTrace());
        UserCredentialEntity credentialEntity = UserCredentialEntity.fromModel(cred);

        getRegisteredEntityByIdOrThrow(realm, user.getId())
                .addCredential(credentialEntity);

        return UserCredentialEntity.toModel(credentialEntity);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        LOG.tracef("removeStoredCredential(%s, %s, %s)%s", realm, user.getId(), id, getShortStackTrace());
        return getRegisteredEntityById(realm, user.getId())
                .map(mapUserEntity -> mapUserEntity.removeCredential(id))
                .orElse(false);
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        LOG.tracef("getStoredCredentialById(%s, %s, %s)%s", realm, user.getId(), id, getShortStackTrace());
        return getEntityById(realm, user.getId())
                .map(mapUserEntity -> mapUserEntity.getCredential(id))
                .map(UserCredentialEntity::toModel)
                .orElse(null);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream(RealmModel realm, UserModel user) {
        LOG.tracef("getStoredCredentialsStream(%s, %s)%s", realm, user.getId(), getShortStackTrace());
        return getEntityById(realm, user.getId())
                .map(MapUserEntity::getCredentials)
                .orElseGet(Stream::empty)
                .map(UserCredentialEntity::toModel);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(RealmModel realm, UserModel user, String type) {
        LOG.tracef("getStoredCredentialsByTypeStream(%s, %s, %s)%s", realm, user.getId(), type, getShortStackTrace());
        return getStoredCredentialsStream(realm, user)
                .filter(credential -> Objects.equals(type, credential.getType()));
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        LOG.tracef("getStoredCredentialByNameAndType(%s, %s, %s, %s)%s", realm, user.getId(), name, type, getShortStackTrace());
        return getStoredCredentialsByType(realm, user, type).stream()
                .filter(credential -> Objects.equals(name, credential.getUserLabel()))
                .findFirst().orElse(null);
    }

    @Override
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId) {
        LOG.tracef("moveCredentialTo(%s, %s, %s, %s)%s", realm, user.getId(), id, newPreviousCredentialId, getShortStackTrace());
        String userId = user.getId();
        MapUserEntity<K> userEntity = getRegisteredEntityById(realm, userId).orElse(null);
        if (userEntity == null) {
            LOG.warnf("User with id: [%s] not found", userId);
            return false;
        }

        // Find index of credential which should be before id in the list
        int newPreviousCredentialIdIndex = -1; // If newPreviousCredentialId == null we need to put id credential to index 0
        if (newPreviousCredentialId != null) {
            newPreviousCredentialIdIndex = userEntity.getCredentialIndex(newPreviousCredentialId);
            if (newPreviousCredentialIdIndex == -1) { // If not null previous credential not found, print warning and return false
                LOG.warnf("Credential with id: [%s] for user: [%s] not found", newPreviousCredentialId, userId);
                return false;
            }
        }

        // Find current index of credential (id) which will be moved
        int currentPositionOfId = userEntity.getCredentialIndex(id);
        if (currentPositionOfId == -1) {
            LOG.warnf("Credential with id: [%s] for user: [%s] not found", id, userId);
            return false;
        }

        // If id is before newPreviousCredentialId in priority list, it will be moved to position -1
        if (currentPositionOfId < newPreviousCredentialIdIndex) {
            newPreviousCredentialIdIndex -= 1;
        }

        // Move credential to desired index
        userEntity.moveCredential(currentPositionOfId, newPreviousCredentialIdIndex + 1);
        return true;
    }

    @Override
    public void close() {

    }
}

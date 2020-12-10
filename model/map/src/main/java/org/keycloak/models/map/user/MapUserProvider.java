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

import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
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
import org.keycloak.models.UserProvider;
import org.keycloak.models.map.common.Serialization;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.utils.DefaultRoles;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.client.ClientStorageProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

public class MapUserProvider implements UserProvider.Streams, UserCredentialStore.Streams {

    private static final Logger LOG = Logger.getLogger(MapUserProvider.class);
    private static final Predicate<MapUserEntity> ALWAYS_FALSE = c -> { return false; };
    private final KeycloakSession session;
    final MapKeycloakTransaction<UUID, MapUserEntity> tx;
    private final MapStorage<UUID, MapUserEntity> userStore;

    public MapUserProvider(KeycloakSession session, MapStorage<UUID, MapUserEntity> store) {
        this.session = session;
        this.userStore = store;
        this.tx = new MapKeycloakTransaction<>(userStore);
        session.getTransactionManager().enlist(tx);
    }

    private MapUserEntity registerEntityForChanges(MapUserEntity origEntity) {
        MapUserEntity res = tx.get(origEntity.getId(), id -> Serialization.from(origEntity));
        tx.putIfChanged(origEntity.getId(), res, MapUserEntity::isUpdated);
        return res;
    }

    private Function<MapUserEntity, UserModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller
        return origEntity -> new MapUserAdapter(session, realm, registerEntityForChanges(origEntity)) {

            @Override
            public boolean checkEmailUniqueness(RealmModel realm, String email) {
                return getUserByEmail(email, realm) != null;
            }

            @Override
            public boolean checkUsernameUniqueness(RealmModel realm, String username) {
                return getUserByUsername(username, realm) != null;
            }
        };
    }

    private Predicate<MapUserEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return MapUserProvider.ALWAYS_FALSE;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    private ModelException userDoesntExistException() {
        return new ModelException("Specified user doesn't exist.");
    }

    private Optional<MapUserEntity> getEntityById(RealmModel realm, String id) {
        try {
            return getEntityById(realm, UUID.fromString(id));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private MapUserEntity getRegisteredEntityByIdOrThrow(RealmModel realm, String id) {
        return getEntityById(realm, id)
                .map(this::registerEntityForChanges)
                .orElseThrow(this::userDoesntExistException);
    }

    private Optional<MapUserEntity> getEntityById(RealmModel realm, UUID id) {
        MapUserEntity mapUserEntity = tx.get(id, userStore::get);
        if (mapUserEntity != null && entityRealmFilter(realm).test(mapUserEntity)) {
            return Optional.of(mapUserEntity);
        }

        return Optional.empty();
    }

    private Optional<MapUserEntity> getRegisteredEntityById(RealmModel realm, String id) {
        return getEntityById(realm, id).map(this::registerEntityForChanges);
    }

    private Stream<MapUserEntity> getNotRemovedUpdatedUsersStream() {
        Stream<MapUserEntity> updatedAndNotRemovedUsersStream = userStore.entrySet().stream()
                .map(tx::getUpdated)    // If the group has been removed, tx.get will return null, otherwise it will return me.getValue()
                .filter(Objects::nonNull);
        return Stream.concat(tx.createdValuesStream(), updatedAndNotRemovedUsersStream);
    }

    private Stream<MapUserEntity> getUnsortedUserEntitiesStream(RealmModel realm) {
        return getNotRemovedUpdatedUsersStream()
                .filter(entityRealmFilter(realm));
    }
    
    private <T> Stream<T> paginatedStream(Stream<T> originalStream, Integer first, Integer max) {
        if (first != null && first > 0) {
            originalStream = originalStream.skip(first);
        }

        if (max != null && max >= 0) {
            originalStream = originalStream.limit(max);
        }

        return originalStream;
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
        getUnsortedUserEntitiesStream(realm)
                .map(this::registerEntityForChanges)
                .forEach(userEntity -> userEntity.removeFederatedIdentity(socialProvider));
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        LOG.tracef("updateFederatedIdentity(%s, %s, %s)%s", realm, federatedUser.getId(), federatedIdentityModel.getIdentityProvider(), getShortStackTrace());
        getRegisteredEntityById(realm, federatedUser.getId())
                .ifPresent(entity -> entity.updateFederatedIdentity(UserFederatedIdentityEntity.fromModel(federatedIdentityModel)));
    }

    @Override
    public Stream<FederatedIdentityModel> getFederatedIdentitiesStream(UserModel user, RealmModel realm) {
        LOG.tracef("getFederatedIdentitiesStream(%s, %s)%s", realm, user.getId(), getShortStackTrace());
        return getEntityById(realm, user.getId())
                .map(AbstractUserEntity::getFederatedIdentities).orElseGet(Stream::empty)
                .map(UserFederatedIdentityEntity::toModel);
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        LOG.tracef("getFederatedIdentity(%s, %s, %s)%s", realm, user.getId(), socialProvider, getShortStackTrace());
        return getEntityById(realm, user.getId())
                .map(userEntity -> userEntity.getFederatedIdentity(socialProvider))
                .map(UserFederatedIdentityEntity::toModel)
                .orElse(null);
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        LOG.tracef("getUserByFederatedIdentity(%s, %s)%s", realm, socialLink, getShortStackTrace());
        return getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> Objects.nonNull(userEntity.getFederatedIdentity(socialLink.getIdentityProvider())))
                .filter(userEntity -> Objects.equals(userEntity.getFederatedIdentity(socialLink.getIdentityProvider()).getUserId(), socialLink.getUserId()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            if (list.size() == 0) {
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

        UserConsentEntity consentEntity = UserConsentEntity.fromModel(consent);
        getRegisteredEntityById(realm, userId).ifPresent(userEntity -> userEntity.addUserConsent(consentEntity));
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
                .map(AbstractUserEntity::getUserConsents)
                .orElse(Stream.empty())
                .map(consent -> UserConsentEntity.toModel(realm, consent));
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        LOG.tracef("updateConsent(%s, %s, %s)%s", realm, userId, consent, getShortStackTrace());

        MapUserEntity user = getRegisteredEntityByIdOrThrow(realm, userId);
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
        getRegisteredEntityById(realm, user.getId()).ifPresent(userEntity -> userEntity.setNotBefore(notBefore));
    }

    @Override
    public int getNotBeforeOfUser(RealmModel realm, UserModel user) {
        LOG.tracef("getNotBeforeOfUser(%s, %s)%s", realm, user.getId(), getShortStackTrace());
        return getEntityById(realm, user.getId())
                .map(AbstractUserEntity::getNotBefore)
                .orElse(0);
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        LOG.tracef("getServiceAccount(%s)%s", client.getId(), getShortStackTrace());
        return getUnsortedUserEntitiesStream(client.getRealm())
                .filter(userEntity -> Objects.equals(userEntity.getServiceAccountClientLink(), client.getId()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            if (list.size() == 0) {
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
        if (getUnsortedUserEntitiesStream(realm)
                .anyMatch(userEntity -> Objects.equals(userEntity.getUsername(), username))) {
            throw new ModelDuplicateException("User with username '" + username + "' in realm " + realm.getName() + " already exists" );
        }
        
        final UUID entityId = id == null ? UUID.randomUUID() : UUID.fromString(id);

        if (tx.get(entityId, userStore::get) != null) {
            throw new ModelDuplicateException("User exists: " + entityId);
        }

        MapUserEntity entity = new MapUserEntity(entityId, realm.getId());
        entity.setUsername(username.toLowerCase());
        entity.setCreatedTimestamp(Time.currentTimeMillis());

        tx.putIfAbsent(entityId, entity);
        final UserModel userModel = entityToAdapterFunc(realm).apply(entity);

        if (addDefaultRoles) {
            DefaultRoles.addDefaultRoles(realm, userModel);

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
        getUnsortedUserEntitiesStream(realm)
                .map(MapUserEntity::getId)
                .forEach(tx::remove);
    }

    @Override
    public void removeImportedUsers(RealmModel realm, String storageProviderId) {
        LOG.tracef("removeImportedUsers(%s, %s)%s", realm, storageProviderId, getShortStackTrace());
        getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> Objects.equals(userEntity.getFederationLink(), storageProviderId))
                .map(MapUserEntity::getId)
                .forEach(tx::remove);
    }

    @Override
    public void unlinkUsers(RealmModel realm, String storageProviderId) {
        LOG.tracef("unlinkUsers(%s, %s)%s", realm, storageProviderId, getShortStackTrace());
        getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> Objects.equals(userEntity.getFederationLink(), storageProviderId))
                .map(this::registerEntityForChanges)
                .forEach(userEntity -> userEntity.setFederationLink(null));
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        String roleId = role.getId();
        LOG.tracef("preRemove[RoleModel](%s, %s)%s", realm, roleId, getShortStackTrace());
        getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> userEntity.getRolesMembership().contains(roleId))
                .map(this::registerEntityForChanges)
                .forEach(userEntity -> userEntity.removeRolesMembership(roleId));
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        String groupId = group.getId();
        LOG.tracef("preRemove[GroupModel](%s, %s)%s", realm, groupId, getShortStackTrace());
        getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> userEntity.getGroupsMembership().contains(groupId))
                .map(this::registerEntityForChanges)
                .forEach(userEntity -> userEntity.removeGroupsMembership(groupId));
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        String clientId = client.getId();
        LOG.tracef("preRemove[ClientModel](%s, %s)%s", realm, clientId, getShortStackTrace());
        getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> Objects.nonNull(userEntity.getUserConsent(clientId)))
                .map(this::registerEntityForChanges)
                .forEach(userEntity -> userEntity.removeUserConsent(clientId));
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        // No-op
    }

    @Override
    public void preRemove(ClientScopeModel clientScope) {
        String clientScopeId = clientScope.getId();
        LOG.tracef("preRemove[ClientScopeModel](%s)%s", clientScopeId, getShortStackTrace());

        getUnsortedUserEntitiesStream(clientScope.getRealm())
                .map(this::registerEntityForChanges)
                .flatMap(AbstractUserEntity::getUserConsents)
                .forEach(consent -> consent.removeGrantedClientScopesIds(clientScopeId));
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
        String componentId = component.getId();
        LOG.tracef("preRemove[ComponentModel](%s, %s)%s", realm, componentId, getShortStackTrace());
        if (component.getProviderType().equals(UserStorageProvider.class.getName())) {
            removeImportedUsers(realm, componentId);
        }
        if (component.getProviderType().equals(ClientStorageProvider.class.getName())) {
            getUnsortedUserEntitiesStream(realm)
                    .forEach(removeConsentsForExternalClient(componentId));
        }
    }

    private Consumer<MapUserEntity> removeConsentsForExternalClient(String componentId) {
        return userEntity -> {
            List<UserConsentEntity> consentModels = userEntity.getUserConsents()
                    .filter(consent ->
                            Objects.equals(new StorageId(consent.getClientId()).getProviderId(), componentId))
                    .collect(Collectors.toList());

            if (consentModels.size() > 0) {
                userEntity = registerEntityForChanges(userEntity);
                for (UserConsentEntity consentEntity : consentModels) {
                    userEntity.removeUserConsent(consentEntity.getClientId());
                }
            }
        };
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        String roleId = role.getId();
        LOG.tracef("grantToAllUsers(%s, %s)%s", realm, roleId, getShortStackTrace());
        getUnsortedUserEntitiesStream(realm)
                .map(this::registerEntityForChanges)
                .forEach(entity -> entity.addRolesMembership(roleId));
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        LOG.tracef("getUserById(%s, %s)%s", realm, id, getShortStackTrace());
        return getEntityById(realm, id).map(entityToAdapterFunc(realm)).orElse(null);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        if (username == null) return null;
        final String usernameLowercase = username.toLowerCase();
        
        LOG.tracef("getUserByUsername(%s, %s)%s", realm, username, getShortStackTrace());
        return getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> Objects.equals(userEntity.getUsername(), usernameLowercase))
                .findFirst()
                .map(entityToAdapterFunc(realm)).orElse(null);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        LOG.tracef("getUserByEmail(%s, %s)%s", realm, email, getShortStackTrace());
        List<MapUserEntity> usersWithEmail = getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> Objects.equals(userEntity.getEmail(), email))
                .collect(Collectors.toList());
        if (usersWithEmail.size() == 0) return null;
        
        if (usersWithEmail.size() > 1) {
            // Realm settings have been changed from allowing duplicate emails to not allowing them
            // but duplicates haven't been removed.
            throw new ModelDuplicateException("Multiple users with email '" + email + "' exist in Keycloak.");
        }

        MapUserEntity userEntity = registerEntityForChanges(usersWithEmail.get(0));
        
        if (!realm.isDuplicateEmailsAllowed()) {
            if (userEntity.getEmail() != null && !userEntity.getEmail().equals(userEntity.getEmailConstraint())) {
                // Realm settings have been changed from allowing duplicate emails to not allowing them.
                // We need to update the email constraint to reflect this change in the user entities.
                userEntity.setEmailConstraint(userEntity.getEmail());
            }
        }
        
        return new MapUserAdapter(session, realm, userEntity) {
            @Override
            public boolean checkEmailUniqueness(RealmModel realm, String email) {
                return getUserByEmail(email, realm) != null;
            }
            @Override
            public boolean checkUsernameUniqueness(RealmModel realm, String username) {
                return getUserByUsername(username, realm) != null;
            }
        };
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        LOG.tracef("getUsersCount(%s)%s", realm, getShortStackTrace());
        return getUsersCount(realm, false);
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        LOG.tracef("getUsersCount(%s, %s)%s", realm, includeServiceAccount, getShortStackTrace());
        Stream<MapUserEntity> unsortedUserEntitiesStream = getUnsortedUserEntitiesStream(realm);
        
        if (!includeServiceAccount) {
            unsortedUserEntitiesStream = unsortedUserEntitiesStream
                    .filter(userEntity -> Objects.isNull(userEntity.getServiceAccountClientLink()));
        }
        
        return (int) unsortedUserEntitiesStream.count();
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults, boolean includeServiceAccounts) {
        LOG.tracef("getUsersStream(%s, %d, %d, %s)%s", realm, firstResult, maxResults, includeServiceAccounts, getShortStackTrace());
        Stream<MapUserEntity> usersStream = getUnsortedUserEntitiesStream(realm);
        if (!includeServiceAccounts) {
            usersStream = usersStream.filter(userEntity -> Objects.isNull(userEntity.getServiceAccountClientLink()));
        }

        return paginatedStream(usersStream.sorted(MapUserEntity.COMPARE_BY_USERNAME), firstResult, maxResults)
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm) {
        LOG.tracef("getUsersStream(%s)%s", realm, getShortStackTrace());
        return getUsersStream(realm, null, null, false);
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, boolean includeServiceAccounts) {
        LOG.tracef("getUsersStream(%s)%s", realm, getShortStackTrace());
        return getUsersStream(realm, null, null, includeServiceAccounts);
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, int firstResult, int maxResults) {
        LOG.tracef("getUsersStream(%s, %d, %d)%s", realm, firstResult, maxResults, getShortStackTrace());
        return getUsersStream(realm, firstResult, maxResults, false);
    }

    @Override
    public Stream<UserModel> searchForUserStream(String search, RealmModel realm) {
        LOG.tracef("searchForUserStream(%s, %s)%s", realm, search, getShortStackTrace());
        return searchForUserStream(search, realm, null, null);
    }

    @Override
    public Stream<UserModel> searchForUserStream(String search, RealmModel realm, Integer firstResult, Integer maxResults) {
        LOG.tracef("searchForUserStream(%s, %s, %d, %d)%s", realm, search, firstResult, maxResults, getShortStackTrace());
        Map<String, String> attributes = new HashMap<>();
        attributes.put(UserModel.SEARCH, search);
        session.setAttribute(UserModel.INCLUDE_SERVICE_ACCOUNT, false);
        return searchForUserStream(attributes, realm, firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> searchForUserStream(Map<String, String> params, RealmModel realm) {
        LOG.tracef("searchForUserStream(%s, %s)%s", realm, params, getShortStackTrace());
        return searchForUserStream(params, realm, null, null);
    }

    @Override
    public Stream<UserModel> searchForUserStream(Map<String, String> attributes, RealmModel realm, Integer firstResult, Integer maxResults) {
        LOG.tracef("searchForUserStream(%s, %s, %d, %d)%s", realm, attributes, firstResult, maxResults, getShortStackTrace());
        /* Find all predicates based on attributes map */
        List<Predicate<MapUserEntity>> predicatesList = new ArrayList<>();

        if (!session.getAttributeOrDefault(UserModel.INCLUDE_SERVICE_ACCOUNT, true)) {
            predicatesList.add(userEntity -> Objects.isNull(userEntity.getServiceAccountClientLink()));
        }

        final boolean exactSearch = Boolean.parseBoolean(attributes.getOrDefault(UserModel.EXACT, Boolean.FALSE.toString()));

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            
            final String searchedString = value.toLowerCase();
            Function<Function<MapUserEntity, String>, Predicate<MapUserEntity>> containsOrExactPredicate =
                    func -> {
                        return userEntity -> testContainsOrExact(func.apply(userEntity), searchedString, exactSearch);
                    };

            switch (key) {
                case UserModel.SEARCH:
                    List<Predicate<MapUserEntity>> orPredicates = new ArrayList<>();
                    orPredicates.add(userEntity -> StringUtils.containsIgnoreCase(userEntity.getUsername(), searchedString));
                    orPredicates.add(userEntity -> StringUtils.containsIgnoreCase(userEntity.getEmail(), searchedString));
                    orPredicates.add(userEntity -> StringUtils.containsIgnoreCase(concatFirstNameLastName(userEntity), searchedString));

                    predicatesList.add(orPredicates.stream().reduce(Predicate::or).orElse(t -> false));
                    break;

                case USERNAME:
                    predicatesList.add(containsOrExactPredicate.apply(MapUserEntity::getUsername));
                    break;
                case FIRST_NAME:
                    predicatesList.add(containsOrExactPredicate.apply(MapUserEntity::getFirstName));
                    break;
                case LAST_NAME:
                    predicatesList.add(containsOrExactPredicate.apply(MapUserEntity::getLastName));
                    break;
                case EMAIL:
                    predicatesList.add(containsOrExactPredicate.apply(MapUserEntity::getEmail));
                    break;
                case EMAIL_VERIFIED: {
                    boolean booleanValue = Boolean.parseBoolean(searchedString);
                    predicatesList.add(userEntity -> Objects.equals(userEntity.isEmailVerified(), booleanValue));
                    break;
                }
                case UserModel.ENABLED: {
                    boolean booleanValue = Boolean.parseBoolean(searchedString);
                    predicatesList.add(userEntity -> Objects.equals(userEntity.isEnabled(), booleanValue));
                    break;
                }
                case UserModel.IDP_ALIAS: {
                    predicatesList.add(mapUserEntity -> Objects.nonNull(mapUserEntity.getFederatedIdentity(value)));
                    break;
                }
                case UserModel.IDP_USER_ID: {
                    predicatesList.add(mapUserEntity -> mapUserEntity.getFederatedIdentities()
                            .anyMatch(idp -> Objects.equals(idp.getUserId(), value)));
                    break;
                }
            }
        }

        @SuppressWarnings("unchecked")
        Set<String> userGroups = (Set<String>) session.getAttribute(UserModel.GROUPS);
        if (userGroups != null && userGroups.size() > 0) {
            final ResourceStore resourceStore = session.getProvider(AuthorizationProvider.class).getStoreFactory()
                    .getResourceStore();
            final Predicate<String> resourceByGroupIdExists = id -> resourceStore
                    .findByResourceServer(Collections.singletonMap("name", new String[] { "group.resource." + id }),
                            null, 0, 1).size() == 1;

            predicatesList.add(userEntity -> {
                return userEntity.getGroupsMembership()
                        .stream()
                        .filter(userGroups::contains)
                        .anyMatch(resourceByGroupIdExists);
            });
        }

        // Prepare resulting predicate
        Predicate<MapUserEntity> resultingPredicate = predicatesList.stream()
                .reduce(Predicate::and) // Combine all predicates with and
                .orElse(t -> true); // If there is no predicate in predicatesList, return all users

        Stream<MapUserEntity> usersStream = getUnsortedUserEntitiesStream(realm) // Get stream of all users in the realm
                .filter(resultingPredicate) // Apply all predicates to userStream
                .sorted(AbstractUserEntity.COMPARE_BY_USERNAME); // Sort before paginating
        
        return paginatedStream(usersStream, firstResult, maxResults) // paginate if necessary
                .map(entityToAdapterFunc(realm))
                .filter(Objects::nonNull);
    }

    private String concatFirstNameLastName(MapUserEntity entity) {
        StringBuilder stringBuilder = new StringBuilder();
        if (entity.getFirstName() != null) {
            stringBuilder.append(entity.getFirstName());
        }

        stringBuilder.append(" ");

        if (entity.getLastName() != null) {
            stringBuilder.append(entity.getLastName());
        }

        return stringBuilder.toString();
    }
    
    private boolean testContainsOrExact(String testedString, String searchedString, boolean exactMatch) {
        if (exactMatch) {
            return StringUtils.equalsIgnoreCase(testedString, searchedString);
        } else {
            return StringUtils.containsIgnoreCase(testedString, searchedString);
        }
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        LOG.tracef("getGroupMembersStream(%s, %s, %d, %d)%s", realm, group.getId(), firstResult, maxResults, getShortStackTrace());
        return paginatedStream(getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> userEntity.getGroupsMembership().contains(group.getId()))
                .sorted(MapUserEntity.COMPARE_BY_USERNAME), firstResult, maxResults)
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
        LOG.tracef("getGroupMembersStream(%s, %s)%s", realm, group.getId(), getShortStackTrace());
        return getGroupMembersStream(realm, group, null, null);
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(String attrName, String attrValue, RealmModel realm) {
        LOG.tracef("searchForUserByUserAttributeStream(%s, %s, %s)%s", realm, attrName, attrValue, getShortStackTrace());
        return getUnsortedUserEntitiesStream(realm)
                .filter(userEntity -> userEntity.getAttribute(attrName).contains(attrValue))
                .map(entityToAdapterFunc(realm))
                .sorted(UserModel.COMPARE_BY_USERNAME);
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
            tx.remove(UUID.fromString(userId));
            return true;
        }

        return false;
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        LOG.tracef("getRoleMembersStream(%s, %s, %d, %d)%s", realm, role, firstResult, maxResults, getShortStackTrace());
        return paginatedStream(getUnsortedUserEntitiesStream(realm)
                .filter(entity -> entity.getRolesMembership().contains(role.getId()))
                .sorted(MapUserEntity.COMPARE_BY_USERNAME), firstResult, maxResults)
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        getRegisteredEntityById(realm, user.getId())
                .ifPresent(updateCredential(cred));
    }
    
    private Consumer<MapUserEntity> updateCredential(CredentialModel credentialModel) {
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
                .map(AbstractUserEntity::getCredentials)
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
        MapUserEntity userEntity = getRegisteredEntityById(realm, userId).orElse(null);
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

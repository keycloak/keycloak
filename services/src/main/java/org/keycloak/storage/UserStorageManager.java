/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.ComponentUtil;
import org.keycloak.models.utils.ReadOnlyUserModelDelegate;
import org.keycloak.services.managers.UserStorageSyncManager;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserBulkUpdateProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

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

import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserStorageManager extends AbstractStorageManager<UserStorageProvider, UserStorageProviderModel> implements UserProvider, OnUserCache, OnCreateComponent, OnUpdateComponent {

    private static final Logger logger = Logger.getLogger(UserStorageManager.class);


    public UserStorageManager(KeycloakSession session) {
        super(session, UserStorageProviderFactory.class, UserStorageProvider.class,
                UserStorageProviderModel::new, "user");
    }

    protected UserProvider localStorage() {
        return session.userLocalStorage();
    }

    private UserFederatedStorageProvider getFederatedStorage() {
        return session.userFederatedStorage();
    }

    /**
     * Allows a UserStorageProvider to proxy and/or synchronize an imported user.
     *
     * @param realm
     * @param user
     * @return
     */
    protected UserModel importValidation(RealmModel realm, UserModel user) {
        if (user == null || user.getFederationLink() == null) return user;

        UserStorageProviderModel model = getStorageProviderModel(realm, user.getFederationLink());
        if (model == null) {
            // remove linked user with unknown storage provider.
            logger.debugf("Removed user with federation link of unknown storage provider '%s'", user.getUsername());
            deleteInvalidUser(realm, user);
            return null;
        }

        if (!model.isEnabled()) {
            return new ReadOnlyUserModelDelegate(user) {
                @Override
                public boolean isEnabled() {
                    return false;
                }
            };
        }

        ImportedUserValidation importedUserValidation = getStorageProviderInstance(model, ImportedUserValidation.class, true);
        if (importedUserValidation == null) return user;

        UserModel validated = importedUserValidation.validate(realm, user);
        if (validated == null) {
            deleteInvalidUser(realm, user);
            return null;
        } else {
            return validated;
        }
    }

    protected void deleteInvalidUser(final RealmModel realm, final UserModel user) {
        String userId = user.getId();
        String userName = user.getUsername();
        UserCache userCache = session.userCache();
        if (userCache != null) {
            userCache.evict(realm, user);
        }
        runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                RealmModel realmModel = session.realms().getRealm(realm.getId());
                if (realmModel == null) return;
                UserModel deletedUser = session.userLocalStorage().getUserById(userId, realmModel);
                if (deletedUser != null) {
                    new UserManager(session).removeUser(realmModel, deletedUser, session.userLocalStorage());
                    logger.debugf("Removed invalid user '%s'", userName);
                }
            }

        });
    }


    protected Stream<UserModel> importValidation(RealmModel realm, Stream<UserModel> users) {
        return users.map(user -> importValidation(realm, user)).filter(Objects::nonNull);
    }

    @FunctionalInterface
    interface PaginatedQuery {
        Stream<UserModel> query(Object provider);
    }

    protected Stream<UserModel> query(PaginatedQuery pagedQuery, RealmModel realm, int firstResult, int maxResults) {
        if (maxResults == 0) return Stream.empty();
        if (firstResult < 0) firstResult = 0;
        if (maxResults < 0) maxResults = Integer.MAX_VALUE - 1;

        Stream<Object> providersStream = Stream.concat(Stream.of((Object) localStorage()), getEnabledStorageProviders(realm, UserQueryProvider.class));

        UserFederatedStorageProvider federatedStorageProvider = getFederatedStorage();
        if (federatedStorageProvider != null) {
            providersStream = Stream.concat(providersStream, Stream.of(federatedStorageProvider));
        }

        return providersStream.flatMap(pagedQuery::query)
                .skip(firstResult)
                .limit(maxResults);
    }

    /**
     * distinctByKey is not supposed to be used with parallel streams
     *
     * To make this method synchronized use {@code ConcurrentHashMap<Object, Boolean>} instead of HashSet
     *
      */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = new HashSet<>();
        return t -> seen.add(keyExtractor.apply(t));
    }

    // removeDuplicates method may cause concurrent issues, it should not be used on parallel streams
    private static Stream<UserModel> removeDuplicates(Stream<UserModel> withDuplicates) {
        return withDuplicates.filter(distinctByKey(UserModel::getId));
    }

    /** {@link UserRegistrationProvider} methods implementations start here */

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return getEnabledStorageProviders(realm, UserRegistrationProvider.class)
                .map(provider -> provider.addUser(realm, username))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> localStorage().addUser(realm, username.toLowerCase()));
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        if (getFederatedStorage() != null) getFederatedStorage().preRemove(realm, user);
        StorageId storageId = new StorageId(user.getId());

        if (storageId.getProviderId() == null) {
            String federationLink = user.getFederationLink();
            boolean linkRemoved = federationLink == null || Optional.ofNullable(
                    getStorageProviderInstance(realm, federationLink, UserRegistrationProvider.class))
                    .map(provider -> provider.removeUser(realm, user))
                    .orElse(false);

            return localStorage().removeUser(realm, user) && linkRemoved;
        }

        UserRegistrationProvider registry = getStorageProviderInstance(realm, storageId.getProviderId(), UserRegistrationProvider.class);
        if (registry == null) {
            throw new ModelException("Could not resolve UserRegistrationProvider: " + storageId.getProviderId());
        }

        return registry.removeUser(realm, user);
    }

    /** {@link UserRegistrationProvider} methods implementations end here
        {@link UserLookupProvider} methods implementations start here */

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        if (storageId.getProviderId() == null) {
            UserModel user = localStorage().getUserById(id, realm);
            return importValidation(realm, user);
        }

        UserLookupProvider provider = getStorageProviderInstance(realm, storageId.getProviderId(), UserLookupProvider.class);
        if (provider == null) return null;

        return provider.getUserById(id, realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        UserModel user = localStorage().getUserByUsername(username, realm);
        if (user != null) {
            return importValidation(realm, user);
        }

        return mapEnabledStorageProvidersWithTimeout(realm, UserLookupProvider.class,
                provider -> provider.getUserByUsername(username, realm)).findFirst().orElse(null);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        UserModel user = localStorage().getUserByEmail(email, realm);
        if (user != null) {
            user = importValidation(realm, user);
            // Case when email was changed directly in the userStorage and doesn't correspond anymore to the email from local DB
            if (email.equalsIgnoreCase(user.getEmail())) {
                return user;
            }
        }

        return mapEnabledStorageProvidersWithTimeout(realm, UserLookupProvider.class,
                provider -> provider.getUserByEmail(email, realm)).findFirst().orElse(null);
    }

    /** {@link UserLookupProvider} methods implementations end here
        {@link UserQueryProvider} methods implementation start here */

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getGroupMembers(realm, group, -1, -1);
    }

    @Override
    public List<UserModel> getGroupMembers(final RealmModel realm, final GroupModel group, int firstResult, int maxResults) {
        Stream<UserModel> results = query((provider) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).getGroupMembers(realm, group).stream();

            } else if (provider instanceof UserFederatedStorageProvider) {
                Stream<String> ids = ((UserFederatedStorageProvider)provider).getMembership(realm, group, -1, -1).stream();
                return ids.map(id -> getUserById(id, realm));
           }
            return Stream.empty();
        }, realm, firstResult, maxResults);

        return importValidation(realm, results).collect(Collectors.toList());
    }

    @Override
    public List<UserModel> getRoleMembers(RealmModel realm, RoleModel role) {
        return getRoleMembers(realm, role, -1, -1);
    }

    @Override
    public List<UserModel> getRoleMembers(final RealmModel realm, final RoleModel role, int firstResult, int maxResults) {
        Stream<UserModel> results = query((provider) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).getRoleMembers(realm, role).stream();
            }
            return Stream.empty();
        }, realm, firstResult, maxResults);
        return importValidation(realm, results).collect(Collectors.toList());
    }


    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, false);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return getUsers(realm, firstResult, maxResults, false);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
        return getUsers(realm, 0, Integer.MAX_VALUE - 1, includeServiceAccounts);
    }

    @Override
    public List<UserModel> getUsers(final RealmModel realm, int firstResult, int maxResults, final boolean includeServiceAccounts) {
        Stream<UserModel> results =  query((provider) -> {
                    if (provider instanceof UserProvider) { // it is local storage
                        return ((UserProvider) provider).getUsers(realm, includeServiceAccounts).stream();
                    } else if (provider instanceof UserQueryProvider) {
                        return ((UserQueryProvider)provider).getUsers(realm).stream();
                    }
                    return Stream.empty();
                }
                , realm, firstResult, maxResults);
        return importValidation(realm, results).collect(Collectors.toList());
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        int localStorageUsersCount = localStorage().getUsersCount(realm, includeServiceAccount);
        int storageProvidersUsersCount = mapEnabledStorageProvidersWithTimeout(realm, UserQueryProvider.class,
                userQueryProvider -> userQueryProvider.getUsersCount(realm))
                .reduce(0, Integer::sum);

        return localStorageUsersCount + storageProvidersUsersCount;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return getUsersCount(realm, false);
    }

    @Override // TODO: missing storageProviders count?
    public int getUsersCount(RealmModel realm, Set<String> groupIds) {
        return localStorage().getUsersCount(realm, groupIds);
    }

    @Override // TODO: missing storageProviders count?
    public int getUsersCount(String search, RealmModel realm) {
        return localStorage().getUsersCount(search, realm);
    }

    @Override // TODO: missing storageProviders count?
    public int getUsersCount(String search, RealmModel realm, Set<String> groupIds) {
        return localStorage().getUsersCount(search, realm, groupIds);
    }

    @Override // TODO: missing storageProviders count?
    public int getUsersCount(Map<String, String> params, RealmModel realm) {
        return localStorage().getUsersCount(params, realm);
    }

    @Override // TODO: missing storageProviders count?
    public int getUsersCount(Map<String, String> params, RealmModel realm, Set<String> groupIds) {
        return localStorage().getUsersCount(params, realm, groupIds);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        Stream<UserModel> results = query((provider) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).searchForUser(search, realm).stream();

            }
            return Stream.empty();
        }, realm, firstResult, maxResults);
        return importValidation(realm, results).collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return searchForUser(attributes, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        Stream<UserModel> results = query((provider) -> {
                    if (provider instanceof UserQueryProvider) {
                        if (attributes.containsKey(UserModel.SEARCH)) {
                            return ((UserQueryProvider)provider).searchForUser(attributes.get(UserModel.SEARCH), realm).stream();
                        } else {
                            return ((UserQueryProvider)provider).searchForUser(attributes, realm).stream();
                        }
                    }
                    return Stream.empty();
                }
                , realm, firstResult, maxResults);
        return importValidation(realm, results).collect(Collectors.toList());

    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        Stream<UserModel> results = query((provider) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).searchForUserByUserAttribute(attrName, attrValue, realm).stream();
            } else if (provider instanceof UserFederatedStorageProvider) {
                return  ((UserFederatedStorageProvider)provider).getUsersByUserAttribute(realm, attrName, attrValue).stream()
                        .map(id -> getUserById(id, realm))
                        .filter(Objects::nonNull);

            }
            return Stream.empty();
        }, realm,0, Integer.MAX_VALUE - 1);

        // removeDuplicates method may cause concurrent issues, it should not be used on parallel streams
        results = removeDuplicates(results);

        return importValidation(realm, results).collect(Collectors.toList());
    }

    /** {@link UserQueryProvider} methods implementation end here
        {@link UserBulkUpdateProvider} methods implementation start here */

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        localStorage().grantToAllUsers(realm, role);
        consumeEnabledStorageProvidersWithTimeout(realm, UserBulkUpdateProvider.class,
                provider -> provider.grantToAllUsers(realm, role));
    }

    /** {@link UserBulkUpdateProvider} methods implementation end here
        {@link UserStorageProvider} methods implementations start here -> no StorageProviders involved */

    @Override
    public void preRemove(RealmModel realm) {
        localStorage().preRemove(realm);

        if (getFederatedStorage() != null) {
            getFederatedStorage().preRemove(realm);
        }

        consumeEnabledStorageProvidersWithTimeout(realm, UserStorageProvider.class,
                provider -> provider.preRemove(realm));
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        localStorage().preRemove(realm, group);

        if (getFederatedStorage() != null) {
            getFederatedStorage().preRemove(realm, group);
        }

        consumeEnabledStorageProvidersWithTimeout(realm, UserStorageProvider.class,
                provider -> provider.preRemove(realm, group));
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        localStorage().preRemove(realm, role);

        if (getFederatedStorage() != null) {
            getFederatedStorage().preRemove(realm, role);
        }

        consumeEnabledStorageProvidersWithTimeout(realm, UserStorageProvider.class, provider -> provider.preRemove(realm, role));
    }

    /** {@link UserStorageProvider} methods implementation end here
     {@link UserProvider} methods implementations start here -> no StorageProviders involved */

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        return localStorage().addUser(realm, id, username.toLowerCase(), addDefaultRoles, addDefaultRequiredActions);
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        if (StorageId.isLocalStorage(user)) {
            localStorage().addFederatedIdentity(realm, user, socialLink);
        } else {
            getFederatedStorage().addFederatedIdentity(realm, user.getId(), socialLink);
        }
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        if (StorageId.isLocalStorage(federatedUser)) {
            localStorage().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);
        } else {
            getFederatedStorage().updateFederatedIdentity(realm, federatedUser.getId(), federatedIdentityModel);
        }
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        if (StorageId.isLocalStorage(user)) {
            return localStorage().removeFederatedIdentity(realm, user, socialProvider);
        } else {
            return getFederatedStorage().removeFederatedIdentity(realm, user.getId(), socialProvider);
        }
    }

    @Override
    public void preRemove(RealmModel realm, IdentityProviderModel provider) {
        localStorage().preRemove(realm, provider);
        getFederatedStorage().preRemove(realm, provider);
    }


    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        if (StorageId.isLocalStorage(userId)) {
            localStorage().addConsent(realm, userId, consent);
        } else {
            getFederatedStorage().addConsent(realm, userId, consent);
        }

    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientInternalId) {
        if (StorageId.isLocalStorage(userId)) {
            return localStorage().getConsentByClient(realm, userId, clientInternalId);
        } else {
            return getFederatedStorage().getConsentByClient(realm, userId, clientInternalId);
        }
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, String userId) {
        if (StorageId.isLocalStorage(userId)) {
            return localStorage().getConsents(realm, userId);
        } else {
            return getFederatedStorage().getConsents(realm, userId);
        }
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        if (StorageId.isLocalStorage(userId)) {
            localStorage().updateConsent(realm, userId, consent);
        } else {
            getFederatedStorage().updateConsent(realm, userId, consent);
        }

    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId) {
        if (StorageId.isLocalStorage(userId)) {
            return localStorage().revokeConsentForClient(realm, userId, clientInternalId);
        } else {
            return getFederatedStorage().revokeConsentForClient(realm, userId, clientInternalId);
        }
    }

    @Override
    public void setNotBeforeForUser(RealmModel realm, UserModel user, int notBefore) {
        if (StorageId.isLocalStorage(user)) {
            localStorage().setNotBeforeForUser(realm, user, notBefore);
        } else {
            getFederatedStorage().setNotBeforeForUser(realm, user.getId(), notBefore);
        }
    }

    @Override
    public int getNotBeforeOfUser(RealmModel realm, UserModel user) {
        if (StorageId.isLocalStorage(user)) {
            return localStorage().getNotBeforeOfUser(realm, user);
        } else {
            return getFederatedStorage().getNotBeforeOfUser(realm, user.getId());
        }
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        UserModel user = localStorage().getUserByFederatedIdentity(socialLink, realm);
        if (user != null) {
            return importValidation(realm, user);
        }
        if (getFederatedStorage() == null) return null;
        String id = getFederatedStorage().getUserByFederatedIdentity(socialLink, realm);
        if (id != null) return getUserById(id, realm);
        return null;
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        return localStorage().getServiceAccount(client);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        Set<FederatedIdentityModel> set = new HashSet<>();
        if (StorageId.isLocalStorage(user)) {
            set.addAll(localStorage().getFederatedIdentities(user, realm));
        }
        if (getFederatedStorage() != null) set.addAll(getFederatedStorage().getFederatedIdentities(user.getId(), realm));
        return set;
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        if (StorageId.isLocalStorage(user)) {
            FederatedIdentityModel model = localStorage().getFederatedIdentity(user, socialProvider, realm);
            if (model != null) return model;
        }
        if (getFederatedStorage() != null) return getFederatedStorage().getFederatedIdentity(user.getId(), socialProvider, realm);
        else return null;
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        localStorage().preRemove(realm, client);
        if (getFederatedStorage() != null) getFederatedStorage().preRemove(realm, client);

    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        localStorage().preRemove(protocolMapper);
        if (getFederatedStorage() != null) getFederatedStorage().preRemove(protocolMapper);
    }

    @Override
    public void preRemove(ClientScopeModel clientScope) {
        localStorage().preRemove(clientScope);
        if (getFederatedStorage() != null) getFederatedStorage().preRemove(clientScope);
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
        if (component.getProviderType().equals(ClientStorageProvider.class.getName())) {
            localStorage().preRemove(realm, component);
            if (getFederatedStorage() != null) getFederatedStorage().preRemove(realm, component);
            return;
        }
        if (!component.getProviderType().equals(UserStorageProvider.class.getName())) return;
        localStorage().preRemove(realm, component);
        if (getFederatedStorage() != null) getFederatedStorage().preRemove(realm, component);
        new UserStorageSyncManager().notifyToRefreshPeriodicSync(session, realm, new UserStorageProviderModel(component), true);

    }

    @Override
    public void removeImportedUsers(RealmModel realm, String storageProviderId) {
        localStorage().removeImportedUsers(realm, storageProviderId);
    }

    @Override
    public void unlinkUsers(RealmModel realm, String storageProviderId) {
        localStorage().unlinkUsers(realm, storageProviderId);
    }

    /** {@link UserProvider} methods implementations end here */

    @Override
    public void close() {
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        ComponentFactory factory = ComponentUtil.getComponentFactory(session, model);
        if (!(factory instanceof UserStorageProviderFactory)) return;
        new UserStorageSyncManager().notifyToRefreshPeriodicSync(session, realm, new UserStorageProviderModel(model), false);

    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        ComponentFactory factory = ComponentUtil.getComponentFactory(session, newModel);
        if (!(factory instanceof UserStorageProviderFactory)) return;
        UserStorageProviderModel old = new UserStorageProviderModel(oldModel);
        UserStorageProviderModel newP= new UserStorageProviderModel(newModel);
        if (old.getChangedSyncPeriod() != newP.getChangedSyncPeriod() || old.getFullSyncPeriod() != newP.getFullSyncPeriod()
                || old.isImportEnabled() != newP.isImportEnabled()) {
            new UserStorageSyncManager().notifyToRefreshPeriodicSync(session, realm, new UserStorageProviderModel(newModel), false);
        }

    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        if (StorageId.isLocalStorage(user)) {
            if (session.userLocalStorage() instanceof OnUserCache) {
                ((OnUserCache)session.userLocalStorage()).onCache(realm, user, delegate);
            }
        } else {
            OnUserCache provider = getStorageProviderInstance(realm, StorageId.resolveProviderId(user), OnUserCache.class);
            if (provider != null ) {
                provider.onCache(realm, user, delegate);
            }
        }
    }
}

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

import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;
import static org.keycloak.utils.StreamsUtil.distinctByKey;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.component.ComponentFactory;
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
import org.keycloak.models.search.SearchQueryJson;
import org.keycloak.models.utils.ComponentUtil;
import org.keycloak.models.utils.ReadOnlyUserModelDelegate;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.datastore.LegacyDatastoreProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.managers.UserStorageSyncManager;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserBulkUpdateProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserStorageManager extends AbstractStorageManager<UserStorageProvider, UserStorageProviderModel>
        implements UserProvider, OnUserCache, OnCreateComponent, OnUpdateComponent {

    private static final Logger logger = Logger.getLogger(UserStorageManager.class);


    public UserStorageManager(KeycloakSession session) {
        super(session, UserStorageProviderFactory.class, UserStorageProvider.class,
                UserStorageProviderModel::new, "user");
    }

    protected UserProvider localStorage() {
        return ((LegacyDatastoreProvider) session.getProvider(DatastoreProvider.class)).userLocalStorage();
    }

    private UserFederatedStorageProvider getFederatedStorage() {
        return UserStorageUtil.userFederatedStorage(session);
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

    private static <T> Stream<T> getCredentialProviders(KeycloakSession session, Class<T> type) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(CredentialProvider.class)
                .filter(f -> Types.supports(type, f, CredentialProviderFactory.class))
                .map(f -> (T) session.getProvider(CredentialProvider.class, f.getId()));
    }

    @Override
    public CredentialValidationOutput getUserByCredential(RealmModel realm, CredentialInput input) {
        Stream<CredentialAuthentication> credentialAuthenticationStream = getEnabledStorageProviders(realm, CredentialAuthentication.class);

        credentialAuthenticationStream = Stream.concat(credentialAuthenticationStream,
                getCredentialProviders(session, CredentialAuthentication.class));

        return credentialAuthenticationStream
                .filter(credentialAuthentication -> credentialAuthentication.supportsCredentialAuthenticationFor(input.getType()))
                .map(credentialAuthentication -> credentialAuthentication.authenticate(realm, input))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    protected void deleteInvalidUser(final RealmModel realm, final UserModel user) {
        String userId = user.getId();
        String userName = user.getUsername();
        UserCache userCache = UserStorageUtil.userCache(session);
        if (userCache != null) {
            userCache.evict(realm, user);
        }

        // This needs to be running in separate transaction because removing the user may end up with throwing
        // PessimisticLockException which also rollbacks Jpa transaction, hence when it is running in current transaction
        // it will become not usable for all consequent jpa calls. It will end up with Transaction is in rolled back
        // state error
        runJobInTransaction(session.getKeycloakSessionFactory(), session -> {
            RealmModel realmModel = session.realms().getRealm(realm.getId());
            if (realmModel == null) return;
            UserModel deletedUser = UserStoragePrivateUtil.userLocalStorage(session).getUserById(realmModel, userId);
            if (deletedUser != null) {
                try {
                    new UserManager(session).removeUser(realmModel, deletedUser, UserStoragePrivateUtil.userLocalStorage(session));
                    logger.debugf("Removed invalid user '%s'", userName);
                } catch (ModelException ex) {
                    // Ignore exception, possible cause may be concurrent deleteInvalidUser calls which means
                    // ModelException exception may be ignored because users will be removed with next call or is
                    // already removed
                    logger.debugf(ex, "ModelException thrown during deleteInvalidUser with username '%s'", userName);
                }
            }
        });
    }


    protected Stream<UserModel> importValidation(RealmModel realm, Stream<UserModel> users) {
        return users.map(user -> importValidation(realm, user)).filter(Objects::nonNull);
    }

    @FunctionalInterface
    interface PaginatedQuery {
        Stream<UserModel> query(Object provider, Integer firstResult, Integer maxResults);
    }

    @FunctionalInterface
    interface CountQuery {
        int query(Object provider, Integer firstResult, Integer maxResult);
    }

    protected Stream<UserModel> query(PaginatedQuery pagedQuery, RealmModel realm, Integer firstResult, Integer maxResults) {
        return query(pagedQuery, ((provider, first, max) -> (int) pagedQuery.query(provider, first, max).count()), realm, firstResult, maxResults);
    }

    protected Stream<UserModel> query(PaginatedQuery pagedQuery, CountQuery countQuery, RealmModel realm, Integer firstResult, Integer maxResults) {
        if (maxResults != null && maxResults == 0) return Stream.empty();

        Stream<Object> providersStream = Stream.concat(Stream.of((Object) localStorage()), getEnabledStorageProviders(realm, UserQueryProvider.class));

        UserFederatedStorageProvider federatedStorageProvider = getFederatedStorage();
        if (federatedStorageProvider != null) {
            providersStream = Stream.concat(providersStream, Stream.of(federatedStorageProvider));
        }

        final AtomicInteger currentFirst;

        if (firstResult == null || firstResult <= 0) { // We don't want to skip any users so we don't need to do firstResult filtering
            currentFirst = new AtomicInteger(0);
        } else {
            AtomicBoolean droppingProviders = new AtomicBoolean(true);
            currentFirst = new AtomicInteger(firstResult);

            providersStream = providersStream
                .filter(provider -> { // This is basically dropWhile
                    if (!droppingProviders.get()) return true; // We have already gathered enough users to pass firstResult number in previous providers, we can take all following providers

                    long expectedNumberOfUsersForProvider = countQuery.query(provider, 0, currentFirst.get() + 1); // check how many users we can obtain from this provider

                    if (expectedNumberOfUsersForProvider == currentFirst.get()) { // This provider provides exactly the amount of users we need for passing firstResult, we can set currentFirst to 0 and drop this provider
                        currentFirst.set(0);
                        droppingProviders.set(false);
                        return false;
                    }

                    if (expectedNumberOfUsersForProvider > currentFirst.get()) { // If we can obtain enough enough users from this provider to fulfill our need we can stop dropping providers
                        droppingProviders.set(false);
                        return true; // don't filter out this provider because we are going to return some users from it
                    }

                    // This provider cannot provide enough users to pass firstResult so we are going to filter it out and change firstResult for next provider
                    currentFirst.set((int) (currentFirst.get() - expectedNumberOfUsersForProvider));
                    return false;
                });
        }

        // Actual user querying
        if (maxResults == null || maxResults < 0) {
            // No maxResult set, we want all users
            return providersStream
                    .flatMap(provider -> pagedQuery.query(provider, currentFirst.getAndSet(0), null));
        } else {
            final AtomicInteger currentMax = new AtomicInteger(maxResults);

            // Query users with currentMax variable counting how many users we return
            return providersStream
                    .filter(provider -> currentMax.get() != 0) // If we reach currentMax == 0, we can skip querying all following providers
                    .flatMap(provider -> pagedQuery.query(provider, currentFirst.getAndSet(0), currentMax.get()))
                    .peek(userModel -> {
                        currentMax.updateAndGet(i -> i > 0 ? i - 1 : i);
                    });
        }

    }

    // removeDuplicates method may cause concurrent issues, it should not be used on parallel streams
    private static Stream<UserModel> removeDuplicates(Stream<UserModel> withDuplicates) {
        return withDuplicates.filter(distinctByKey(UserModel::getId));
    }

    /** {@link UserRegistrationProvider} methods implementations start here */

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        if (username.startsWith(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX)) {
            // Don't use federation for service account user
            return localStorage().addUser(realm, username);
        }

        return getEnabledStorageProviders(realm, UserRegistrationProvider.class)
                .map(provider -> provider.addUser(realm, username))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> localStorage().addUser(realm, username.toLowerCase()));
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        if (getFederatedStorage() != null && user.getServiceAccountClientLink() == null) {
            getFederatedStorage().preRemove(realm, user);
        }

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
    public UserModel getUserById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        if (storageId.getProviderId() == null) {
            UserModel user = localStorage().getUserById(realm, id);
            return importValidation(realm, user);
        }

        UserLookupProvider provider = getStorageProviderInstance(realm, storageId.getProviderId(), UserLookupProvider.class);
        if (provider == null) return null;

        return provider.getUserById(realm, id);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        UserModel user = localStorage().getUserByUsername(realm, username);
        if (user != null) {
            return importValidation(realm, user);
        }

        return mapEnabledStorageProvidersWithTimeout(realm, UserLookupProvider.class,
                provider -> provider.getUserByUsername(realm, username)).findFirst().orElse(null);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        UserModel user = localStorage().getUserByEmail(realm, email);
        if (user != null) {
            user = importValidation(realm, user);
            // Case when email was changed directly in the userStorage and doesn't correspond anymore to the email from local DB
            if (email.equalsIgnoreCase(user.getEmail())) {
                return user;
            }
        }

        return mapEnabledStorageProvidersWithTimeout(realm, UserLookupProvider.class,
                provider -> provider.getUserByEmail(realm, email)).findFirst().orElse(null);
    }

    /** {@link UserLookupProvider} methods implementations end here
        {@link UserQueryProvider} methods implementation start here */

    @Override
    public Stream<UserModel> getGroupMembersStream(final RealmModel realm, final GroupModel group, Integer firstResult, Integer maxResults) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).getGroupMembersStream(realm, group, firstResultInQuery, maxResultsInQuery);

            } else if (provider instanceof UserFederatedStorageProvider) {
                return ((UserFederatedStorageProvider)provider).getMembershipStream(realm, group, firstResultInQuery, maxResultsInQuery).
                        map(id -> getUserById(realm, id));
           }
            return Stream.empty();
        }, realm, firstResult, maxResults);

        return importValidation(realm, results);
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(final RealmModel realm, final RoleModel role, Integer firstResult, Integer maxResults) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).getRoleMembersStream(realm, role, firstResultInQuery, maxResultsInQuery);
            }
            return Stream.empty();
        }, realm, firstResult, maxResults);
        return importValidation(realm, results);
    }


    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm) {
        return getUsersStream(realm, null, null, false);
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return getUsersStream(realm, firstResult, maxResults, false);
    }

    @Override
    public Stream<UserModel> getUsersStream(final RealmModel realm, Integer firstResult, Integer maxResults, final boolean includeServiceAccounts) {
        Stream<UserModel> results =  query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserProvider) { // it is local storage
                return ((UserProvider) provider).getUsersStream(realm, firstResultInQuery, maxResultsInQuery, includeServiceAccounts);
            } else if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).getUsersStream(realm);
            }
            return Stream.empty();
        }
        , realm, firstResult, maxResults);
        return importValidation(realm, results);
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
    public int getUsersCount(RealmModel realm, String search) {
        return localStorage().getUsersCount(realm, search);
    }

    @Override // TODO: missing storageProviders count?
    public int getUsersCount(RealmModel realm, String search, Set<String> groupIds) {
        return localStorage().getUsersCount(realm, search, groupIds);
    }

    @Override // TODO: missing storageProviders count?
    public int getUsersCount(RealmModel realm, Map<String, String> params) {
        return localStorage().getUsersCount(realm, params);
    }

    @Override // TODO: missing storageProviders count?
    public int getUsersCount(RealmModel realm, Map<String, String> params, Set<String> groupIds) {
        return localStorage().getUsersCount(realm, params, groupIds);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).searchForUserStream(realm, search, firstResultInQuery, maxResultsInQuery);
            }
            return Stream.empty();
        }, (provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).getUsersCount(realm, search);
            }
            return 0;
        }, realm, firstResult, maxResults);
        return importValidation(realm, results);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryProvider) {
                if (attributes.containsKey(UserModel.SEARCH)) {
                    return ((UserQueryProvider)provider).searchForUserStream(realm, attributes.get(UserModel.SEARCH), firstResultInQuery, maxResultsInQuery);
                } else {
                    return ((UserQueryProvider)provider).searchForUserStream(realm, attributes, firstResultInQuery, maxResultsInQuery);
                }
            }
            return Stream.empty();
        },
        (provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryProvider) {
                if (attributes.containsKey(UserModel.SEARCH)) {
                    return ((UserQueryProvider)provider).getUsersCount(realm, attributes.get(UserModel.SEARCH));
                } else {
                    return ((UserQueryProvider)provider).getUsersCount(realm, attributes);
                }
            }
            return 0;
        }
        , realm, firstResult, maxResults);
        return importValidation(realm, results);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, SearchQueryJson query, Integer firstResult, Integer maxResults) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).searchForUserStream(realm, query, firstResultInQuery, maxResultsInQuery);
            }
            return Stream.empty();
        },
        (provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).getUsersCount(realm, query);
            }
            return 0;
        }
        , realm, firstResult, maxResults);
        return importValidation(realm, results);
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryProvider) {
                return paginatedStream(((UserQueryProvider)provider).searchForUserByUserAttributeStream(realm, attrName, attrValue), firstResultInQuery, maxResultsInQuery);
            } else if (provider instanceof UserFederatedStorageProvider) {
                return  paginatedStream(((UserFederatedStorageProvider)provider).getUsersByUserAttributeStream(realm, attrName, attrValue)
                        .map(id -> getUserById(realm, id))
                        .filter(Objects::nonNull), firstResultInQuery, maxResultsInQuery);

            }
            return Stream.empty();
        }, realm, null, null);

        // removeDuplicates method may cause concurrent issues, it should not be used on parallel streams
        results = removeDuplicates(results);

        return importValidation(realm, results);
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
    public Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId) {
        if (StorageId.isLocalStorage(userId)) {
            return localStorage().getConsentsStream(realm, userId);
        } else {
            return getFederatedStorage().getConsentsStream(realm, userId);
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
    public UserModel getUserByFederatedIdentity(RealmModel realm, FederatedIdentityModel socialLink) {
        UserModel user = localStorage().getUserByFederatedIdentity(realm, socialLink);
        if (user != null) {
            return importValidation(realm, user);
        }
        if (getFederatedStorage() == null) return null;
        String id = getFederatedStorage().getUserByFederatedIdentity(socialLink, realm);
        if (id != null) return getUserById(realm, id);
        return null;
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        return localStorage().getServiceAccount(client);
    }

    @Override
    public Stream<FederatedIdentityModel> getFederatedIdentitiesStream(RealmModel realm, UserModel user) {
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        Stream<FederatedIdentityModel> stream = StorageId.isLocalStorage(user) ?
                localStorage().getFederatedIdentitiesStream(realm, user) : Stream.empty();
        if (getFederatedStorage() != null)
            stream = Stream.concat(stream, getFederatedStorage().getFederatedIdentitiesStream(user.getId(), realm));
        return stream.distinct();
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        if (StorageId.isLocalStorage(user)) {
            FederatedIdentityModel model = localStorage().getFederatedIdentity(realm, user, socialProvider);
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
            if (UserStoragePrivateUtil.userLocalStorage(session) instanceof OnUserCache) {
                ((OnUserCache)UserStoragePrivateUtil.userLocalStorage(session)).onCache(realm, user, delegate);
            }
        } else {
            OnUserCache provider = getStorageProviderInstance(realm, StorageId.resolveProviderId(user), OnUserCache.class);
            if (provider != null ) {
                provider.onCache(realm, user, delegate);
            }
        }
    }
}

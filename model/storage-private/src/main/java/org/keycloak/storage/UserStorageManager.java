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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialAuthentication;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.AbstractKeycloakTransaction;
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
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.ComponentUtil;
import org.keycloak.models.utils.ReadOnlyUserModelDelegate;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.datastore.DefaultDatastoreProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserBulkUpdateProvider;
import org.keycloak.storage.user.UserCountMethodsProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryMethodsProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfileDecorator;
import org.keycloak.userprofile.UserProfileMetadata;
import org.keycloak.utils.StreamsUtil;
import org.keycloak.utils.StringUtil;

import io.opentelemetry.api.trace.StatusCode;
import org.jboss.logging.Logger;

import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;
import static org.keycloak.storage.managers.UserStorageSyncManager.notifyToRefreshPeriodicSync;
import static org.keycloak.utils.StreamsUtil.distinctByKey;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserStorageManager extends AbstractStorageManager<UserStorageProvider, UserStorageProviderModel>
        implements UserProvider, OnUserCache, OnCreateComponent, OnUpdateComponent, UserProfileDecorator {

    private static final Logger logger = Logger.getLogger(UserStorageManager.class);


    public UserStorageManager(KeycloakSession session) {
        super(session, UserStorageProviderFactory.class, UserStorageProvider.class,
                UserStorageProviderModel::new, "user");
    }

    protected UserProvider localStorage() {
        return ((DefaultDatastoreProvider) session.getProvider(DatastoreProvider.class)).userLocalStorage();
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
    protected UserModel validateUser(RealmModel realm, UserModel user) {
        if (user == null) {
            return null;
        }

        if (user.isFederated()) {
            user = validateFederatedUser(realm, user);
        }

        if (isReadOnlyOrganizationMember(user)) {
            if (user instanceof CachedUserModel cachedUserModel) {
                cachedUserModel.invalidate();
            }
            return new ReadOnlyUserModelDelegate(user, false);
        }

        return user;
    }

    private UserModel validateFederatedUser(RealmModel realm, UserModel user) {
        if (!user.isFederated()) {
            return user;
        }

        UserStorageProviderModel model = getUserStorageProviderModel(realm, user);

        if (model == null) {
            return null;
        }

        if (!model.isEnabled()) {
            if (user instanceof CachedUserModel cachedUserModel) {
                cachedUserModel.invalidate();
            }
            return new ReadOnlyUserModelDelegate(user, false);
        }

        if (user instanceof CachedUserModel) {
            // if the user is cached do not validate import for the cached configured time
            return user;
        }

        ImportedUserValidation validator = getStorageProviderInstance(model, ImportedUserValidation.class, true);

        if (validator == null) {
            return user;
        }

        try {
            UserModel validated = validator.validate(realm, user);

            if (validated == null) {
                return deleteFederatedUser(realm, user);
            }

            return validated;
        } catch (Exception e) {
            logger.warnf(e, "User storage provider %s failed during federated user validation", model.getName());
            return new ReadOnlyUserModelDelegate(user, false, ignore -> new ReadOnlyException("The user is read-only. The user storage provider '" + model.getName() + "' is currently unavailable. Check the server logs for more details."));
        }
    }

    private ReadOnlyUserModelDelegate deleteFederatedUser(RealmModel realm, UserModel user) {
        if (!user.isFederated()) {
            return null;
        }

        UserStorageProviderModel model = getUserStorageProviderModel(realm, user);

        if (model == null) {
            return null;
        }

        deleteInvalidUserCache(realm, user);

        if (model.isRemoveInvalidUsersEnabled()) {
            deleteInvalidUser(realm, user);
            return null;
        }

        return new ReadOnlyUserModelDelegate(user, false);
    }

    private UserStorageProviderModel getUserStorageProviderModel(RealmModel realm, UserModel user) {
        if (user.isFederated()) {
            UserStorageProviderModel model = getStorageProviderModel(realm, user.getFederationLink());

            if (model == null) {
                // remove linked user with unknown storage provider.
                logger.debugf("Removed user with federation link of unknown storage provider '%s'", user.getUsername());
                deleteInvalidUserCache(realm, user);
                deleteInvalidUser(realm, user);
            }

            return model;
        }

        return null;
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

        CredentialValidationOutput result = null;
        for (CredentialAuthentication credentialAuthentication : credentialAuthenticationStream
                .filter(credentialAuthentication -> credentialAuthentication.supportsCredentialAuthenticationFor(input.getType()))
                .collect(Collectors.toList())) {
            CredentialValidationOutput validationOutput = session.getProvider(TracingProvider.class).trace(credentialAuthentication.getClass(), "authenticate",
                    span -> {
                        CredentialValidationOutput output = credentialAuthentication.authenticate(realm, input);
                        if (span.isRecording()) {
                            if (output != null) {
                                CredentialValidationOutput.Status status = output.getAuthStatus();
                                span.setAttribute("kc.validationStatus", status.name());
                                if (status == CredentialValidationOutput.Status.FAILED) {
                                    span.setStatus(StatusCode.ERROR);
                                }
                            }
                        }
                        return output;
                    }
            );
            if (Objects.nonNull(validationOutput)) {
                CredentialValidationOutput.Status status = validationOutput.getAuthStatus();
                if (status == CredentialValidationOutput.Status.AUTHENTICATED || status == CredentialValidationOutput.Status.CONTINUE || status == CredentialValidationOutput.Status.FAILED) {
                    logger.tracef("Attempt to authenticate credential '%s' with provider '%s' finished with '%s'.", input.getType(), credentialAuthentication, status);
                    if (status == CredentialValidationOutput.Status.AUTHENTICATED) {
                        logger.tracef("Authenticated user is '%s'", validationOutput.getAuthenticatedUser().getUsername());
                    }
                    result = validationOutput;
                    break;
                }
            }
            logger.tracef("Did not authenticate user by provider '%s' with the credential type '%s'. Will try to fallback to other user storage providers", credentialAuthentication, input.getType());
        }
        return result;
    }

    protected void deleteInvalidUserCache(final RealmModel realm, final UserModel user) {
        UserCache userCache = UserStorageUtil.userCache(session);
        if (userCache != null) {
            userCache.evict(realm, user);
        }
    }

    protected void deleteInvalidUser(final RealmModel realm, final UserModel user) {
        String userId = user.getId();
        String userName = user.getUsername();
        // This needs to be running in separate transaction because removing the user may end up with throwing
        // PessimisticLockException which also rollbacks Jpa transaction, hence when it is running in current transaction
        // it will become not usable for all consequent jpa calls. It will end up with Transaction is in rolled back
        // state error
        runJobInTransaction(session.getKeycloakSessionFactory(), session -> {
            RealmModel realmModel = session.realms().getRealm(realm.getId());
            if (realmModel == null) return;
            session.getContext().setRealm(realm);
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
        return users.map(user -> validateUser(realm, user)).filter(Objects::nonNull);
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

        Stream<Object> providersStream = Stream.concat(Stream.of((Object) localStorage()), getEnabledStorageProviders(realm, UserQueryMethodsProvider.class));

        UserFederatedStorageProvider federatedStorageProvider = getFederatedStorage();
        if (federatedStorageProvider != null) {
            providersStream = Stream.concat(providersStream, Stream.of(federatedStorageProvider));
        }

        final AtomicInteger currentFirst;
        final AtomicBoolean needsAdditionalFirstResultFiltering = new AtomicBoolean(false);

        if (firstResult == null || firstResult <= 0) { // We don't want to skip any users so we don't need to do firstResult filtering
            currentFirst = new AtomicInteger(0);
        } else {
            // This is an optimization using count query to skip querying users if we can use count method to determine how many users can be provided by each provider
            AtomicBoolean droppingProviders = new AtomicBoolean(true);
            currentFirst = new AtomicInteger(firstResult);

            providersStream = providersStream
                .filter(provider -> { // This is basically dropWhile
                    if (!droppingProviders.get()) return true; // We have already gathered enough users to pass firstResult number in previous providers, we can take all following providers

                    if (!(provider instanceof UserCountMethodsProvider)) {
                        logger.tracef("We encountered a provider (%s) that does not implement count queries therefore we can't say how many users it can provide.", provider.getClass().getSimpleName());
                        // for this reason we need to start querying this provider and all following providers
                        droppingProviders.set(false);
                        needsAdditionalFirstResultFiltering.set(true);
                        return true; // don't filter out this provider because we are unable to say how many users it can provide
                    }

                    long expectedNumberOfUsersForProvider = countQueryWithGracefulDegradation(provider, countQuery, 0, currentFirst.get() + 1); // check how many users we can obtain from this provider
                    logger.tracef("This provider (%s) is able to return %d users.", provider.getClass().getSimpleName(), expectedNumberOfUsersForProvider);

                    if (expectedNumberOfUsersForProvider == currentFirst.get()) { // This provider provides exactly the amount of users we need for passing firstResult, we can set currentFirst to 0 and drop this provider
                        currentFirst.set(0);
                        droppingProviders.set(false);
                        return false;
                    }

                    if (expectedNumberOfUsersForProvider > currentFirst.get()) { // If we can obtain enough enough users from this provider to fulfill our need we can stop dropping providers
                        droppingProviders.set(false);
                        return true; // don't filter out this provider because we are going to return some users from it
                    }

                    logger.tracef("This provider (%s) cannot provide enough users to pass firstResult so we are going to filter it out and change "
                            + "firstResult for next provider: %d - %d = %d", provider.getClass().getSimpleName(),
                            currentFirst.get(), expectedNumberOfUsersForProvider, currentFirst.get() - expectedNumberOfUsersForProvider);
                    currentFirst.set((int) (currentFirst.get() - expectedNumberOfUsersForProvider));
                    return false;
                })
                // collecting stream of providers to ensure the filtering (above) is evaluated before we move forward to actual querying
                .collect(Collectors.toList()).stream();
        }

        if (needsAdditionalFirstResultFiltering.get() && currentFirst.get() > 0) {
            logger.tracef("In the providerStream there is a provider that does not support count queries and we need to skip some users.");
            // we need to make sure, we skip firstResult users from this or the following providers
            if (maxResults == null || maxResults < 0) {
                return paginatedStream(providersStream
                        .flatMap(provider -> queryWithGracefulDegradation(provider, pagedQuery, null, null)), currentFirst.get(), null);
            } else {
                final AtomicInteger currentMax = new AtomicInteger(currentFirst.get() + maxResults);

                return paginatedStream(providersStream
                    .flatMap(provider -> queryWithGracefulDegradation(provider, pagedQuery, null, currentMax.get()))
                    .peek(userModel -> {
                        currentMax.updateAndGet(i -> i > 0 ? i - 1 : i);
                    }), currentFirst.get(), maxResults);
            }
        }

        // Actual user querying
        if (maxResults == null || maxResults < 0) {
            // No maxResult set, we want all users
            return providersStream
                    .flatMap(provider -> queryWithGracefulDegradation(provider, pagedQuery, currentFirst.getAndSet(0), null));
        } else {
            final AtomicInteger currentMax = new AtomicInteger(maxResults);

            // Query users with currentMax variable counting how many users we return
            return providersStream
                    .filter(provider -> currentMax.get() != 0) // If we reach currentMax == 0, we can skip querying all following providers
                    .flatMap(provider -> queryWithGracefulDegradation(provider, pagedQuery, currentFirst.getAndSet(0), currentMax.get()))
                    .peek(userModel -> {
                        currentMax.updateAndGet(i -> i > 0 ? i - 1 : i);
                    });
        }

    }

    /**
     * Executes a query against a user storage provider with graceful degradation.
     * If the provider throws an exception, logs the error and returns an empty stream
     * to allow other providers to continue functioning.
     */
    private Stream<UserModel> queryWithGracefulDegradation(Object provider, PaginatedQuery pagedQuery, 
                                                          Integer firstResult, Integer maxResults) {
        try {
            return pagedQuery.query(provider, firstResult, maxResults);
        } catch (Exception e) {
            // Log the failure but continue with other providers for graceful degradation
            logger.warnf(e, "User storage provider %s failed during query operation. " +
                         "Continuing with other providers for graceful degradation. " +
                         "This may indicate an issue with external user store connectivity (e.g., LDAP server down).",
                         provider.getClass().getSimpleName());
            return Stream.empty();
        }
    }

    /**
     * Executes a count query against a user storage provider with graceful degradation.
     * If the provider throws an exception, logs the error and returns 0
     * to allow other providers to continue functioning.
     */
    private int countQueryWithGracefulDegradation(Object provider, CountQuery countQuery, 
                                                 Integer firstResult, Integer maxResults) {
        try {
            return countQuery.query(provider, firstResult, maxResults);
        } catch (Exception e) {
            // Log the failure but continue with other providers for graceful degradation
            logger.warnf(e, "User storage provider %s failed during count operation. " +
                         "Continuing with other providers for graceful degradation. " +
                         "This may indicate an issue with external user store connectivity (e.g., LDAP server down).",
                         provider.getClass().getSimpleName());
            return 0;
        }
    }

    /**
     * Helper method to get total user count from local storage plus all federated storage providers
     * with graceful degradation.
     */
    private int getTotalUserCountWithGracefulDegradation(RealmModel realm, 
                                                        java.util.function.Function<Object, Integer> countFunction) {
        // Count users from local storage
        int localCount = countFunction.apply(localStorage());
        
        // Count users from all enabled storage providers with graceful degradation
        Stream<Object> providers = getEnabledStorageProviders(realm, Object.class);
        
        int federatedCount = providers
            .mapToInt(provider -> countQueryWithGracefulDegradation(provider, 
                (p, firstResult, maxResults) -> countFunction.apply(p), null, null))
            .sum();
            
        return localCount + federatedCount;
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

        publishUserPreRemovedEvent(realm, user);

        StorageId storageId = new StorageId(user.getId());

        if (storageId.getProviderId() == null) {
            boolean linkRemoved = !user.isFederated() || Optional.ofNullable(
                    getStorageProviderInstance(realm, user.getFederationLink(), UserRegistrationProvider.class))
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
            return validateUser(realm, user);
        }

        UserLookupProvider provider = getStorageProviderInstance(realm, storageId.getProviderId(), UserLookupProvider.class);
        if (provider == null) return null;

        return provider.getUserById(realm, id);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return getUserByAttribute(realm,
                provider -> provider.getUserByUsername(realm, username),
                u -> username.equalsIgnoreCase(u.getUsername()));
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return getUserByAttribute(realm,
                provider -> provider.getUserByEmail(realm, email),
                u -> email.equalsIgnoreCase(u.getEmail()));
    }

    /** {@link UserLookupProvider} methods implementations end here
        {@link UserQueryProvider} methods implementation start here */

    @Override
    public Stream<UserModel> getGroupMembersStream(final RealmModel realm, final GroupModel group, Integer firstResult, Integer maxResults) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
                    if (provider instanceof UserQueryMethodsProvider) {
                        return ((UserQueryMethodsProvider) provider).getGroupMembersStream(realm, group, firstResultInQuery, maxResultsInQuery);

                    } else if (provider instanceof UserFederatedStorageProvider) {
                        return ((UserFederatedStorageProvider) provider).getMembershipStream(realm, group, firstResultInQuery, maxResultsInQuery).
                                map(id -> getUserById(realm, id));
                    }
                    return Stream.empty();
                },
                (provider, firstResultInQuery, maxResultsInQuery) -> {
                    if (provider instanceof UserCountMethodsProvider) {
                        return ((UserCountMethodsProvider) provider).getUsersCount(realm, Set.of(group.getId()));
                    }
                    return 0;
                },
                realm, firstResult, maxResults);

        return importValidation(realm, results);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(final RealmModel realm, final GroupModel group, final String search,
                                                   final Boolean exact, final Integer firstResult, final Integer maxResults) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryMethodsProvider) {
                return ((UserQueryMethodsProvider)provider).getGroupMembersStream(realm, group, search, exact, firstResultInQuery, maxResultsInQuery);

            } else if (provider instanceof UserFederatedStorageProvider) {
                // modify this if UserGroupMembershipFederatedStorage adds a getMembershipStream variant with search option.
                return StreamsUtil.paginatedStream(((UserFederatedStorageProvider)provider).getMembershipStream(realm, group, null, null)
                        .map(id -> getUserById(realm, id))
                        .filter(user -> {
                            if (StringUtil.isBlank(search)) return true;
                            if (Boolean.TRUE.equals(exact)) {
                                return search.equals(user.getUsername()) || search.equals(user.getEmail())
                                        || search.equals(user.getFirstName()) || search.equals(user.getLastName());
                            } else {
                                return Optional.ofNullable(user.getUsername()).orElse("").toLowerCase().contains(search.toLowerCase()) ||
                                        Optional.ofNullable(user.getEmail()).orElse("").toLowerCase().contains(search.toLowerCase()) ||
                                        Optional.ofNullable(user.getFirstName()).orElse("").toLowerCase().contains(search.toLowerCase()) ||
                                        Optional.ofNullable(user.getLastName()).orElse("").toLowerCase().contains(search.toLowerCase());
                            }
                        }), firstResultInQuery, maxResultsInQuery);
            }
            return Stream.empty();
        }, realm, firstResult, maxResults);

        return importValidation(realm, results);
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(final RealmModel realm, final RoleModel role, Integer firstResult, Integer maxResults) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryMethodsProvider) {
                return ((UserQueryMethodsProvider)provider).getRoleMembersStream(realm, role, firstResultInQuery, maxResultsInQuery);
            }
            else if (provider instanceof UserFederatedStorageProvider) {
                return ((UserFederatedStorageProvider)provider).getRoleMembersStream(realm, role, firstResultInQuery, maxResultsInQuery).
                        map(id -> getUserById(realm, id));
            }
            return Stream.empty();
        }, realm, firstResult, maxResults);
        return importValidation(realm, results);
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        int localStorageUsersCount = localStorage().getUsersCount(realm, includeServiceAccount);
        int storageProvidersUsersCount = mapEnabledStorageProvidersWithTimeout(realm, UserCountMethodsProvider.class,
                userQueryProvider -> userQueryProvider.getUsersCount(realm))
                .reduce(0, Integer::sum);

        return localStorageUsersCount + storageProvidersUsersCount;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return getUsersCount(realm, false);
    }

    @Override
    public int getUsersCount(RealmModel realm, Set<String> groupIds) {
        return getTotalUserCountWithGracefulDegradation(realm, provider -> {
            if (provider instanceof UserCountMethodsProvider) {
                return ((UserCountMethodsProvider) provider).getUsersCount(realm, groupIds);
            }
            return 0;
        });
    }

    @Override
    public int getUsersCount(RealmModel realm, String search) {
        return getTotalUserCountWithGracefulDegradation(realm, provider -> {
            if (provider instanceof UserCountMethodsProvider) {
                return ((UserCountMethodsProvider) provider).getUsersCount(realm, search);
            }
            return 0;
        });
    }

    @Override
    public int getUsersCount(RealmModel realm, String search, Set<String> groupIds) {
        return getTotalUserCountWithGracefulDegradation(realm, provider -> {
            if (provider instanceof UserCountMethodsProvider) {
                return ((UserCountMethodsProvider) provider).getUsersCount(realm, search, groupIds);
            }
            return 0;
        });
    }

    @Override
    public int getUsersCount(RealmModel realm, Map<String, String> params) {
        return getTotalUserCountWithGracefulDegradation(realm, provider -> {
            if (provider instanceof UserCountMethodsProvider) {
                return ((UserCountMethodsProvider) provider).getUsersCount(realm, params);
            }
            return 0;
        });
    }

    @Override
    public int getUsersCount(RealmModel realm, Map<String, String> params, Set<String> groupIds) {
        return getTotalUserCountWithGracefulDegradation(realm, provider -> {
            if (provider instanceof UserCountMethodsProvider) {
                return ((UserCountMethodsProvider) provider).getUsersCount(realm, params, groupIds);
            }
            return 0;
        });
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryMethodsProvider) {
                return ((UserQueryMethodsProvider)provider).searchForUserStream(realm, attributes, firstResultInQuery, maxResultsInQuery);
            }
            return Stream.empty();
        },
        (provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserCountMethodsProvider) {
                return ((UserCountMethodsProvider)provider).getUsersCount(realm, attributes);
            }
            return 0;
        }
        , realm, firstResult, maxResults);
        return importValidation(realm, results);
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        Stream<UserModel> results = query((provider, firstResultInQuery, maxResultsInQuery) -> {
            if (provider instanceof UserQueryMethodsProvider) {
                return paginatedStream(((UserQueryMethodsProvider)provider).searchForUserByUserAttributeStream(realm, attrName, attrValue), firstResultInQuery, maxResultsInQuery);
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

        session.getKeycloakSessionFactory().publish(new FederatedIdentityModel.FederatedIdentityCreatedEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public UserModel getUser() {
                return user;
            }

            @Override
            public FederatedIdentityModel getFederatedIdentity() {
                return socialLink;
            }
        });
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
        FederatedIdentityModel federatedIdentityModel;
        if (StorageId.isLocalStorage(user)) {
            UserProvider localStorage = localStorage();
            federatedIdentityModel = localStorage.getFederatedIdentity(realm, user, socialProvider);
            localStorage.removeFederatedIdentity(realm, user, socialProvider);
        } else {
            UserFederatedStorageProvider federatedStorage = getFederatedStorage();
            federatedIdentityModel = federatedStorage.getFederatedIdentity(user.getId(), socialProvider, realm);
            federatedStorage.removeFederatedIdentity(realm, user.getId(), socialProvider);
        }

        if (federatedIdentityModel == null) {
           return false;
        }

        session.getKeycloakSessionFactory().publish(new FederatedIdentityModel.FederatedIdentityRemovedEvent() {
            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public UserModel getUser() {
                return user;
            }

            @Override
            public FederatedIdentityModel getFederatedIdentity() {
                return federatedIdentityModel;
            }
        });

        return true;
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
            return validateUser(realm, user);
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
        notifyToRefreshPeriodicSync(session, realm, new UserStorageProviderModel(component), true);
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

        // enlistAfterCompletion(..) as we need to ensure that the realm is available in the system
        session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
            @Override
            protected void commitImpl() {
                notifyToRefreshPeriodicSync(session, realm, new UserStorageProviderModel(model), false);
            }

            @Override
            protected void rollbackImpl() {
                // NOOP
            }
        });
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        ComponentFactory<?, ?> factory = ComponentUtil.getComponentFactory(session, newModel);

        if (!(factory instanceof UserStorageProviderFactory)) {
            return;
        }

        UserStorageProviderModel previous = new UserStorageProviderModel(oldModel);
        UserStorageProviderModel actual= new UserStorageProviderModel(newModel);

        if (isSyncSettingsUpdated(previous, actual)) {
            notifyToRefreshPeriodicSync(session, realm, actual, false);
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

    @Override
    public List<AttributeMetadata> decorateUserProfile(String providerId, UserProfileMetadata metadata) {
        RealmModel realm = session.getContext().getRealm();
        UserStorageProviderModel providerModel = getStorageProviderModel(realm, providerId);

        if (providerModel != null) {
            UserProfileDecorator decorator = getStorageProviderInstance(providerModel, UserProfileDecorator.class);

            if (decorator != null) {
                return decorator.decorateUserProfile(providerId, metadata);
            }
        }

        return Collections.emptyList();
    }

    @Override
    public UserCredentialManager getUserCredentialManager(UserModel user) {
        return new org.keycloak.credential.UserCredentialManager(session, session.getContext().getRealm(), user);
    }

    private boolean isReadOnlyOrganizationMember(UserModel delegate) {
        if (delegate == null) {
            return false;
        }

        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return false;
        }

        OrganizationProvider organizationProvider = session.getProvider(OrganizationProvider.class);

        if (organizationProvider.count() == 0) {
            return false;
        }

        // check if provider is enabled and user is managed member of a disabled organization OR provider is disabled and user is managed member
        return organizationProvider.getByMember(delegate)
                .anyMatch((org) -> (organizationProvider.isEnabled() && org.isManaged(delegate) && !org.isEnabled()) ||
                        (!organizationProvider.isEnabled() && org.isManaged(delegate)));
    }

    private void publishUserPreRemovedEvent(RealmModel realm, UserModel user) {
        session.getKeycloakSessionFactory().publish(new UserModel.UserPreRemovedEvent() {
            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public UserModel getUser() {
                return user;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
    }

    private UserModel getUserByAttribute(RealmModel realm, Function<UserLookupProvider, UserModel> loader, Predicate<UserModel> attributeValidator) {
        // first try the local storage
        UserModel user = loader.apply(localStorage());

        if (user != null) {
            // run global user validations
            UserModel validated = validateUser(realm, user);

            // make sure the attribute is valid
            if (validated != null && attributeValidator.test(validated)) {
                return validated;
            }

            // user or attribute not valid, invalidate cache
            deleteInvalidUserCache(realm, user);
        }

        // try to resolve the user from the external storage
        return tryResolveFederatedUser(realm, loader);
    }

    private UserModel tryResolveFederatedUser(RealmModel realm, Function<UserLookupProvider, UserModel> loader) {
        return mapEnabledStorageProvidersWithTimeout(realm, UserLookupProvider.class, provider -> {
            try {
                return loader.apply(provider);
            } catch (StorageUnavailableException e) {
                logger.warnf(e, "User storage provider %s is unavailable. " +
                             "Continuing with other providers for graceful degradation.",
                             provider.getClass().getSimpleName());
                return null;
            }
        })
        .findFirst()
        .orElse(null);
    }

    private boolean isSyncSettingsUpdated(UserStorageProviderModel previous, UserStorageProviderModel actual) {
        return previous.getChangedSyncPeriod() != actual.getChangedSyncPeriod()
                || previous.getFullSyncPeriod() != actual.getFullSyncPeriod()
                || previous.isImportEnabled() != actual.isImportEnabled()
                || previous.isEnabled() != actual.isEnabled();
    }
}

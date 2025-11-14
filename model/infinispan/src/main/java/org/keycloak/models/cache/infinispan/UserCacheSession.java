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

package org.keycloak.models.cache.infinispan;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.support.EntityManagers;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.cache.infinispan.entities.CachedFederatedIdentityLinks;
import org.keycloak.models.cache.infinispan.entities.CachedUser;
import org.keycloak.models.cache.infinispan.entities.CachedUserConsent;
import org.keycloak.models.cache.infinispan.entities.CachedUserConsents;
import org.keycloak.models.cache.infinispan.entities.UserListQuery;
import org.keycloak.models.cache.infinispan.events.CacheKeyInvalidatedEvent;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;
import org.keycloak.models.cache.infinispan.events.UserCacheRealmInvalidationEvent;
import org.keycloak.models.cache.infinispan.events.UserConsentsUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.UserFederationLinkRemovedEvent;
import org.keycloak.models.cache.infinispan.events.UserFederationLinkUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.UserFullInvalidationEvent;
import org.keycloak.models.cache.infinispan.events.UserUpdatedEvent;
import org.keycloak.models.cache.infinispan.stream.InIdentityProviderPredicate;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ReadOnlyUserModelDelegate;
import org.keycloak.storage.CacheableStorageProviderModel;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.OnCreateComponent;
import org.keycloak.storage.OnUpdateComponent;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.StoreManagers;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfileDecorator;
import org.keycloak.userprofile.UserProfileMetadata;

import org.jboss.logging.Logger;

import static java.util.Optional.ofNullable;

import static org.keycloak.organization.utils.Organizations.isReadOnlyOrganizationMember;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCacheSession implements UserCache, OnCreateComponent, OnUpdateComponent, UserProfileDecorator {
    protected static final Logger logger = Logger.getLogger(UserCacheSession.class);
    protected UserCacheManager cache;
    protected KeycloakSession session;
    protected UserProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;
    protected final long startupRevision;


    protected Set<String> invalidations = new HashSet<>();
    protected Set<String> realmInvalidations = new HashSet<>();
    protected Set<InvalidationEvent> invalidationEvents = new HashSet<>(); // Events to be sent across cluster
    protected Map<String, UserModel> managedUsers = new HashMap<>();
    private StoreManagers datastoreProvider;

    public UserCacheSession(UserCacheManager cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;
        this.startupRevision = cache.getCurrentCounter();
        this.datastoreProvider = (StoreManagers) session.getProvider(DatastoreProvider.class);
        session.getTransactionManager().enlistAfterCompletion(getTransaction());
    }

    @Override
    public void clear() {
        cache.clear();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.notify(InfinispanUserCacheProviderFactory.USER_CLEAR_CACHE_EVENTS, ClearCacheEvent.getInstance(), true);
    }

    public UserProvider getDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (delegate != null) return delegate;
        delegate = this.datastoreProvider.userStorageManager();

        return delegate;
    }

    public void registerUserInvalidation(CachedUser user) {
        cache.userUpdatedInvalidations(user.getId(), user.getUsername(), user.getEmail(), user.getRealm(), invalidations);
        invalidationEvents.add(UserUpdatedEvent.create(user.getId(), user.getUsername(), user.getEmail(), user.getRealm()));
    }

    @Override
    public void evict(RealmModel realm, UserModel user) {
        if (!transactionActive) throw new IllegalStateException("Cannot call evict() without a transaction");
        getDelegate(); // invalidations need delegate set
        if (user instanceof CachedUserModel) {
            ((CachedUserModel)user).invalidate();
        } else {
            cache.userUpdatedInvalidations(user.getId(), user.getUsername(), user.getEmail(), realm.getId(), invalidations);
            invalidationEvents.add(UserUpdatedEvent.create(user.getId(), user.getUsername(), user.getEmail(), realm.getId()));
        }
    }

    @Override
    public void evict(RealmModel realm) {
        addRealmInvalidation(realm.getId());
    }

    protected void runInvalidations() {
        for (String realmId : realmInvalidations) {
            cache.invalidateRealmUsers(realmId, invalidations);
        }
        for (String invalidation : invalidations) {
            cache.invalidateObject(invalidation);
        }

        cache.sendInvalidationEvents(session, invalidationEvents, InfinispanUserCacheProviderFactory.USER_INVALIDATION_EVENTS);
    }

    private KeycloakTransaction getTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void setRollbackOnly() {
                setRollbackOnly = true;
            }

            @Override
            public boolean getRollbackOnly() {
                return setRollbackOnly;
            }

            @Override
            public boolean isActive() {
                return transactionActive;
            }
        };
    }

    private boolean isRegisteredForInvalidation(RealmModel realm, String userId) {
        return realmInvalidations.contains(realm.getId()) || invalidations.contains(userId);
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        logger.tracev("getuserById {0}", id);
        if (isRegisteredForInvalidation(realm, id)) {
            logger.trace("registered for invalidation return delegate");
            return getDelegate().getUserById(realm, id);
        }
        if (managedUsers.containsKey(id)) {
            logger.trace("return managedusers");
            return managedUsers.get(id);
        }

        CachedUser cached = cache.get(id, CachedUser.class);

        if (cached != null && !cached.getRealm().equals(realm.getId())) {
            cached = null;
        }

        UserModel adapter = null;
        if (cached == null) {
            logger.trace("not cached");
            Long loaded = cache.getCurrentRevision(id);
            UserModel delegate = getDelegate().getUserById(realm, id);
            if (delegate == null) {
                logger.trace("delegate returning null");
                return null;
            }
            adapter = cacheUser(realm, delegate, loaded);
        } else {
            adapter = validateCache(realm, cached, () -> getDelegate().getUserById(realm, id));
        }
        addManagedUser(id, adapter);
        return adapter;
    }

    static String getUserByUsernameCacheKey(String realmId, String username) {
        return realmId + ".username." + username;
    }

    static String getUserByEmailCacheKey(String realmId, String email) {
        return realmId + ".email." + email;
    }

    private static String getUserByFederatedIdentityCacheKey(String realmId, FederatedIdentityModel socialLink) {
        return getUserByFederatedIdentityCacheKey(realmId, socialLink.getIdentityProvider(), socialLink.getUserId());
    }

    static String getUserByFederatedIdentityCacheKey(String realmId, String identityProvider, String socialUserId) {
        return realmId + ".idp." + identityProvider + "." + socialUserId;
    }

    static String getFederatedIdentityLinksCacheKey(String userId) {
        return userId + ".idplinks";
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String rawUsername) {
        if (rawUsername == null) {
            return null;
        }
        String username = rawUsername.toLowerCase();
        logger.tracev("getUserByUsername: {0}", username);
        if (realmInvalidations.contains(realm.getId())) {
            logger.tracev("realmInvalidations");
            return getDelegate().getUserByUsername(realm, username);
        }
        String cacheKey = getUserByUsernameCacheKey(realm.getId(), username);
        if (invalidations.contains(cacheKey)) {
            logger.tracev("invalidations");
            return getDelegate().getUserByUsername(realm, username);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        if (query == null) {
            logger.tracev("query null");
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getUserByUsername(realm, username);
            if (model == null) {
                logger.tracev("model from delegate null");
                return null;
            }
            String userId = model.getId();
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) {
                logger.tracev("return managed user");
                return managedUsers.get(userId);
            }

            UserModel adapter = getUserAdapter(realm, userId, loaded, model);
            if (adapter instanceof UserAdapter) { // this was cached, so we can cache query too
                query = new UserListQuery(loaded, cacheKey, realm, model.getId());
                cache.addRevisioned(query, startupRevision, getLifespan(realm, adapter));
            }
            addManagedUser(userId, adapter);
            return adapter;
        }

        String userId = query.getUsers().iterator().next();
        if (invalidations.contains(userId)) {
            logger.tracev("invalidated cache return delegate");
            return getDelegate().getUserByUsername(realm, username);

        }
        logger.trace("return getUserById");
        return ofNullable(getUserById(realm, userId))
                // Validate for cases where the cached elements are not in sync.
                // This might happen to changes in a federated store where caching is enabled and different items expire at different times,
                // for example when they are evicted due to the limited size of the cache
                .filter((u) -> username.equalsIgnoreCase(u.getUsername()))
                .orElseGet(() -> {
                    registerInvalidation(cacheKey);
                    return getDelegate().getUserByUsername(realm, username);
                });
    }

    private long getLifespan(RealmModel realm, UserModel user) {
        if (!user.isFederated()) {
            return -1; // cache infinite
        }

        String providerId = user.getFederationLink();

        if (providerId == null) {
            providerId = StorageId.providerId(user.getId());
        }

        ComponentModel component = realm.getComponent(providerId);
        UserStorageProviderModel model = new UserStorageProviderModel(component);

        if (model.isEnabled()) {
            UserStorageProviderModel.CachePolicy policy = model.getCachePolicy();

            if (policy == null) {
                // no policy set, cache entries by default
                return -1;
            }

            if (!UserStorageProviderModel.CachePolicy.NO_CACHE.equals(policy)) {
                long lifespan = model.getLifespan();
                return lifespan > 0 ? lifespan : -1;
            }
        }

        return 0; // do not cache
    }

    protected UserModel getUserAdapter(RealmModel realm, String userId, Long loaded, UserModel delegate) {
        CachedUser cached = cache.get(userId, CachedUser.class);
        if (cached == null) {
            return cacheUser(realm, delegate, loaded);
        } else {
            return validateCache(realm, cached, () -> getDelegate().getUserById(realm, userId));
        }
    }

    protected UserModel validateCache(RealmModel realm, CachedUser cached, Supplier<UserModel> supplier) {
        if (!realm.getId().equals(cached.getRealm())) {
            return null;
        }

        StorageId storageId = cached.getFederationLink() != null ?
                new StorageId(cached.getFederationLink(), cached.getId()) : new StorageId(cached.getId());

        if (!storageId.isLocal()) {
            ComponentModel component = realm.getComponent(storageId.getProviderId());
            if (component == null) {
                return null;
            }
            CacheableStorageProviderModel model = new CacheableStorageProviderModel(component);

            if (model.shouldInvalidate(cached)) {
                registerUserInvalidation(cached);
                return supplier.get();
            }
        }

        UserAdapter userAdapter = new UserAdapter(cached, this, session, realm);

        if (isReadOnlyOrganizationMember(session, userAdapter)) {
            return new ReadOnlyUserModelDelegate(userAdapter, false);
        }

        return userAdapter;
    }

    protected UserModel cacheUser(RealmModel realm, UserModel delegate, Long revision) {
        int notBefore = getDelegate().getNotBeforeOfUser(realm, delegate);

        if (isReadOnlyOrganizationMember(session, delegate)) {
            return new ReadOnlyUserModelDelegate(delegate, false);
        }

        CachedUser cached;
        UserAdapter adapter;

        if (delegate.isFederated()) {
            ComponentModel component = realm.getComponent(delegate.getFederationLink());
            UserStorageProviderModel model = new UserStorageProviderModel(component);
            if (!model.isEnabled()) {
                return new ReadOnlyUserModelDelegate(delegate, false);
            }

            long lifespan = getLifespan(realm, delegate);
            if (lifespan == 0) {
                return delegate;
            }

            cached = new CachedUser(revision, realm, delegate, notBefore);
            adapter = new UserAdapter(cached, this, session, realm);
            onCache(realm, adapter, delegate);

            cache.addRevisioned(cached, startupRevision, lifespan);
        } else {
            cached = new CachedUser(revision, realm, delegate, notBefore);
            adapter = new UserAdapter(cached, this, session, realm);
            onCache(realm, adapter, delegate);
            cache.addRevisioned(cached, startupRevision);
        }

        return adapter;
    }

    private void onCache(RealmModel realm, UserAdapter adapter, UserModel delegate) {
        ((OnUserCache)getDelegate()).onCache(realm, adapter, delegate);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String rawEmail) {
        if (rawEmail == null) return null;
        String email = rawEmail.toLowerCase();
        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByEmail(realm, email);
        }
        String cacheKey = getUserByEmailCacheKey(realm.getId(), email);
        if (invalidations.contains(cacheKey)) {
            return getDelegate().getUserByEmail(realm, email);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getUserByEmail(realm, email);
            if (model == null) return null;
            String userId = model.getId();
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) return managedUsers.get(userId);

            UserModel adapter = getUserAdapter(realm, userId, loaded, model);
            if (adapter instanceof UserAdapter) {
                query = new UserListQuery(loaded, cacheKey, realm, model.getId());
                cache.addRevisioned(query, startupRevision, getLifespan(realm, adapter));
            }
            addManagedUser(userId, adapter);
            return adapter;
        }

        String userId = query.getUsers().iterator().next();
        if (invalidations.contains(userId)) {
            return getDelegate().getUserByEmail(realm, email);

        }
        return ofNullable(getUserById(realm, userId))
                // Validate for cases where the cached elements are not in sync.
                // This might happen to changes in a federated store where caching is enabled and different items expire at different times,
                // for example when they are evicted due to the limited size of the cache
                .filter((u) -> email.equalsIgnoreCase(u.getEmail()))
                .orElseGet(() -> {
                    registerInvalidation(cacheKey);
                    return getDelegate().getUserByEmail(realm, email);
                });
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public UserModel getUserByFederatedIdentity(RealmModel realm, FederatedIdentityModel socialLink) {
        if (socialLink == null) return null;
        if (!realm.isIdentityFederationEnabled()) return null;

        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByFederatedIdentity(realm, socialLink);
        }
        String cacheKey = getUserByFederatedIdentityCacheKey(realm.getId(), socialLink);
        if (invalidations.contains(cacheKey)) {
            return getDelegate().getUserByFederatedIdentity(realm, socialLink);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        String userId = null;
        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getUserByFederatedIdentity(realm, socialLink);
            if (model == null) return null;
            userId = model.getId();
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) return managedUsers.get(userId);

            UserModel adapter = getUserAdapter(realm, userId, loaded, model);
            if (adapter instanceof UserAdapter) {
                query = new UserListQuery(loaded, cacheKey, realm, model.getId());
                cache.addRevisioned(query, startupRevision, getLifespan(realm, adapter));
            }

            addManagedUser(userId, adapter);
            return adapter;
        } else {
            userId = query.getUsers().iterator().next();
            if (invalidations.contains(userId)) {
                invalidations.add(cacheKey);
                return getDelegate().getUserByFederatedIdentity(realm, socialLink);

            }
            return getUserById(realm, userId);
        }
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return getDelegate().getGroupMembersStream(realm, group, firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        return getDelegate().getGroupMembersStream(realm, group, search, exact, firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
        return getDelegate().getGroupMembersStream(realm, group);
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        return getDelegate().getRoleMembersStream(realm, role, firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role) {
        return getDelegate().getRoleMembersStream(realm, role);
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        // Just an attempt to find the user from cache by default serviceAccount username
        UserModel user = findServiceAccount(client);
        if (user != null && user.getServiceAccountClientLink() != null && user.getServiceAccountClientLink().equals(client.getId())) {
            return user;
        }

        return getDelegate().getServiceAccount(client);
    }

    public UserModel findServiceAccount(ClientModel client) {
        String username = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId();
        logger.tracev("getServiceAccount: {0}", username);
        username = username.toLowerCase();
        RealmModel realm = client.getRealm();
        if (realmInvalidations.contains(realm.getId())) {
            logger.tracev("realmInvalidations");
            return getDelegate().getServiceAccount(client);
        }
        String cacheKey = getUserByUsernameCacheKey(realm.getId(), username);
        if (invalidations.contains(cacheKey)) {
            logger.tracev("invalidations");
            return getDelegate().getServiceAccount(client);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        String userId = null;
        if (query == null) {
            logger.tracev("query null");
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getServiceAccount(client);
            if (model == null) {
                logger.tracev("model from delegate null");
                return null;
            }
            userId = model.getId();
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) {
                logger.tracev("return managed user");
                return managedUsers.get(userId);
            }

            UserModel adapter = getUserAdapter(realm, userId, loaded, model);
            if (adapter instanceof UserAdapter) { // this was cached, so we can cache query too
                query = new UserListQuery(loaded, cacheKey, realm, model.getId());
                cache.addRevisioned(query, startupRevision, getLifespan(realm, adapter));
            }
            addManagedUser(userId, adapter);
            return adapter;
        } else {
            userId = query.getUsers().iterator().next();
            if (invalidations.contains(userId)) {
                logger.tracev("invalidated cache return delegate");
                return getDelegate().getUserByUsername(realm, username);

            }
            logger.trace("return getUserById");
            return getUserById(realm, userId);
        }
    }

    @Override
    public CredentialValidationOutput getUserByCredential(RealmModel realm, CredentialInput input) {
        return getDelegate().getUserByCredential(realm, input);
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        return getDelegate().getUsersCount(realm, includeServiceAccount);
    }

    @Override
    public int getUsersCount(RealmModel realm, Set<String> groupIds) {
        return getDelegate().getUsersCount(realm, groupIds);
    }

    @Override
    public int getUsersCount(RealmModel realm, String search) {
        return getDelegate().getUsersCount(realm, search);
    }

    @Override
    public int getUsersCount(RealmModel realm, String search, Set<String> groupIds) {
        return getDelegate().getUsersCount(realm, search, groupIds);
    }

    @Override
    public int getUsersCount(RealmModel realm, Map<String, String> params) {
        return getDelegate().getUsersCount(realm, params);
    }

    @Override
    public int getUsersCount(RealmModel realm, Map<String, String> params, Set<String> groupIds) {
        return getDelegate().getUsersCount(realm, params, groupIds);
    }

    private UserModel returnFromCacheIfPresent(RealmModel realm, UserModel delegate) {
        if (delegate == null || delegate instanceof CachedUserModel || isRegisteredForInvalidation(realm, delegate.getId())) {
            return delegate;
        }

        if (managedUsers.containsKey(delegate.getId())) {
            return managedUsers.get(delegate.getId());
        }

        CachedUser cached = cache.get(delegate.getId(), CachedUser.class);
        if (cached == null) {
            return delegate;
        }

        UserModel cachedUserModel = validateCache(realm, cached, () -> delegate);
        return cachedUserModel != null? cachedUserModel : delegate;
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search) {
        return getDelegate().searchForUserStream(realm, search).map(u -> returnFromCacheIfPresent(realm, u));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        return getDelegate().searchForUserStream(realm, search, firstResult, maxResults).map(u -> returnFromCacheIfPresent(realm, u));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> attributes) {
        return getDelegate().searchForUserStream(realm, attributes).map(u -> returnFromCacheIfPresent(realm, u));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return getDelegate().searchForUserStream(realm, attributes, firstResult, maxResults).map(u -> returnFromCacheIfPresent(realm, u));
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return getDelegate().searchForUserByUserAttributeStream(realm, attrName, attrValue).map(u -> returnFromCacheIfPresent(realm, u));
    }

    @Override
    public Stream<FederatedIdentityModel> getFederatedIdentitiesStream(RealmModel realm, UserModel user) {
        logger.tracev("getFederatedIdentities: {0}", user.getUsername());

        String cacheKey = getFederatedIdentityLinksCacheKey(user.getId());
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(user.getId()) || invalidations.contains(cacheKey)) {
            return getDelegate().getFederatedIdentitiesStream(realm, user);
        }

        CachedFederatedIdentityLinks cachedLinks = cache.get(cacheKey, CachedFederatedIdentityLinks.class);

        if (cachedLinks == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            Set<FederatedIdentityModel> federatedIdentities = getDelegate().getFederatedIdentitiesStream(realm, user)
                    .collect(Collectors.toSet());
            cachedLinks = new CachedFederatedIdentityLinks(loaded, cacheKey, realm, federatedIdentities);
            cache.addRevisioned(cachedLinks, startupRevision); // this is Keycloak's internal store, cache indefinitely
            return federatedIdentities.stream();
        } else {
            return cachedLinks.getFederatedIdentities().stream();
        }
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        logger.tracev("getFederatedIdentity: {0} {1}", user.getUsername(), socialProvider);

        String cacheKey = getFederatedIdentityLinksCacheKey(user.getId());
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(user.getId()) || invalidations.contains(cacheKey)) {
            return getDelegate().getFederatedIdentity(realm, user, socialProvider);
        }

        return getFederatedIdentitiesStream(realm, user)
                .filter(socialLink -> Objects.equals(socialLink.getIdentityProvider(), socialProvider))
                .findFirst().orElse(null);
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        invalidateConsent(userId);
        getDelegate().updateConsent(realm, userId, consent);
    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId) {
        invalidateConsent(userId);
        return getDelegate().revokeConsentForClient(realm, userId, clientInternalId);
    }

    static String getConsentCacheKey(String userId) {
        return userId + ".consents";
    }


    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        invalidateConsent(userId);
        getDelegate().addConsent(realm, userId, consent);
    }

    private void invalidateConsent(String userId) {
        cache.consentInvalidation(userId, invalidations);
        invalidationEvents.add(UserConsentsUpdatedEvent.create(userId));
    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientId) {
        logger.tracev("getConsentByClient: {0}", userId);

        String cacheKey = getConsentCacheKey(userId);
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(userId) || invalidations.contains(cacheKey)) {
            return getDelegate().getConsentByClient(realm, userId, clientId);
        }

        CachedUserConsents cached = cache.get(cacheKey, CachedUserConsents.class);

        if (cached == null) {
            UserConsentModel consent = getDelegate().getConsentByClient(realm, userId, clientId);
            List<CachedUserConsent> consents;

            if (consent == null) {
                consents = Collections.singletonList(new CachedUserConsent(clientId));
            } else {
                consents = Collections.singletonList(new CachedUserConsent(consent));
            }

            Long loaded = cache.getCurrentRevision(cacheKey);
            cached = new CachedUserConsents(loaded, cacheKey, realm, consents, false);
            cache.addRevisioned(cached, startupRevision); // this is from Keycloak's internal store, cache indefinitely
        }

        Map<String, CachedUserConsent> consents = cached.getConsents();
        CachedUserConsent cachedConsent = consents.get(clientId);

        if (cachedConsent == null) {
            UserConsentModel consent = getDelegate().getConsentByClient(realm, userId, clientId);

            if (consent == null) {
                cachedConsent = new CachedUserConsent(clientId);
            } else {
                cachedConsent = new CachedUserConsent(consent);
            }

            consents.put(cachedConsent.getClientDbId(), cachedConsent);
        }

        return toConsentModel(realm, cachedConsent);
    }

    @Override
    public Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId) {
        logger.tracev("getConsents: {0}", userId);

        String cacheKey = getConsentCacheKey(userId);
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(userId) || invalidations.contains(cacheKey)) {
            return getDelegate().getConsentsStream(realm, userId);
        }

        CachedUserConsents cached = cache.get(cacheKey, CachedUserConsents.class);

        if (cached != null && !cached.isAllConsents()) {
            cached = null;
            cache.invalidateObject(cacheKey);
        }

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<UserConsentModel> consents = getDelegate().getConsentsStream(realm, userId).collect(Collectors.toList());
            cached = new CachedUserConsents(loaded, cacheKey, realm, consents.stream().map(CachedUserConsent::new).collect(Collectors.toList()));
            cache.addRevisioned(cached, startupRevision); // this is from Keycloak's internal store, cache indefinitely
            return consents.stream();
        } else {
            return cached.getConsents().values().stream().map(cachedConsent -> toConsentModel(realm, cachedConsent))
                    .filter(Objects::nonNull);
        }
    }

    private UserConsentModel toConsentModel(RealmModel realm, CachedUserConsent cachedConsent) {
        if (cachedConsent.isNotExistent()) {
            return null;
        }

        ClientModel client = session.clients().getClientById(realm, cachedConsent.getClientDbId());
        if (client == null) {
            return null;
        }

        UserConsentModel consentModel = new UserConsentModel(client);
        consentModel.setCreatedDate(cachedConsent.getCreatedDate());
        consentModel.setLastUpdatedDate(cachedConsent.getLastUpdatedDate());

        for (String clientScopeId : cachedConsent.getClientScopeIds()) {
            ClientScopeModel clientScope = KeycloakModelUtils.findClientScopeById(realm, client, clientScopeId);
            if (clientScope != null) {
                consentModel.addGrantedClientScope(clientScope);
            }
        }

        return consentModel;
    }

    @Override
    public void setNotBeforeForUser(RealmModel realm, UserModel user, int notBefore) {
        if (!isRegisteredForInvalidation(realm, user.getId())) {
            UserModel foundUser = getUserById(realm, user.getId());
            if (foundUser instanceof UserAdapter) {
                ((UserAdapter) foundUser).invalidate();
            }
        }

        getDelegate().setNotBeforeForUser(realm, user, notBefore);

    }

    @Override
    public int getNotBeforeOfUser(RealmModel realm, UserModel user) {
        if (isRegisteredForInvalidation(realm, user.getId())) {
            return getDelegate().getNotBeforeOfUser(realm, user);
        }

        UserModel foundUser = getUserById(realm, user.getId());
        if (foundUser instanceof UserAdapter) {
            return ((UserAdapter) foundUser).cached.getNotBefore();
        } else {
            return getDelegate().getNotBeforeOfUser(realm, user);
        }
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        UserModel user = getDelegate().addUser(realm, id, username, addDefaultRoles, addDefaultRequiredActions);
        // just in case the transaction is rolled back you need to invalidate the user and all cache queries for that user
        fullyInvalidateUser(realm, user);
        addManagedUser(user.getId(), user);
        return user;
    }

    private void addManagedUser(String id, UserModel user) {
        if (EntityManagers.isBatchMode()) {
            return;
        }
        managedUsers.put(id, user);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserModel user = getDelegate().addUser(realm, username);
        // just in case the transaction is rolled back you need to invalidate the user and all cache queries for that user
        fullyInvalidateUser(realm, user);
        addManagedUser(user.getId(), user);
        return user;
    }

    // just in case the transaction is rolled back you need to invalidate the user and all cache queries for that user
    protected void fullyInvalidateUser(RealmModel realm, UserModel user) {
        if (user instanceof CachedUserModel) {
           ((CachedUserModel) user).invalidate();
        }

        Stream<FederatedIdentityModel> federatedIdentities = realm.isIdentityFederationEnabled() ?
                getFederatedIdentitiesStream(realm, user) : Stream.empty();

        UserFullInvalidationEvent event = UserFullInvalidationEvent.create(user.getId(), user.getUsername(), user.getEmail(), realm.getId(), realm.isIdentityFederationEnabled(), federatedIdentities);

        cache.fullUserInvalidation(user.getId(), user.getUsername(), user.getEmail(), realm.getId(), realm.isIdentityFederationEnabled(), event.getFederatedIdentities(), invalidations);
        invalidationEvents.add(event);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        fullyInvalidateUser(realm, user);
        return getDelegate().removeUser(realm, user);
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        invalidateFederationLink(user.getId());
        getDelegate().addFederatedIdentity(realm, user, socialLink);
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        invalidateFederationLink(federatedUser.getId());
        getDelegate().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);
    }

    private void invalidateFederationLink(String userId) {
        cache.federatedIdentityLinkUpdatedInvalidation(userId, invalidations);
        invalidationEvents.add(UserFederationLinkUpdatedEvent.create(userId));
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        // Needs to invalidate both directions
        FederatedIdentityModel socialLink = getFederatedIdentity(realm, user, socialProvider);

        UserFederationLinkRemovedEvent event = UserFederationLinkRemovedEvent.create(user.getId(), realm.getId(), socialLink);
        cache.federatedIdentityLinkRemovedInvalidation(user.getId(), realm.getId(), event.getIdentityProviderId(), event.getSocialUserId(), invalidations);
        invalidationEvents.add(event);

        return getDelegate().removeFederatedIdentity(realm, user, socialProvider);
    }

    @Override
    public void preRemove(RealmModel realm, IdentityProviderModel provider) {
        cache.addInvalidations(InIdentityProviderPredicate.create().provider(provider.getAlias()), invalidations);
        getDelegate().preRemove(realm, provider);
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().grantToAllUsers(realm, role);
    }

    @Override
    public void preRemove(RealmModel realm) {
        addRealmInvalidation(realm.getId());
        getDelegate().preRemove(realm);
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, role);
    }
    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, group);
    }


    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, client);
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        getDelegate().preRemove(protocolMapper);
    }

    @Override
    public void preRemove(ClientScopeModel clientScope) {
        // Not needed to invalidate realm probably. Just consents are affected ATM and they are checked if they exists
        getDelegate().preRemove(clientScope);
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
        if (!component.getProviderType().equals(UserStorageProvider.class.getName()) && !component.getProviderType().equals(ClientStorageProvider.class.getName())) return;
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, component);

    }

    @Override
    public void removeImportedUsers(RealmModel realm, String storageProviderId) {
        getDelegate().removeImportedUsers(realm, storageProviderId);
        clear();
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm
    }

    @Override
    public void unlinkUsers(RealmModel realm, String storageProviderId) {
        getDelegate().unlinkUsers(realm, storageProviderId);
        clear();
        addRealmInvalidation(realm.getId()); // easier to just invalidate whole realm

    }

    private void addRealmInvalidation(String realmId) {
        realmInvalidations.add(realmId);
        invalidationEvents.add(UserCacheRealmInvalidationEvent.create(realmId));
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        if (getDelegate() instanceof OnUpdateComponent) {
            ((OnUpdateComponent) getDelegate()).onUpdate(session, realm, oldModel, newModel);
        }
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        if (getDelegate() instanceof OnCreateComponent) {
            ((OnCreateComponent) getDelegate()).onCreate(session, realm, model);
        }
    }

    @Override
    public List<AttributeMetadata> decorateUserProfile(String providerId, UserProfileMetadata metadata) {
        if (getDelegate() instanceof UserProfileDecorator) {
            return ((UserProfileDecorator) getDelegate()).decorateUserProfile(providerId, metadata);
        }
        return List.of();
    }

    @Override
    public UserCredentialManager getUserCredentialManager(UserModel user) {
        return new org.keycloak.credential.UserCredentialManager(session, session.getContext().getRealm(), user);
    }

    public UserCacheManager getCache() {
        return cache;
    }

    public long getStartupRevision() {
        return startupRevision;
    }

    public void registerInvalidation(String id) {
        cache.invalidateCacheKey(id, invalidations);
        invalidationEvents.add(new CacheKeyInvalidatedEvent(id));
    }

    public boolean isInvalid(String key) {
        return invalidations.contains(key);
    }
}

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

import org.jboss.logging.Logger;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.models.cache.infinispan.entities.CachedFederatedIdentityLinks;
import org.keycloak.models.cache.infinispan.entities.CachedUser;
import org.keycloak.models.cache.infinispan.entities.CachedUserConsent;
import org.keycloak.models.cache.infinispan.entities.CachedUserConsents;
import org.keycloak.models.cache.infinispan.entities.UserListQuery;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCacheSession implements CacheUserProvider {
    protected static final Logger logger = Logger.getLogger(UserCacheSession.class);
    protected UserCacheManager cache;
    protected KeycloakSession session;
    protected UserProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;
    protected final long startupRevision;


    protected Set<String> invalidations = new HashSet<>();
    protected Set<String> realmInvalidations = new HashSet<>();
    protected Map<String, UserModel> managedUsers = new HashMap<>();

    public UserCacheSession(UserCacheManager cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;
        this.startupRevision = cache.getCurrentCounter();
        session.getTransactionManager().enlistAfterCompletion(getTransaction());
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public UserProvider getDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (delegate != null) return delegate;
        delegate = session.userStorageManager();

        return delegate;
    }

    public void registerUserInvalidation(RealmModel realm,CachedUser user) {
        invalidations.add(user.getId());
        if (user.getEmail() != null) invalidations.add(getUserByEmailCacheKey(realm.getId(), user.getEmail()));
        invalidations.add(getUserByUsernameCacheKey(realm.getId(), user.getUsername()));
        if (realm.isIdentityFederationEnabled()) invalidations.add(getFederatedIdentityLinksCacheKey(user.getId()));
    }

    protected void runInvalidations() {
        for (String realmId : realmInvalidations) {
            cache.invalidateRealmUsers(realmId, invalidations);
        }
        for (String invalidation : invalidations) {
            cache.invalidateObject(invalidation);
        }
    }

    private KeycloakTransaction getTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                if (delegate == null) return;
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
    public UserModel getUserById(String id, RealmModel realm) {
        logger.tracev("getuserById {0}", id);
        if (isRegisteredForInvalidation(realm, id)) {
            logger.trace("registered for invalidation return delegate");
            return getDelegate().getUserById(id, realm);
        }

        CachedUser cached = cache.get(id, CachedUser.class);
        if (cached == null) {
            logger.trace("not cached");
            Long loaded = cache.getCurrentRevision(id);
            UserModel model = getDelegate().getUserById(id, realm);
            if (model == null) {
                logger.trace("delegate returning null");
                return null;
            }
            if (managedUsers.containsKey(id)) {
                logger.trace("return managedusers");
                return managedUsers.get(id);
            }
            if (invalidations.contains(id)) return model;
            cached = new CachedUser(loaded, realm, model);
            cache.addRevisioned(cached, startupRevision);
        } else if (managedUsers.containsKey(id)) {
            logger.trace("return managedusers");
            return managedUsers.get(id);
        }
        logger.trace("returning new cache adapter");
        UserAdapter adapter = new UserAdapter(cached, this, session, realm);
        managedUsers.put(id, adapter);
        return adapter;
    }

    public String getUserByUsernameCacheKey(String realmId, String username) {
        return realmId + ".username." + username;
    }

    public String getUserByEmailCacheKey(String realmId, String email) {
        return realmId + ".email." + email;
    }

    public String getUserByFederatedIdentityCacheKey(String realmId, FederatedIdentityModel socialLink) {
        return realmId + ".idp." + socialLink.getIdentityProvider() + "." + socialLink.getUserId();
    }

    public String getFederatedIdentityLinksCacheKey(String userId) {
        return userId + ".idplinks";
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        logger.tracev("getUserByUsername: {0}", username);
        username = username.toLowerCase();
        if (realmInvalidations.contains(realm.getId())) {
            logger.tracev("realmInvalidations");
            return getDelegate().getUserByUsername(username, realm);
        }
        String cacheKey = getUserByUsernameCacheKey(realm.getId(), username);
        if (invalidations.contains(cacheKey)) {
            logger.tracev("invalidations");
            return getDelegate().getUserByUsername(username, realm);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        String userId = null;
        if (query == null) {
            logger.tracev("query null");
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getUserByUsername(username, realm);
            if (model == null) {
                logger.tracev("model from delegate null");
                return null;
            }
            userId = model.getId();
            query = new UserListQuery(loaded, cacheKey, realm, model.getId());
            cache.addRevisioned(query, startupRevision);
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) {
                logger.tracev("return managed user");
                return managedUsers.get(userId);
            }

            CachedUser cached = cache.get(userId, CachedUser.class);
            if (cached == null) {
                cached = new CachedUser(loaded, realm, model);
                cache.addRevisioned(cached, startupRevision);
            }
            logger.trace("return new cache adapter");
            UserAdapter adapter = new UserAdapter(cached, this, session, realm);
            managedUsers.put(userId, adapter);
            return adapter;
        } else {
            userId = query.getUsers().iterator().next();
            if (invalidations.contains(userId)) {
                logger.tracev("invalidated cache return delegate");
                return getDelegate().getUserByUsername(username, realm);

            }
            logger.trace("return getUserById");
            return getUserById(userId, realm);
        }
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        if (email == null) return null;
        email = email.toLowerCase();
        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByEmail(email, realm);
        }
        String cacheKey = getUserByEmailCacheKey(realm.getId(), email);
        if (invalidations.contains(cacheKey)) {
            return getDelegate().getUserByEmail(email, realm);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        String userId = null;
        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getUserByEmail(email, realm);
            if (model == null) return null;
            userId = model.getId();
            query = new UserListQuery(loaded, cacheKey, realm, model.getId());
            cache.addRevisioned(query, startupRevision);
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) return managedUsers.get(userId);

            CachedUser cached = cache.get(userId, CachedUser.class);
            if (cached == null) {
                cached = new CachedUser(loaded, realm, model);
                cache.addRevisioned(cached, startupRevision);
            }
            UserAdapter adapter = new UserAdapter(cached, this, session, realm);
            managedUsers.put(userId, adapter);
            return adapter;
        } else {
            userId = query.getUsers().iterator().next();
            if (invalidations.contains(userId)) {
                return getDelegate().getUserByEmail(email, realm);

            }
            return getUserById(userId, realm);
        }
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        if (socialLink == null) return null;
        if (!realm.isIdentityFederationEnabled()) return null;

        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByFederatedIdentity(socialLink, realm);
        }
        String cacheKey = getUserByFederatedIdentityCacheKey(realm.getId(), socialLink);
        if (invalidations.contains(cacheKey)) {
            return getDelegate().getUserByFederatedIdentity(socialLink, realm);
        }
        UserListQuery query = cache.get(cacheKey, UserListQuery.class);

        String userId = null;
        if (query == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            UserModel model = getDelegate().getUserByFederatedIdentity(socialLink, realm);
            if (model == null) return null;
            userId = model.getId();
            query = new UserListQuery(loaded, cacheKey, realm, userId);
            cache.addRevisioned(query, startupRevision);
            if (invalidations.contains(userId)) return model;
            if (managedUsers.containsKey(userId)) return managedUsers.get(userId);

            CachedUser cached = cache.get(userId, CachedUser.class);
            if (cached == null) {
                cached = new CachedUser(loaded, realm, model);
                cache.addRevisioned(cached, startupRevision);
            }
            UserAdapter adapter = new UserAdapter(cached, this, session, realm);
            managedUsers.put(userId, adapter);
            return adapter;
        } else {
            userId = query.getUsers().iterator().next();
            if (invalidations.contains(userId)) {
                invalidations.add(cacheKey);
                return getDelegate().getUserByFederatedIdentity(socialLink, realm);

            }
            return getUserById(userId, realm);
        }
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return getDelegate().getGroupMembers(realm, group, firstResult, maxResults);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getDelegate().getGroupMembers(realm, group);
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        // Just an attempt to find the user from cache by default serviceAccount username
        String username = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId();
        UserModel user = getUserByUsername(username, client.getRealm());
        if (user != null && user.getServiceAccountClientLink() != null && user.getServiceAccountClientLink().equals(client.getId())) {
            return user;
        }

        return getDelegate().getServiceAccount(client);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
        return getDelegate().getUsers(realm, includeServiceAccounts);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return getDelegate().getUsersCount(realm);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts) {
        return getDelegate().getUsers(realm, firstResult, maxResults, includeServiceAccounts);
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
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return getDelegate().searchForUser(search, realm);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return getDelegate().searchForUser(search, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return getDelegate().searchForUser(attributes, realm);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        return getDelegate().searchForUser(attributes, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return getDelegate().searchForUserByUserAttribute(attrName, attrValue, realm);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        logger.tracev("getFederatedIdentities: {0}", user.getUsername());

        String cacheKey = getFederatedIdentityLinksCacheKey(user.getId());
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(user.getId()) || invalidations.contains(cacheKey)) {
            return getDelegate().getFederatedIdentities(user, realm);
        }

        CachedFederatedIdentityLinks cachedLinks = cache.get(cacheKey, CachedFederatedIdentityLinks.class);

        if (cachedLinks == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            Set<FederatedIdentityModel> federatedIdentities = getDelegate().getFederatedIdentities(user, realm);
            cachedLinks = new CachedFederatedIdentityLinks(loaded, cacheKey, realm, federatedIdentities);
            cache.addRevisioned(cachedLinks, startupRevision);
            return federatedIdentities;
        } else {
            return new HashSet<>(cachedLinks.getFederatedIdentities());
        }
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        logger.tracev("getFederatedIdentity: {0} {1}", user.getUsername(), socialProvider);

        String cacheKey = getFederatedIdentityLinksCacheKey(user.getId());
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(user.getId()) || invalidations.contains(cacheKey)) {
            return getDelegate().getFederatedIdentity(user, socialProvider, realm);
        }

        Set<FederatedIdentityModel> federatedIdentities = getFederatedIdentities(user, realm);
        for (FederatedIdentityModel socialLink : federatedIdentities) {
            if (socialLink.getIdentityProvider().equals(socialProvider)) {
                return socialLink;
            }
        }
        return null;
    }

    @Override
    public void updateConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        invalidations.add(getConsentCacheKey(user.getId()));
        getDelegate().updateConsent(realm, user, consent);
    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, UserModel user, String clientInternalId) {
        invalidations.add(getConsentCacheKey(user.getId()));
        return getDelegate().revokeConsentForClient(realm, user, clientInternalId);
    }

    public String getConsentCacheKey(String userId) {
        return userId + ".consents";
    }


    @Override
    public void addConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        invalidations.add(getConsentCacheKey(user.getId()));
        getDelegate().addConsent(realm, user, consent);
    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, UserModel user, String clientId) {
        logger.tracev("getConsentByClient: {0}", user.getUsername());

        String cacheKey = getConsentCacheKey(user.getId());
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(user.getId()) || invalidations.contains(cacheKey)) {
            return getDelegate().getConsentByClient(realm, user, clientId);
        }

        CachedUserConsents cached = cache.get(cacheKey, CachedUserConsents.class);

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<UserConsentModel> consents = getDelegate().getConsents(realm, user);
            cached = new CachedUserConsents(loaded, cacheKey, realm, consents);
            cache.addRevisioned(cached, startupRevision);
        }
        CachedUserConsent cachedConsent = cached.getConsents().get(clientId);
        if (cachedConsent == null) return null;
        return toConsentModel(realm, cachedConsent);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, UserModel user) {
        logger.tracev("getConsents: {0}", user.getUsername());

        String cacheKey = getConsentCacheKey(user.getId());
        if (realmInvalidations.contains(realm.getId()) || invalidations.contains(user.getId()) || invalidations.contains(cacheKey)) {
            return getDelegate().getConsents(realm, user);
        }

        CachedUserConsents cached = cache.get(cacheKey, CachedUserConsents.class);

        if (cached == null) {
            Long loaded = cache.getCurrentRevision(cacheKey);
            List<UserConsentModel> consents = getDelegate().getConsents(realm, user);
            cached = new CachedUserConsents(loaded, cacheKey, realm, consents);
            cache.addRevisioned(cached, startupRevision);
            return consents;
        } else {
            List<UserConsentModel> result = new LinkedList<>();
            for (CachedUserConsent cachedConsent : cached.getConsents().values()) {
                UserConsentModel consent = toConsentModel(realm, cachedConsent);
                if (consent != null) {
                    result.add(consent);
                }
            }
            return result;
        }
    }

    private UserConsentModel toConsentModel(RealmModel realm, CachedUserConsent cachedConsent) {
        ClientModel client = session.realms().getClientById(cachedConsent.getClientDbId(), realm);
        if (client == null) {
            return null;
        }

        UserConsentModel consentModel = new UserConsentModel(client);

        for (String roleId : cachedConsent.getRoleIds()) {
            RoleModel role = session.realms().getRoleById(roleId, realm);
            if (role != null) {
                consentModel.addGrantedRole(role);
            }
        }
        for (ProtocolMapperModel protocolMapper : cachedConsent.getProtocolMappers()) {
            consentModel.addGrantedProtocolMapper(protocolMapper);
        }
        return consentModel;
    }


    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        UserModel user = getDelegate().addUser(realm, id, username, addDefaultRoles, addDefaultRoles);
        // just in case the transaction is rolled back you need to invalidate the user and all cache queries for that user
        invalidateUser(realm, user);
        managedUsers.put(user.getId(), user);
        return user;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserModel user = getDelegate().addUser(realm, username);
        // just in case the transaction is rolled back you need to invalidate the user and all cache queries for that user
        invalidateUser(realm, user);
        managedUsers.put(user.getId(), user);
        return user;
    }

    protected void invalidateUser(RealmModel realm, UserModel user) {
        // just in case the transaction is rolled back you need to invalidate the user and all cache queries for that user

        if (realm.isIdentityFederationEnabled()) {
            // Invalidate all keys for lookup this user by any identityProvider link
            Set<FederatedIdentityModel> federatedIdentities = getFederatedIdentities(user, realm);
            for (FederatedIdentityModel socialLink : federatedIdentities) {
                String fedIdentityCacheKey = getUserByFederatedIdentityCacheKey(realm.getId(), socialLink);
                invalidations.add(fedIdentityCacheKey);
            }

            // Invalidate federationLinks of user
            invalidations.add(getFederatedIdentityLinksCacheKey(user.getId()));
        }

        invalidations.add(user.getId());
        if (user.getEmail() != null) invalidations.add(getUserByEmailCacheKey(realm.getId(), user.getEmail()));
        invalidations.add(getUserByUsernameCacheKey(realm.getId(), user.getUsername()));
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        invalidateUser(realm, user);
        return getDelegate().removeUser(realm, user);
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        invalidations.add(getFederatedIdentityLinksCacheKey(user.getId()));
        getDelegate().addFederatedIdentity(realm, user, socialLink);
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        invalidations.add(getFederatedIdentityLinksCacheKey(federatedUser.getId()));
        getDelegate().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        // Needs to invalidate both directions
        FederatedIdentityModel socialLink = getFederatedIdentity(user, socialProvider, realm);
        invalidations.add(getFederatedIdentityLinksCacheKey(user.getId()));
        if (socialLink != null) {
            invalidations.add(getUserByFederatedIdentityCacheKey(realm.getId(), socialLink));
        }

        return getDelegate().removeFederatedIdentity(realm, user, socialProvider);
    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        return getDelegate().validCredentials(session, realm, user, input);
    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, UserCredentialModel... input) {
        return getDelegate().validCredentials(session, realm, user, input);
    }

    @Override
    public CredentialValidationOutput validCredentials(KeycloakSession session, RealmModel realm, UserCredentialModel... input) {
        return getDelegate().validCredentials(session, realm, input);
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        realmInvalidations.add(realm.getId()); // easier to just invalidate whole realm
        getDelegate().grantToAllUsers(realm, role);
    }

    @Override
    public void preRemove(RealmModel realm) {
        realmInvalidations.add(realm.getId());
        getDelegate().preRemove(realm);
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        getDelegate().preRemove(realm, role);
    }
    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        getDelegate().preRemove(realm, group);
    }


    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel link) {
        realmInvalidations.add(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, link);
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        realmInvalidations.add(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, client);
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        getDelegate().preRemove(protocolMapper);
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
        getDelegate().preRemove(realm, component);

    }
}

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

import org.keycloak.models.*;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.models.cache.infinispan.entities.CachedUser;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultCacheUserProvider implements CacheUserProvider {
    protected UserCache cache;
    protected KeycloakSession session;
    protected UserProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Map<String, String> userInvalidations = new HashMap<>();
    protected Set<String> realmInvalidations = new HashSet<>();
    protected Map<String, UserModel> managedUsers = new HashMap<>();

    public DefaultCacheUserProvider(UserCache cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;

        session.getTransaction().enlistAfterCompletion(getTransaction());
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public UserProvider getDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (delegate != null) return delegate;
        delegate = session.getProvider(UserProvider.class);
        return delegate;
    }

    @Override
    public void registerUserInvalidation(RealmModel realm, String id) {
        userInvalidations.put(id, realm.getId());
    }

    protected void runInvalidations() {
        for (Map.Entry<String, String> invalidation : userInvalidations.entrySet()) {
            cache.invalidateCachedUserById(invalidation.getValue(), invalidation.getKey());
        }
        for (String realmId : realmInvalidations) {
            cache.invalidateRealmUsers(realmId);
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
        return realmInvalidations.contains(realm.getId()) || userInvalidations.containsKey(userId);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        if (isRegisteredForInvalidation(realm, id)) {
            return getDelegate().getUserById(id, realm);
        }

        CachedUser cached = cache.getCachedUser(realm.getId(), id);
        if (cached == null) {
            UserModel model = getDelegate().getUserById(id, realm);
            if (model == null) return null;
            if (managedUsers.containsKey(id)) return managedUsers.get(id);
            if (userInvalidations.containsKey(id)) return model;
            cached = new CachedUser(realm, model);
            cache.addCachedUser(realm.getId(), cached);
        } else if (managedUsers.containsKey(id)) {
            return managedUsers.get(id);
        }
        UserAdapter adapter = new UserAdapter(cached, this, session, realm);
        managedUsers.put(id, adapter);
        return adapter;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        
        username = username.toLowerCase();
        
        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByUsername(username, realm);
        }
        CachedUser cached = cache.getCachedUserByUsername(realm.getId(), username);
        if (cached == null) {
            UserModel model = getDelegate().getUserByUsername(username, realm);
            if (model == null) return null;
            if (managedUsers.containsKey(model.getId())) return managedUsers.get(model.getId());
            if (userInvalidations.containsKey(model.getId())) return model;
            cached = new CachedUser(realm, model);
            cache.addCachedUser(realm.getId(), cached);
        } else if (userInvalidations.containsKey(cached.getId())) {
            return getDelegate().getUserById(cached.getId(), realm);
        } else if (managedUsers.containsKey(cached.getId())) {
            return managedUsers.get(cached.getId());
        }
        UserAdapter adapter = new UserAdapter(cached, this, session, realm);
        managedUsers.put(cached.getId(), adapter);
        return adapter;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        if (email == null) return null;
        
        email = email.toLowerCase();
        
        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByEmail(email, realm);
        }
        CachedUser cached = cache.getCachedUserByEmail(realm.getId(), email);
        if (cached == null) {
            UserModel model = getDelegate().getUserByEmail(email, realm);
            if (model == null) return null;
            if (userInvalidations.containsKey(model.getId())) return model;
            cached = new CachedUser(realm, model);
            cache.addCachedUser(realm.getId(), cached);
        } else if (userInvalidations.containsKey(cached.getId())) {
            return getDelegate().getUserByEmail(email, realm);
        } else if (managedUsers.containsKey(cached.getId())) {
            return managedUsers.get(cached.getId());
        }
        UserAdapter adapter = new UserAdapter(cached, this, session, realm);
        managedUsers.put(cached.getId(), adapter);
        return adapter;
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        return getDelegate().getUserByFederatedIdentity(socialLink, realm);
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
    public UserModel getUserByServiceAccountClient(ClientModel client) {
        return getDelegate().getUserByServiceAccountClient(client);
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
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return getDelegate().searchForUser(search, realm);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return getDelegate().searchForUser(search, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return getDelegate().searchForUserByAttributes(attributes, realm);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        return getDelegate().searchForUserByAttributes(attributes, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return getDelegate().searchForUserByUserAttribute(attrName, attrValue, realm);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        return getDelegate().getFederatedIdentities(user, realm);
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        return getDelegate().getFederatedIdentity(user, socialProvider, realm);
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        UserModel user = getDelegate().addUser(realm, id, username, addDefaultRoles, addDefaultRoles);
        managedUsers.put(user.getId(), user);
        return user;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserModel user = getDelegate().addUser(realm, username);
        managedUsers.put(user.getId(), user);
        return user;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        registerUserInvalidation(realm, user.getId());
        return getDelegate().removeUser(realm, user);
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        getDelegate().addFederatedIdentity(realm, user, socialLink);
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        getDelegate().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
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
}

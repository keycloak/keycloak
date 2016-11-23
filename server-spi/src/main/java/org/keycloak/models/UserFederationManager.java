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

package org.keycloak.models;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserFederationManager implements UserProvider {

    private static final Logger logger = Logger.getLogger(UserFederationManager.class);

    protected KeycloakSession session;

    // Set of already validated/proxied federation users during this session. Key is user ID
    private Map<String, UserModel> managedUsers = new HashMap<>();

    public UserFederationManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        UserModel user = session.userStorage().addUser(realm, id, username.toLowerCase(), addDefaultRoles, addDefaultRequiredActions);
        return registerWithFederation(realm, user);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserModel user = session.userStorage().addUser(realm, username.toLowerCase());
        return registerWithFederation(realm, user);
    }

    protected UserModel registerWithFederation(RealmModel realm, UserModel user) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            if (fed.synchronizeRegistrations()) {
                user.setFederationLink(federation.getId());
                UserModel registered = fed.register(realm, user);
                managedUsers.put(registered.getId(), registered);
                return registered;
            }
        }
        return user;
    }

    public UserFederationProvider getFederationProvider(UserFederationProviderModel model) {
        UserFederationProviderFactory factory = (UserFederationProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, model.getProviderName());
        return factory.getInstance(session, model);
    }

    public UserFederationProvider getFederationLink(RealmModel realm, UserModel user) {
        if (user.getFederationLink() == null) return null;
        for (UserFederationProviderModel fed : realm.getUserFederationProviders()) {
            if (fed.getId().equals(user.getFederationLink())) {
                return getFederationProvider(fed);
            }
        }
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        UserFederationProvider link = getFederationLink(realm, user);
        if (link != null) {
            boolean fedRemoved = link.removeUser(realm, user);
            if (fedRemoved) {
                boolean localRemoved = session.userStorage().removeUser(realm, user);
                managedUsers.remove(user.getId());
                if (!localRemoved) {
                    logger.warn("User possibly removed from federation provider, but failed to remove him from keycloak model");
                }
                return localRemoved;
            } else {
                logger.warn("Failed to remove user from federation provider");
                return false;
            }
        }
        return session.userStorage().removeUser(realm, user);

    }

    public void validateUser(RealmModel realm, UserModel user) {
        if (managedUsers.containsKey(user.getId())) {
            return;
        }

        UserFederationProvider link = getFederationLink(realm, user);
        if (link != null  && !link.isValid(realm, user)) {
            deleteInvalidUser(realm, user);
            throw new IllegalStateException("Federated user no longer valid");
        }

    }

    protected void deleteInvalidUser(final RealmModel realm, final UserModel user) {
        runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                RealmModel realmModel = session.realms().getRealm(realm.getId());
                if (realmModel == null) return;
                UserModel deletedUser = session.userStorage().getUserById(user.getId(), realmModel);
                new UserManager(session).removeUser(realmModel, deletedUser, session.userStorage());
                logger.debugf("Removed invalid user '%s'", user.getUsername());
            }

        });
    }

    private  static void runJobInTransaction(KeycloakSessionFactory factory, KeycloakSessionTask task) {
        KeycloakSession session = factory.create();
        KeycloakTransaction tx = session.getTransactionManager();
        try {
            tx.begin();
            task.run(session);

            if (tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
        } catch (RuntimeException re) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw re;
        } finally {
            session.close();
        }
    }

    protected UserModel validateAndProxyUser(RealmModel realm, UserModel user) {
        UserModel managed = managedUsers.get(user.getId());
        if (managed != null) {
            return managed;
        }

        UserFederationProvider link = getFederationLink(realm, user);
        if (link != null) {
            UserModel validatedProxyUser = link.validateAndProxy(realm, user);
            if (validatedProxyUser != null) {
                managedUsers.put(user.getId(), validatedProxyUser);
                return validatedProxyUser;
            } else {
                deleteInvalidUser(realm, user);
                return null;
            }
        }
        return user;
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        validateUser(realm, user);
        session.userStorage().addFederatedIdentity(realm, user, socialLink);
    }

    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        session.userStorage().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        validateUser(realm, user);
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        return session.userStorage().removeFederatedIdentity(realm, user, socialProvider);
    }

    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        session.userStorage().addConsent(realm, userId, consent);

    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientInternalId) {
        return session.userStorage().getConsentByClient(realm, userId, clientInternalId);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, String userId) {
        return session.userStorage().getConsents(realm, userId);
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        session.userStorage().updateConsent(realm, userId, consent);

    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId) {
        return session.userStorage().revokeConsentForClient(realm, userId, clientInternalId);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        UserModel user = session.userStorage().getUserById(id, realm);
        if (user != null) {
            user = validateAndProxyUser(realm, user);
        }
        return user;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, final GroupModel group, int firstResult, int maxResults) {
        // Not very effective. For the page X, it is loading also all previous pages 0..X-1 . Improve if needed...
        int maxTotal = firstResult + maxResults;
        List<UserModel> localMembers = query(new PaginatedQuery() {

            @Override
            public List<UserModel> query(RealmModel realm, int first, int max) {
                return session.userStorage().getGroupMembers(realm, group, first, max);
            }

        }, realm, 0, maxTotal);

        Set<UserModel> result = new LinkedHashSet<>(localMembers);

        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            if (result.size() >= maxTotal) {
                break;
            }

            int max = maxTotal - result.size();

            UserFederationProvider fed = getFederationProvider(federation);
            List<UserModel> current = fed.getGroupMembers(realm, group, 0, max);
            if (current != null) {
                result.addAll(current);
            }
        }

        if (result.size() <= firstResult) {
            return Collections.emptyList();
        }

        int max = Math.min(maxTotal, result.size());
        return new ArrayList<>(result).subList(firstResult, max);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getGroupMembers(realm, group, -1, -1);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        UserModel user = session.userStorage().getUserByUsername(username.toLowerCase(), realm);
        if (user != null) {
            user = validateAndProxyUser(realm, user);
            if (user != null) return user;
        }
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            user = fed.getUserByUsername(realm, username);
            if (user != null) return user;
        }
        return user;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        UserModel user = session.userStorage().getUserByEmail(email.toLowerCase(), realm);
        if (user != null) {
            user = validateAndProxyUser(realm, user);
            if (user != null) return user;
        }
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            user = fed.getUserByEmail(realm, email);
            if (user != null) return user;
        }
        return user;
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        UserModel user = session.userStorage().getUserByFederatedIdentity(socialLink, realm);
        if (user != null) {
            user = validateAndProxyUser(realm, user);
        }
        return user;
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        UserModel user = session.userStorage().getServiceAccount(client);
        if (user != null) {
            user = validateAndProxyUser(client.getRealm(), user);
        }
        return user;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
        return getUsers(realm, 0, Integer.MAX_VALUE - 1, includeServiceAccounts);

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
    public int getUsersCount(RealmModel realm) {
        return session.userStorage().getUsersCount(realm);
    }

    interface PaginatedQuery {
        List<UserModel> query(RealmModel realm, int first, int max);
    }

    protected List<UserModel> query(PaginatedQuery pagedQuery, RealmModel realm, int firstResult, int maxResults) {
        List<UserModel> results = new LinkedList<UserModel>();
        if (maxResults == 0) return results;
        int first = firstResult;
        int max = maxResults;
        do {
            List<UserModel> query = pagedQuery.query(realm, first, max);
            if (query == null || query.size() == 0) return results;
            int added = 0;
            for (UserModel user : query) {
                user = validateAndProxyUser(realm, user);
                if (user == null) continue;
                results.add(user);
                added++;
            }
            if (results.size() == maxResults) return results;
            if (query.size() < max) return results;
            first = query.size();
            max -= added;
            if (max <= 0) return results;
        } while (true);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, final boolean includeServiceAccounts) {
        return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(RealmModel realm, int first, int max) {
                return session.userStorage().getUsers(realm, first, max, includeServiceAccounts);
            }
        }, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE - 1);
    }

    void federationLoad(RealmModel realm, Map<String, String> attributes) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            fed.searchByAttributes(attributes, realm, 30);
        }
    }

    @Override
    public List<UserModel> searchForUser(final String search, RealmModel realm, int firstResult, int maxResults) {
        Map<String, String> attributes = new HashMap<String, String>();
        int spaceIndex = search.lastIndexOf(' ');
        if (spaceIndex > -1) {
            String firstName = search.substring(0, spaceIndex).trim();
            String lastName = search.substring(spaceIndex).trim();
            attributes.put(UserModel.FIRST_NAME, firstName);
            attributes.put(UserModel.LAST_NAME, lastName);
        } else if (search.indexOf('@') > -1) {
            attributes.put(UserModel.USERNAME, search.trim().toLowerCase());
            attributes.put(UserModel.EMAIL, search.trim().toLowerCase());
        } else {
            attributes.put(UserModel.LAST_NAME, search.trim());
            attributes.put(UserModel.USERNAME, search.trim().toLowerCase());
        }
        federationLoad(realm, attributes);

        List<UserModel> result = query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(RealmModel realm, int first, int max) {
                return session.userStorage().searchForUser(search, realm, first, max);
            }
        }, realm, firstResult, maxResults);

        // see KEYCLOAK-3879
        UserModel userById = getUserById(search.trim(), realm);
        if (userById != null) {
            result.add(userById);
        }

        return result;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return searchForUser(attributes, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUser(final Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        federationLoad(realm, attributes);
        return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(RealmModel realm, int first, int max) {
                return session.userStorage().searchForUser(attributes, realm, first, max);
            }
        }, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return session.userStorage().searchForUserByUserAttribute(attrName, attrValue, realm);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        validateUser(realm, user);
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        return session.userStorage().getFederatedIdentities(user, realm);
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        validateUser(realm, user);
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        return session.userStorage().getFederatedIdentity(user, socialProvider, realm);
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        // not federation-aware for now
        session.userStorage().grantToAllUsers(realm, role);
    }

    @Override
    public void preRemove(RealmModel realm) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            fed.preRemove(realm);
        }
        session.userStorage().preRemove(realm);
    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel model) {
        session.userStorage().preRemove(realm, model);
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            fed.preRemove(realm, group);
        }
        session.userStorage().preRemove(realm, group);

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        for (UserFederationProviderModel federation : realm.getUserFederationProviders()) {
            UserFederationProvider fed = getFederationProvider(federation);
            fed.preRemove(realm, role);
        }
        session.userStorage().preRemove(realm, role);
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        session.userStorage().preRemove(realm, client);
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        session.userStorage().preRemove(protocolMapper);
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
        session.userStorage().preRemove(realm, component);

    }

    @Override
    public void close() {
    }
}

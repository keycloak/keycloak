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
import org.keycloak.models.ClientModel;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.storage.user.UserCredentialAuthenticationProvider;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.storage.user.UserCredentialValidatorProvider;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserUpdateProvider;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserStorageManager implements UserProvider {

    private static final Logger logger = Logger.getLogger(UserStorageManager.class);

    protected KeycloakSession session;

    // Set of already validated/proxied federation users during this session. Key is user ID
    private Map<String, UserModel> managedUsers = new HashMap<>();
    private UserProvider localStorage = null;

    public UserStorageManager(KeycloakSession session) {
        this.session = session;
    }

    protected UserProvider localStorage() {
        return session.userLocalStorage();
    }

    protected List<StorageProviderModel> getStorageProviders(RealmModel realm) {
        return realm.getStorageProviders();
    }

    protected <T> T getFirstStorageProvider(RealmModel realm, Class<T> type) {
        for (StorageProviderModel model : getStorageProviders(realm)) {
            StorageProviderFactory factory = (StorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(StorageProvider.class, model.getProviderName());
            if (factory.supports(type)) {
                return type.cast(factory.getInstance(session, model));
            }
        }
        return null;
    }

    protected <T> List<T> getStorageProviders(RealmModel realm, Class<T> type) {
        List<T> list = new LinkedList<>();
        for (StorageProviderModel model : getStorageProviders(realm)) {
            StorageProviderFactory factory = (StorageProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(StorageProvider.class, model.getProviderName());
            if (factory.supports(type)) {
                list.add(type.cast(factory.getInstance(session, model)));
            }


        }
        return list;
    }


    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        UserUpdateProvider registry = getFirstStorageProvider(realm, UserUpdateProvider.class);
        if (registry != null) {
            return registry.addUser(realm, id, username, addDefaultRoles, addDefaultRequiredActions);
        }
        return localStorage().addUser(realm, id, username.toLowerCase(), addDefaultRoles, addDefaultRequiredActions);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserUpdateProvider registry = getFirstStorageProvider(realm, UserUpdateProvider.class);
        if (registry != null) {
            return registry.addUser(realm, username);
        }
        return localStorage().addUser(realm, username.toLowerCase());
    }

    public StorageProvider getStorageProvider(StorageProviderModel model) {
        StorageProviderFactory factory = (StorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(StorageProvider.class, model.getProviderName());
        return factory.getInstance(session, model);
    }

    public StorageProvider getStorageProvider(RealmModel realm, String providerId) {
        StorageProviderModel model = realm.getStorageProvider(providerId);
        if (model == null) return null;
        StorageProviderFactory factory = (StorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(StorageProvider.class, model.getProviderName());
        if (factory == null) {
            throw new ModelException("Could not find StorageProviderFactory for: " + model.getProviderName());
        }
        return factory.getInstance(session, model);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        StorageId storageId = new StorageId(user.getId());
        if (storageId.getProviderId() == null) {
            return localStorage().removeUser(realm, user);
        }
        UserUpdateProvider registry = (UserUpdateProvider)getStorageProvider(realm, storageId.getProviderId());
        if (registry == null) {
            throw new ModelException("Could not resolve StorageProvider: " + storageId.getProviderId());
        }
        return registry.removeUser(realm, user);

    }

    public UserFederatedStorageProvider getFederatedStorage() {
        return session.userFederatedStorage();
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        if (StorageId.isLocalStorage(user)) {
            localStorage().addFederatedIdentity(realm, user, socialLink);
        } else {
            getFederatedStorage().addFederatedIdentity(realm, user, socialLink);
        }
    }

    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        if (StorageId.isLocalStorage(federatedUser)) {
            localStorage().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);

        } else {
            getFederatedStorage().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);
        }
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        if (StorageId.isLocalStorage(user)) {
            return localStorage().removeFederatedIdentity(realm, user, socialProvider);
        } else {
            return getFederatedStorage().removeFederatedIdentity(realm, user, socialProvider);
        }
    }

    @Override
    public void addConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        if (StorageId.isLocalStorage(user)) {
            localStorage().addConsent(realm, user, consent);
        } else {
            getFederatedStorage().addConsent(realm, user, consent);
        }

    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, UserModel user, String clientInternalId) {
        if (StorageId.isLocalStorage(user)) {
            return localStorage().getConsentByClient(realm, user, clientInternalId);
        } else {
            return getFederatedStorage().getConsentByClient(realm, user, clientInternalId);
        }
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, UserModel user) {
        if (StorageId.isLocalStorage(user)) {
            return localStorage().getConsents(realm, user);

        } else {
            return getFederatedStorage().getConsents(realm, user);
        }
    }

    @Override
    public void updateConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        if (StorageId.isLocalStorage(user)) {
            localStorage().updateConsent(realm, user, consent);
        } else {
            getFederatedStorage().updateConsent(realm, user, consent);
        }

    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, UserModel user, String clientInternalId) {
        if (StorageId.isLocalStorage(user)) {
            return localStorage().revokeConsentForClient(realm, user, clientInternalId);
        } else {
            return getFederatedStorage().revokeConsentForClient(realm, user, clientInternalId);
        }
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        if (storageId.getProviderId() == null) {
            return localStorage().getUserById(id, realm);
        }
        UserLookupProvider provider = (UserLookupProvider)getStorageProvider(realm, storageId.getProviderId());
        return provider.getUserById(id, realm);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getGroupMembers(realm, group, -1, -1);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        UserModel user = localStorage().getUserByUsername(username, realm);
        if (user != null) return user;
        for (UserLookupProvider provider : getStorageProviders(realm, UserLookupProvider.class)) {
            user = provider.getUserByUsername(username, realm);
            if (user != null) return user;
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        UserModel user = localStorage().getUserByEmail(email, realm);
        if (user != null) return user;
        for (UserLookupProvider provider : getStorageProviders(realm, UserLookupProvider.class)) {
            user = provider.getUserByEmail(email, realm);
            if (user != null) return user;
        }
        return null;
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        UserModel user = localStorage().getUserByFederatedIdentity(socialLink, realm);
        if (user != null) {
            return user;
        }
        String id = getFederatedStorage().getUserByFederatedIdentity(socialLink, realm);
        if (id != null) return getUserById(id, realm);
        return null;
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        return localStorage().getServiceAccount(client);
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
        int size = localStorage().getUsersCount(realm);
        for (UserQueryProvider provider : getStorageProviders(realm, UserQueryProvider.class)) {
            size += provider.getUsersCount(realm);
        }
        return size;
    }

    interface PaginatedQuery {
        List<UserModel> query(UserQueryProvider provider, int first, int max);
    }

    protected List<UserModel> query(PaginatedQuery pagedQuery, RealmModel realm, int firstResult, int maxResults) {
        List<UserModel> results = new LinkedList<UserModel>();
        if (maxResults == 0) return results;


        List<UserQueryProvider> storageProviders = getStorageProviders(realm, UserQueryProvider.class);
        LinkedList<UserQueryProvider> providers = new LinkedList<>();
        if (providers.isEmpty()) {
            return pagedQuery.query(localStorage(), firstResult, maxResults);
        }
        providers.add(localStorage());
        providers.addAll(storageProviders);

        int leftToRead = maxResults;
        int leftToFirstResult = firstResult;

        Iterator<UserQueryProvider> it = providers.iterator();
        while (it.hasNext() && leftToRead != 0) {
            UserQueryProvider provider = it.next();
            boolean exhausted = false;
            int index = 0;
            if (leftToFirstResult > 0) {
                do {
                    int toRead = Math.min(50, leftToFirstResult);
                    List<UserModel> tmp = pagedQuery.query(provider, index, toRead);
                    leftToFirstResult -= tmp.size();
                    index += tmp.size();
                    if (tmp.size() < toRead) {
                        exhausted = true;
                        break;
                    }
                } while (leftToFirstResult > 0);
            }
            if (exhausted) continue;
            List<UserModel> tmp = pagedQuery.query(provider, index, leftToRead);
            results.addAll(tmp);
            if (leftToRead > 0) leftToRead -= tmp.size();
        }
        return results;
    }

    @Override
    public List<UserModel> getUsers(final RealmModel realm, int firstResult, int maxResults, final boolean includeServiceAccounts) {
        return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(UserQueryProvider provider, int first, int max) {
                 if (provider instanceof UserProvider) { // it is local storage
                     return ((UserProvider)provider).getUsers(realm, first, max, includeServiceAccounts);
                 }
                return provider.getUsers(realm, first, max);
            }
        }, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUser(final String search, final RealmModel realm, int firstResult, int maxResults) {
         return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(UserQueryProvider provider, int first, int max) {
                return provider.searchForUser(search, realm, first, max);
            }
        }, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return searchForUserByAttributes(attributes, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(final Map<String, String> attributes, final RealmModel realm, int firstResult, int maxResults) {
        return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(UserQueryProvider provider, int first, int max) {
                return provider.searchForUserByAttributes(attributes, realm, first, max);
            }
        }, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(final String attrName, final String attrValue, RealmModel realm) {
        return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(UserQueryProvider provider, int first, int max) {
                return provider.searchForUserByUserAttribute(attrName, attrValue, realm);
            }
        }, realm,0, Integer.MAX_VALUE - 1);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        Set<FederatedIdentityModel> set = new HashSet<>();
        if (StorageId.isLocalStorage(user)) {
            set.addAll(localStorage().getFederatedIdentities(user, realm));
        }
        set.addAll(getFederatedStorage().getFederatedIdentities(user, realm));
        return set;
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        if (user == null) throw new IllegalStateException("Federated user no longer valid");
        if (StorageId.isLocalStorage(user)) {
            FederatedIdentityModel model = localStorage().getFederatedIdentity(user, socialProvider, realm);
            if (model != null) return model;
        }
        return getFederatedStorage().getFederatedIdentity(user, socialProvider, realm);
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        // not federation-aware for now
        List<UserUpdateProvider> storageProviders = getStorageProviders(realm, UserUpdateProvider.class);
        LinkedList<UserUpdateProvider> providers = new LinkedList<>();
        providers.add(localStorage());
        providers.addAll(storageProviders);
        for (UserUpdateProvider provider : providers) {
            provider.grantToAllUsers(realm, role);
        }
    }

    @Override
    public List<UserModel> getGroupMembers(final RealmModel realm, final GroupModel group, int firstResult, int maxResults) {
        return query(new PaginatedQuery() {
            @Override
            public List<UserModel> query(UserQueryProvider provider, int first, int max) {
                return provider.getGroupMembers(realm, group, first, max);
            }
        }, realm, firstResult, maxResults);
    }


    @Override
    public void preRemove(RealmModel realm) {
        localStorage().preRemove(realm);
        getFederatedStorage().preRemove(realm);
        for (StorageProvider provider : getStorageProviders(realm, StorageProvider.class)) {
            provider.preRemove(realm);
        }
    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel model) {
        getFederatedStorage().preRemove(realm, model);
        localStorage().preRemove(realm, model);
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        localStorage().preRemove(realm, group);
        getFederatedStorage().preRemove(realm, group);
        for (StorageProvider provider : getStorageProviders(realm, StorageProvider.class)) {
            provider.preRemove(realm, group);
        }
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        localStorage().preRemove(realm, role);
        getFederatedStorage().preRemove(realm, role);
        for (StorageProvider provider : getStorageProviders(realm, StorageProvider.class)) {
            provider.preRemove(realm, role);
        }
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        localStorage().preRemove(realm, client);
        getFederatedStorage().preRemove(realm, client);

    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        localStorage().preRemove(protocolMapper);
        getFederatedStorage().preRemove(protocolMapper);
    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        if (StorageId.isLocalStorage(user)) {
            return localStorage().validCredentials(session, realm, user, input);
        }
        // make sure we hit the cache here!
        List<UserCredentialValueModel> userCreds = user.getCredentialsDirectly();

        LinkedList<UserCredentialModel> toValidate = new LinkedList<>();
        toValidate.addAll(input);
        Iterator<UserCredentialModel> it = toValidate.iterator();
        boolean failedStoredCredential = false;
        // we allow for multiple credentials of same type, i.e. multiple OTP devices
        while (it.hasNext()) {
            UserCredentialModel cred = it.next();
            boolean credValidated = false;
            for (UserCredentialValueModel userCred : userCreds) {
                if (!userCred.getType().equals(cred.getType())) continue;
                if (CredentialValidation.validCredential(session, realm, user, cred)) {
                    credValidated = true;
                    break;
                } else {
                    failedStoredCredential = true;
                }
            }
            if (credValidated) {
                it.remove();
            } else if (failedStoredCredential) {
                return false;
            }
        }

        if (toValidate.isEmpty()) return true;

        StorageProvider provider = getStorageProvider(realm, StorageId.resolveProviderId(user));
        if (!(provider instanceof UserCredentialValidatorProvider)) {
            return false;
        }
        return ((UserCredentialValidatorProvider)provider).validCredentials(session, realm, user, toValidate);
    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, UserCredentialModel... input) {
        return validCredentials(session, realm, user, Arrays.asList(input));
    }

    @Override
    public CredentialValidationOutput validCredentials(KeycloakSession session, RealmModel realm, UserCredentialModel... input) {
        List<UserCredentialAuthenticationProvider> providers = getStorageProviders(realm, UserCredentialAuthenticationProvider.class);
        if (providers.isEmpty()) return CredentialValidationOutput.failed();

        CredentialValidationOutput result = null;
        for (UserCredentialModel cred : input) {
            UserCredentialAuthenticationProvider providerSupportingCreds = null;

            // Find first provider, which supports required credential type
            for (UserCredentialAuthenticationProvider provider : providers) {
                if (provider.getSupportedCredentialAuthenticationTypes().contains(cred.getType())) {
                    providerSupportingCreds = provider;
                    break;
                }
            }

            if (providerSupportingCreds == null) {
                logger.warn("Don't have provider supporting credentials of type " + cred.getType());
                return CredentialValidationOutput.failed();
            }

            logger.debug("Found provider [" + providerSupportingCreds + "] supporting credentials of type " + cred.getType());
            CredentialValidationOutput currentResult = providerSupportingCreds.validCredential(session, realm, cred);
            result = (result == null) ? currentResult : result.merge(currentResult);
        }

        // For now, validCredentials(realm, input) is not supported for local userProviders
        return (result != null) ? result : CredentialValidationOutput.failed();
    }

    @Override
    public void preRemove(RealmModel realm, StorageProviderModel link) {

    }

    @Override
    public void close() {
    }
}

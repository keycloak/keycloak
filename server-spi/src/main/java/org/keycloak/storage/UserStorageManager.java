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
import org.keycloak.common.util.reflections.Types;
import org.keycloak.component.ComponentModel;
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
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.Arrays;
import java.util.Collections;
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

    public UserStorageManager(KeycloakSession session) {
        this.session = session;
    }

    protected UserProvider localStorage() {
        return session.userLocalStorage();
    }

    protected List<UserStorageProviderModel> getStorageProviders(RealmModel realm) {
        return realm.getUserStorageProviders();
    }

    protected <T> T getFirstStorageProvider(RealmModel realm, Class<T> type) {
        for (UserStorageProviderModel model : getStorageProviders(realm)) {
            UserStorageProviderFactory factory = (UserStorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, model.getProviderId());

            if (Types.supports(type, factory, UserStorageProviderFactory.class)) {
                return type.cast(getStorageProviderInstance(model, factory));
            }
        }
        return null;
    }

    private UserStorageProvider getStorageProviderInstance(UserStorageProviderModel model, UserStorageProviderFactory factory) {
        UserStorageProvider instance = (UserStorageProvider)session.getAttribute(model.getId());
        if (instance != null) return instance;
        instance = factory.create(session, model);
        session.enlistForClose(instance);
        session.setAttribute(model.getId(), instance);
        return instance;
    }


    protected <T> List<T> getStorageProviders(RealmModel realm, Class<T> type) {
        List<T> list = new LinkedList<>();
        for (UserStorageProviderModel model : getStorageProviders(realm)) {
            UserStorageProviderFactory factory = (UserStorageProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, model.getProviderId());
            if (Types.supports(type, factory, UserStorageProviderFactory.class)) {
                list.add(type.cast(getStorageProviderInstance(model, factory)));
            }


        }
        return list;
    }


    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        return localStorage().addUser(realm, id, username.toLowerCase(), addDefaultRoles, addDefaultRequiredActions);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserRegistrationProvider registry = getFirstStorageProvider(realm, UserRegistrationProvider.class);
        if (registry != null) {
            return registry.addUser(realm, username);
        }
        return localStorage().addUser(realm, username.toLowerCase());
    }

    public UserStorageProvider getStorageProvider(RealmModel realm, String componentId) {
        ComponentModel model = realm.getComponent(componentId);
        if (model == null) return null;
        UserStorageProviderFactory factory = (UserStorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, model.getProviderId());
        if (factory == null) {
            throw new ModelException("Could not find UserStorageProviderFactory for: " + model.getProviderId());
        }
        return getStorageProviderInstance(new UserStorageProviderModel(model), factory);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        getFederatedStorage().preRemove(realm, user);
        StorageId storageId = new StorageId(user.getId());
        if (storageId.getProviderId() == null) {
            return localStorage().removeUser(realm, user);
        }
        UserRegistrationProvider registry = (UserRegistrationProvider)getStorageProvider(realm, storageId.getProviderId());
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

    @FunctionalInterface
    interface PaginatedQuery {
        List<UserModel> query(Object provider, int first, int max);
    }

    protected List<UserModel> query(PaginatedQuery pagedQuery, RealmModel realm, int firstResult, int maxResults) {
        if (maxResults == 0) return Collections.EMPTY_LIST;



        List<UserQueryProvider> storageProviders = getStorageProviders(realm, UserQueryProvider.class);
        // we can skip rest of method if there are no storage providers
        if (storageProviders.isEmpty()) {
            return pagedQuery.query(localStorage(), firstResult, maxResults);
        }
        LinkedList<Object> providers = new LinkedList<>();
        List<UserModel> results = new LinkedList<UserModel>();
        providers.add(localStorage());
        providers.addAll(storageProviders);
        providers.add(getFederatedStorage());

        int leftToRead = maxResults;
        int leftToFirstResult = firstResult;

        Iterator<Object> it = providers.iterator();
        while (it.hasNext() && leftToRead != 0) {
            Object provider = it.next();
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
        return query((provider, first, max) -> {
            if (provider instanceof UserProvider) { // it is local storage
                return ((UserProvider) provider).getUsers(realm, first, max, includeServiceAccounts);
            } else if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).getUsers(realm, first, max);

            }
            return Collections.EMPTY_LIST;
        }
        , realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return query((provider, first, max) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).searchForUser(search, realm, first, max);

            }
            return Collections.EMPTY_LIST;
        }, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return searchForUser(attributes, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        return query((provider, first, max) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).searchForUser(attributes, realm, first, max);

            }
            return Collections.EMPTY_LIST;
        }
        , realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        List<UserModel> results = query((provider, first, max) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).searchForUserByUserAttribute(attrName, attrValue, realm);

            } else if (provider instanceof UserFederatedStorageProvider) {
                List<String> ids = ((UserFederatedStorageProvider)provider).getUsersByUserAttribute(realm, attrName, attrValue);
                List<UserModel> rs = new LinkedList<>();
                for (String id : ids) {
                    UserModel user = getUserById(id, realm);
                    if (user != null) rs.add(user);
                }
                return rs;

            }
            return Collections.EMPTY_LIST;
        }, realm,0, Integer.MAX_VALUE - 1);
        return results;
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
        List<UserRegistrationProvider> storageProviders = getStorageProviders(realm, UserRegistrationProvider.class);
        LinkedList<UserRegistrationProvider> providers = new LinkedList<>();
        providers.add(localStorage());
        providers.addAll(storageProviders);
        for (UserRegistrationProvider provider : providers) {
            provider.grantToAllUsers(realm, role);
        }
    }

    @Override
    public List<UserModel> getGroupMembers(final RealmModel realm, final GroupModel group, int firstResult, int maxResults) {
        List<UserModel> results = query((provider, first, max) -> {
            if (provider instanceof UserQueryProvider) {
                return ((UserQueryProvider)provider).getGroupMembers(realm, group, first, max);

            } else if (provider instanceof UserFederatedStorageProvider) {
                List<String> ids = ((UserFederatedStorageProvider)provider).getMembership(realm, group, first, max);
                List<UserModel> rs = new LinkedList<UserModel>();
                for (String id : ids) {
                    UserModel user = getUserById(id, realm);
                    if (user != null) rs.add(user);
                }
                return rs;

            }
            return Collections.EMPTY_LIST;
        }, realm, firstResult, maxResults);
        return results;
    }


    @Override
    public void preRemove(RealmModel realm) {
        localStorage().preRemove(realm);
        getFederatedStorage().preRemove(realm);
        for (UserStorageProvider provider : getStorageProviders(realm, UserStorageProvider.class)) {
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
        for (UserStorageProvider provider : getStorageProviders(realm, UserStorageProvider.class)) {
            provider.preRemove(realm, group);
        }
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        localStorage().preRemove(realm, role);
        getFederatedStorage().preRemove(realm, role);
        for (UserStorageProvider provider : getStorageProviders(realm, UserStorageProvider.class)) {
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

        UserStorageProvider provider = getStorageProvider(realm, StorageId.resolveProviderId(user));
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
    public void preRemove(RealmModel realm, ComponentModel component) {

    }

    @Override
    public void close() {
    }
}

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

package org.keycloak.storage.changeset;

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
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserUpdateProvider;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.StorageProvider;
import org.keycloak.storage.StorageProviderFactory;
import org.keycloak.storage.StorageProviderModel;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.HashMap;
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
        if (localStorage == null) {
            localStorage = session.getProvider(UserProvider.class);
        }
        return localStorage;
    }

    protected List<StorageProviderModel> getStorageProviders(RealmModel realm) {
        return null;
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
        UserDataStore store = getFirstStorageProvider(realm, UserDataStore.class);
        if (store != null) {
            UserData data = new UserData();
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

    public StorageProvider getStorageProvider(String providerId) {
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        StorageId storageId = new StorageId(user.getId());
        if (storageId.getProviderId() == null) {
            return localStorage().removeUser(realm, user);
        }
        UserUpdateProvider registry = (UserUpdateProvider)getStorageProvider(storageId.getProviderId());
        if (registry == null) {
            throw new ModelException("Could not resolve StorageProvider: " + storageId.getProviderId());
        }
        return registry.removeUser(realm, user);

    }

    public UserFederatedStorageProvider getFederatedStorage() {
        return null;
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        getFederatedStorage().addFederatedIdentity(realm, user, socialLink);
    }

    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        getFederatedStorage().updateFederatedIdentity(realm, federatedUser, federatedIdentityModel);
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        return getFederatedStorage().removeFederatedIdentity(realm, user, socialProvider);
    }

    @Override
    public void addConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        getFederatedStorage().addConsent(realm, user, consent);

    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, UserModel user, String clientInternalId) {
        return getFederatedStorage().getConsentByClient(realm, user, clientInternalId);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, UserModel user) {
        return getFederatedStorage().getConsents(realm, user);
    }

    @Override
    public void updateConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        getFederatedStorage().updateConsent(realm, user, consent);

    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, UserModel user, String clientInternalId) {
        return getFederatedStorage().revokeConsentForClient(realm, user, clientInternalId);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        return null;
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        return null;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return null;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return null;
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        return null;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
        return null;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts) {
        return null;
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {

    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel link) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {

    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {

    }

    @Override
    public void preRemove(RealmModel realm, StorageProviderModel link) {

    }

    @Override
    public CredentialValidationOutput validCredentials(KeycloakSession session, RealmModel realm, UserCredentialModel... input) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        return false;
    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, UserCredentialModel... input) {
        return false;
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return null;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return 0;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return null;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return null;
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return null;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return null;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return null;
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        return null;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return null;
    }
}

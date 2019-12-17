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
package org.keycloak.testsuite.federation;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserPropertyFileStorage implements UserLookupProvider, UserStorageProvider, UserQueryProvider, CredentialInputValidator {

    protected Properties userPasswords;
    protected ComponentModel model;
    protected KeycloakSession session;
    protected boolean federatedStorageEnabled;

    public UserPropertyFileStorage(KeycloakSession session, ComponentModel model, Properties userPasswords) {
        this.session = session;
        this.model = model;
        this.userPasswords = userPasswords;
        this.federatedStorageEnabled = model.getConfig().containsKey("federatedStorage") && Boolean.valueOf(model.getConfig().getFirst("federatedStorage")).booleanValue();
    }


    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        final String username = storageId.getExternalId();
        if (!userPasswords.containsKey(username)) return null;

        return createUser(realm, username);
    }

    private UserModel createUser(RealmModel realm, String username) {
        if (federatedStorageEnabled) {
            return new AbstractUserAdapterFederatedStorage(session, realm,  model) {
                @Override
                public String getUsername() {
                    return username;
                }

                @Override
                public void setUsername(String username) {
                    throw new RuntimeException("Unsupported");
                }
            };
        } else {
            return new AbstractUserAdapter(session, realm, model) {
                @Override
                public String getUsername() {
                    return username;
                }
            };
        }
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        if (!userPasswords.containsKey(username)) return null;

        return createUser(realm, username);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE) && userPasswords.get(user.getUsername()) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return false;
        if (input.getType().equals(PasswordCredentialModel.TYPE)) {
            String pw = (String)userPasswords.get(user.getUsername());
            return pw != null && pw.equals(input.getChallengeResponse());
        } else {
            return false;
        }
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return userPasswords.size();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        List<UserModel> users = new LinkedList<>();
        for (Object username : userPasswords.keySet()) {
            users.add(createUser(realm, (String)username));
        }
        return users;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        if (maxResults == 0) return Collections.EMPTY_LIST;
        List<UserModel> users = new LinkedList<>();
        int count = 0;
        for (Object un : userPasswords.keySet()) {
            if (count++ < firstResult) continue;
            String username = (String)un;
            users.add(createUser(realm, username));
            if (users.size() + 1 > maxResults) break;
        }
        return users;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        if (maxResults == 0) return Collections.EMPTY_LIST;
        List<UserModel> users = new LinkedList<>();
        int count = 0;
        for (Object un : userPasswords.keySet()) {
            String username = (String)un;
            if (username.contains(search)) {
                if (count++ < firstResult) {
                    continue;
                }
                users.add(createUser(realm, username));
                if (users.size() + 1 > maxResults) break;
            }
        }
        return users;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        String username = attributes.get(UserModel.USERNAME);
        if (username == null) return Collections.EMPTY_LIST;
        return searchForUser(username, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return getUsers(realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void close() {

    }
}

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
package org.keycloak.testsuite.federation.storage;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.StorageProvider;
import org.keycloak.storage.StorageProviderModel;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.user.UserCredentialValidatorProvider;
import org.keycloak.storage.user.UserLookupProvider;

import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserPropertyFileStorage implements UserLookupProvider, StorageProvider, UserCredentialValidatorProvider {

    protected Properties userPasswords;
    protected StorageProviderModel model;
    protected KeycloakSession session;
    protected boolean federatedStorageEnabled;

    public UserPropertyFileStorage(KeycloakSession session, StorageProviderModel model, Properties userPasswords) {
        this.session = session;
        this.model = model;
        this.userPasswords = userPasswords;
        this.federatedStorageEnabled = model.getConfig().containsKey("USER_FEDERATED_STORAGE");
    }


    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        final String username = storageId.getStorageId();
        if (!userPasswords.containsKey(username)) return null;

        return createUser(realm, username);
    }

    private UserModel createUser(final RealmModel realm, final String username) {
        return new AbstractUserAdapter(session, realm, model) {
            @Override
            public String getUsername() {
                return username;
            }
        };
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
    public void preRemove(RealmModel realm, StorageProviderModel model) {

    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        for (UserCredentialModel cred : input) {
            if (!cred.getType().equals(UserCredentialModel.PASSWORD)) return false;
            String password = (String)userPasswords.get(user.getUsername());
            if (password == null) return false;
            if (!password.equals(cred.getValue())) return false;
        }
        return true;
    }

    @Override
    public void close() {

    }
}

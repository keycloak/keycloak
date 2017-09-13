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

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserMapStorage implements UserLookupProvider, UserStorageProvider, UserRegistrationProvider, CredentialInputUpdater, CredentialInputValidator {

    protected Map<String, String> userPasswords;
    protected ComponentModel model;
    protected KeycloakSession session;

    public static final AtomicInteger allocations = new AtomicInteger(0);
    public static final AtomicInteger closings = new AtomicInteger(0);

    public UserMapStorage(KeycloakSession session, ComponentModel model, Map<String, String> userPasswords) {
        this.session = session;
        this.model = model;
        this.userPasswords = userPasswords;
        allocations.incrementAndGet();
    }


    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        final String username = storageId.getExternalId();
        if (!userPasswords.containsKey(username)) return null;

        return createUser(realm, username);
    }

    private UserModel createUser(RealmModel realm, String username) {
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
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return false;
        if (input.getType().equals(UserCredentialModel.PASSWORD)) {
            userPasswords.put(user.getUsername(), ((UserCredentialModel)input).getValue());
            return true;

        } else {
            return false;
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return false;
        if (input.getType().equals(UserCredentialModel.PASSWORD)) {
            String pw = userPasswords.get(user.getUsername());
            return pw != null && pw.equals( ((UserCredentialModel)input).getValue());
        } else {
            return false;
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
    public UserModel addUser(RealmModel realm, String username) {
        userPasswords.put(username, "");
        return createUser(realm, username);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return userPasswords.remove(user.getUsername()) != null;
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
    public void close() {
        closings.incrementAndGet();

    }
}

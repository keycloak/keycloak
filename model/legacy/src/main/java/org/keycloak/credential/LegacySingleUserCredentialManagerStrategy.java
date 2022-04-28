/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.credential;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.AbstractStorageManager;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.LegacyStoreManagers;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;

import java.util.List;
import java.util.stream.Stream;

/**
 * Strategy for {@link LegacySingleUserCredentialManager} to handle classic local storage including federation.
 *
 * @author Alexander Schwartz
 */
public class LegacySingleUserCredentialManagerStrategy extends AbstractStorageManager<UserStorageProvider, UserStorageProviderModel> implements SingleUserCredentialManagerStrategy {

    private final UserModel user;
    private final RealmModel realm;

    public LegacySingleUserCredentialManagerStrategy(KeycloakSession session, RealmModel realm, UserModel user) {
        super(session, UserStorageProviderFactory.class, UserStorageProvider.class, UserStorageProviderModel::new, "user");
        this.user = user;
        this.realm = realm;
    }

    @Override
    public void validateCredentials(List<CredentialInput> toValidate) {
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        return false;
    }

    @Override
    public void updateStoredCredential(CredentialModel cred) {
        getStoreForUser(user).updateCredential(realm, user, cred);
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        return getStoreForUser(user).createCredential(realm, user, cred);
    }

    @Override
    public Boolean removeStoredCredentialById(String id) {
        return getStoreForUser(user).removeStoredCredential(realm, user, id);
    }

    @Override
    public CredentialModel getStoredCredentialById(String id) {
        return getStoreForUser(user).getStoredCredentialById(realm, user, id);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream() {
        return getStoreForUser(user).getStoredCredentialsStream(realm, user);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
        return getStoreForUser(user).getStoredCredentialsByTypeStream(realm, user, type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
        return getStoreForUser(user).getStoredCredentialByNameAndType(realm, user, name, type);
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        return getStoreForUser(user).moveCredentialTo(realm, user, id, newPreviousCredentialId);
    }

    private UserCredentialStore getStoreForUser(UserModel user) {
        LegacyStoreManagers p = (LegacyStoreManagers) session.getProvider(DatastoreProvider.class);
        if (StorageId.isLocalStorage(user.getId())) {
            return (UserCredentialStore) p.userLocalStorage();
        } else {
            return (UserCredentialStore) p.userFederatedStorage();
        }
    }

}

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
package org.keycloak.credential;

import org.keycloak.common.util.reflections.Types;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.StorageId;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCredentialStoreManager
        implements UserCredentialManager.Streams, OnUserCache {

    private final KeycloakSession session;

    public UserCredentialStoreManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        user.getUserCredentialManager().updateStoredCredential(cred);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        return user.getUserCredentialManager().createStoredCredential(cred);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        return user.getUserCredentialManager().removeStoredCredentialById(id);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        return user.getUserCredentialManager().getStoredCredentialById(id);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public Stream<CredentialModel> getStoredCredentialsStream(RealmModel realm, UserModel user) {
        return user.getUserCredentialManager().getStoredCredentialsStream();
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(RealmModel realm, UserModel user, String type) {
        return user.getUserCredentialManager().getStoredCredentialsByTypeStream(type);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        return user.getUserCredentialManager().getStoredCredentialByNameAndType(name, type);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId){
        return user.getUserCredentialManager().moveStoredCredentialTo(id, newPreviousCredentialId);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput... inputs) {
        return isValid(realm, user, Arrays.asList(inputs));
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public CredentialModel createCredentialThroughProvider(RealmModel realm, UserModel user, CredentialModel model){
        return user.getUserCredentialManager().createCredentialThroughProvider(model);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public void updateCredentialLabel(RealmModel realm, UserModel user, String credentialId, String userLabel){
        user.getUserCredentialManager().updateCredentialLabel(credentialId, userLabel);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public boolean isValid(RealmModel realm, UserModel user, List<CredentialInput> inputs) {
        return user.getUserCredentialManager().isValid(inputs);
    }

    @Deprecated // Keep this up to and including Keycloak 19, then inline
    public static <T> Stream<T> getCredentialProviders(KeycloakSession session, Class<T> type) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(CredentialProvider.class)
                .filter(f -> Types.supports(type, f, CredentialProviderFactory.class))
                .map(f -> (T) session.getProvider(CredentialProvider.class, f.getId()));
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        return user.getUserCredentialManager().updateCredential(input);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        user.getUserCredentialManager().disableCredentialType(credentialType);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return user.getUserCredentialManager().getDisableableCredentialTypesStream();
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String type) {
        return user.getUserCredentialManager().isConfiguredFor(type);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public boolean isConfiguredLocally(RealmModel realm, UserModel user, String type) {
        // TODO: no longer used, can be removed
        return user.getUserCredentialManager().isConfiguredLocally(type);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public CredentialValidationOutput authenticate(KeycloakSession session, RealmModel realm, CredentialInput input) {
        // TODO: no longer used, can be removed
        return session.users().getUserByCredential(realm, input);
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, then remove it together with the OnUserCache class
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        getCredentialProviders(session, OnUserCache.class).forEach(validator -> validator.onCache(realm, user, delegate));
    }

    @Override
    @Deprecated // Keep this up to and including Keycloak 19, the use methods on user.getUserCredentialManager() instead
    public Stream<String> getConfiguredUserStorageCredentialTypesStream(RealmModel realm, UserModel user) {
        return user.getUserCredentialManager().getConfiguredUserStorageCredentialTypesStream(user);
    }

    @Override
    public void close() {

    }

}

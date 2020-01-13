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
import org.keycloak.models.cache.UserCache;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.UserStorageProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCredentialStoreManager implements UserCredentialManager, OnUserCache {
    protected KeycloakSession session;

    public UserCredentialStoreManager(KeycloakSession session) {
        this.session = session;
    }

    protected UserCredentialStore getStoreForUser(UserModel user) {
        if (StorageId.isLocalStorage(user)) {
            return (UserCredentialStore) session.userLocalStorage();
        } else {
            return (UserCredentialStore) session.userFederatedStorage();
        }
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        getStoreForUser(user).updateCredential(realm, user, cred);
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        return getStoreForUser(user).createCredential(realm, user, cred);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        boolean removalResult = getStoreForUser(user).removeStoredCredential(realm, user, id);
        session.userCache().evict(realm, user);
        return removalResult;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        return getStoreForUser(user).getStoredCredentialById(realm, user, id);
    }

    @Override
    public List<CredentialModel> getStoredCredentials(RealmModel realm, UserModel user) {
        return getStoreForUser(user).getStoredCredentials(realm, user);
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, UserModel user, String type) {
        return getStoreForUser(user).getStoredCredentialsByType(realm, user, type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        return getStoreForUser(user).getStoredCredentialByNameAndType(realm, user, name, type);
    }

    @Override
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId){
        return getStoreForUser(user).moveCredentialTo(realm, user, id, newPreviousCredentialId);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput... inputs) {
        return isValid(realm, user, Arrays.asList(inputs));
    }

    @Override
    public CredentialModel createCredentialThroughProvider(RealmModel realm, UserModel user, CredentialModel model){
        List <CredentialProvider> credentialProviders = session.getKeycloakSessionFactory().getProviderFactories(CredentialProvider.class)
                .stream()
                .map(f -> session.getProvider(CredentialProvider.class, f.getId()))
                .filter(provider -> provider.getType().equals(model.getType()))
                .collect(Collectors.toList());
        if (credentialProviders.isEmpty()) {
            return null;
        } else {
            return credentialProviders.get(0).createCredential(realm, user, credentialProviders.get(0).getCredentialFromModel(model));
        }
    }

    @Override
    public void updateCredentialLabel(RealmModel realm, UserModel user, String credentialId, String userLabel){
        CredentialModel credential = getStoredCredentialById(realm, user, credentialId);
        credential.setUserLabel(userLabel);
        getStoreForUser(user).updateCredential(realm, user, credential);
        UserCache userCache = session.userCache();
        if (userCache != null) {
            userCache.evict(realm, user);
        }
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, List<CredentialInput> inputs) {

        List<CredentialInput> toValidate = new LinkedList<>();
        toValidate.addAll(inputs);
        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider instanceof CredentialInputValidator) {
                if (!UserStorageManager.isStorageProviderEnabled(realm, providerId)) return false;
                Iterator<CredentialInput> it = toValidate.iterator();
                while (it.hasNext()) {
                    CredentialInput input = it.next();
                    CredentialInputValidator validator = (CredentialInputValidator) provider;
                    if (validator.supportsCredentialType(input.getType()) && validator.isValid(realm, user, input)) {
                        it.remove();
                    }
                }
            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider instanceof CredentialInputValidator) {
                    if (!UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) return false;
                    validate(realm, user, toValidate, ((CredentialInputValidator)provider));
                }
            }
        }

        if (toValidate.isEmpty()) return true;

        List<CredentialInputValidator> credentialProviders = getCredentialProviders(session, realm, CredentialInputValidator.class);
        for (CredentialInputValidator validator : credentialProviders) {
            validate(realm, user, toValidate, validator);

        }
        return toValidate.isEmpty();
    }

    private void validate(RealmModel realm, UserModel user, List<CredentialInput> toValidate, CredentialInputValidator validator) {
        Iterator<CredentialInput> it = toValidate.iterator();
        while (it.hasNext()) {
            CredentialInput input = it.next();
            if (validator.supportsCredentialType(input.getType()) && validator.isValid(realm, user, input)) {
                it.remove();
            }
        }
    }

    public static <T> List<T> getCredentialProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        List<T> list = new LinkedList<T>();
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(CredentialProvider.class)) {
            if (!Types.supports(type, f, CredentialProviderFactory.class)) continue;
            list.add((T) session.getProvider(CredentialProvider.class, f.getId()));
        }
        return list;

    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider instanceof CredentialInputUpdater) {
                if (!UserStorageManager.isStorageProviderEnabled(realm, providerId)) return;
                CredentialInputUpdater updater = (CredentialInputUpdater) provider;
                if (updater.supportsCredentialType(input.getType())) {
                    if (updater.updateCredential(realm, user, input)) return;
                }

            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider instanceof CredentialInputUpdater) {
                    if (!UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) return;
                    if (((CredentialInputUpdater) provider).updateCredential(realm, user, input)) return;
                }
            }
        }

        List<CredentialInputUpdater> credentialProviders = getCredentialProviders(session, realm, CredentialInputUpdater.class);
        for (CredentialInputUpdater updater : credentialProviders) {
            if (!updater.supportsCredentialType(input.getType())) continue;
            if (updater.updateCredential(realm, user, input)) return;

        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider instanceof CredentialInputUpdater) {
                if (!UserStorageManager.isStorageProviderEnabled(realm, providerId)) return;
                CredentialInputUpdater updater = (CredentialInputUpdater) provider;
                if (updater.supportsCredentialType(credentialType)) {
                    updater.disableCredentialType(realm, user, credentialType);
                }

            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider instanceof CredentialInputUpdater) {
                    if (!UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) return;
                    ((CredentialInputUpdater) provider).disableCredentialType(realm, user, credentialType);
                }
            }

        }

        List<CredentialInputUpdater> credentialProviders = getCredentialProviders(session, realm, CredentialInputUpdater.class);
        for (CredentialInputUpdater updater : credentialProviders) {
            if (!updater.supportsCredentialType(credentialType)) continue;
            updater.disableCredentialType(realm, user, credentialType);

        }


    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        Set<String> types = new HashSet<>();
        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider instanceof CredentialInputUpdater) {
                if (!UserStorageManager.isStorageProviderEnabled(realm, providerId)) return Collections.EMPTY_SET;
                CredentialInputUpdater updater = (CredentialInputUpdater) provider;
                types.addAll(updater.getDisableableCredentialTypes(realm, user));
            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider instanceof CredentialInputUpdater) {
                    if (!UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) return Collections.EMPTY_SET;
                    types.addAll(((CredentialInputUpdater) provider).getDisableableCredentialTypes(realm, user));
                }
            }

        }

        List<CredentialInputUpdater> credentialProviders = getCredentialProviders(session, realm, CredentialInputUpdater.class);
        for (CredentialInputUpdater updater : credentialProviders) {
            types.addAll(updater.getDisableableCredentialTypes(realm, user));
        }
        return types;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String type) {
        UserStorageCredentialConfigured userStorageConfigured = isConfiguredThroughUserStorage(realm, user, type);

        // Check if we can rely just on userStorage to decide if credential is configured for the user or not
        switch (userStorageConfigured) {
            case CONFIGURED: return true;
            case USER_STORAGE_DISABLED: return false;
        }

        // Check locally as a fallback
        return isConfiguredLocally(realm, user, type);
    }


    private enum UserStorageCredentialConfigured {
        CONFIGURED,
        USER_STORAGE_DISABLED,
        NOT_CONFIGURED
    }


    private UserStorageCredentialConfigured isConfiguredThroughUserStorage(RealmModel realm, UserModel user, String type) {
        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider instanceof CredentialInputValidator) {
                if (!UserStorageManager.isStorageProviderEnabled(realm, providerId)) return UserStorageCredentialConfigured.USER_STORAGE_DISABLED;
                CredentialInputValidator validator = (CredentialInputValidator) provider;
                if (validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                    return UserStorageCredentialConfigured.CONFIGURED;
                }
            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider instanceof CredentialInputValidator) {
                    if (!UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) return UserStorageCredentialConfigured.USER_STORAGE_DISABLED;
                    if (((CredentialInputValidator) provider).isConfiguredFor(realm, user, type)) return UserStorageCredentialConfigured.CONFIGURED;
                }
            }

        }

        return UserStorageCredentialConfigured.NOT_CONFIGURED;
    }

    @Override
    public boolean isConfiguredLocally(RealmModel realm, UserModel user, String type) {
        List<CredentialInputValidator> credentialProviders = getCredentialProviders(session, realm, CredentialInputValidator.class);
        for (CredentialInputValidator validator : credentialProviders) {
            if (validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                return true;
            }

        }
        return false;
    }

    @Override
    public CredentialValidationOutput authenticate(KeycloakSession session, RealmModel realm, CredentialInput input) {
        List<CredentialAuthentication> list = UserStorageManager.getEnabledStorageProviders(session, realm, CredentialAuthentication.class);
        for (CredentialAuthentication auth : list) {
            if (auth.supportsCredentialAuthenticationFor(input.getType())) {
                CredentialValidationOutput output = auth.authenticate(realm, input);
                if (output != null) return output;
            }
        }

        list = getCredentialProviders(session, realm, CredentialAuthentication.class);
        for (CredentialAuthentication auth : list) {
            if (auth.supportsCredentialAuthenticationFor(input.getType())) {
                CredentialValidationOutput output = auth.authenticate(realm, input);
                if (output != null) return output;
            }
        }

        return null;
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        List<OnUserCache> credentialProviders = getCredentialProviders(session, realm, OnUserCache.class);
        for (OnUserCache validator : credentialProviders) {
            validator.onCache(realm, user, delegate);
        }
    }

    @Override
    public List<String> getConfiguredUserStorageCredentialTypes(RealmModel realm, UserModel user) {
        List<CredentialProvider> credentialProviders = getCredentialProviders(session, realm, CredentialProvider.class);

        return credentialProviders.stream().map(CredentialProvider::getType)
                .filter(credentialType -> UserStorageCredentialConfigured.CONFIGURED == isConfiguredThroughUserStorage(realm, user, credentialType))
                .collect(Collectors.toList());
    }

    @Override
    public void close() {

    }
}

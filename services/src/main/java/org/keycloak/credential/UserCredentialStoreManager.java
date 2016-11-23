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
import org.keycloak.provider.ProviderFactory;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.UserStorageProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
            return (UserCredentialStore)session.userLocalStorage();
        } else {
            return (UserCredentialStore)session.userFederatedStorage();
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
        return getStoreForUser(user).removeStoredCredential(realm, user, id);
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
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput... inputs) {
        return isValid(realm, user, Arrays.asList(inputs));
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, List<CredentialInput> inputs) {

        List<CredentialInput> toValidate = new LinkedList<>();
        toValidate.addAll(inputs);
        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider instanceof CredentialInputValidator) {
                Iterator<CredentialInput> it = toValidate.iterator();
                while (it.hasNext()) {
                    CredentialInput input = it.next();
                    CredentialInputValidator validator = (CredentialInputValidator)provider;
                    if (validator.supportsCredentialType(input.getType()) && validator.isValid(realm, user, input)) {
                        it.remove();
                    }
                }
            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider != null && provider instanceof CredentialInputValidator) {
                    validate(realm, user, toValidate, ((CredentialInputValidator)provider));
                }
            }
        }

        if (toValidate.isEmpty()) return true;

        List<CredentialInputValidator> credentialProviders = getCredentialProviders(realm, CredentialInputValidator.class);
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

    protected <T> List<T> getCredentialProviders(RealmModel realm, Class<T> type) {
        List<T> list = new LinkedList<T>();
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(CredentialProvider.class)) {
            if (!Types.supports(CredentialInputUpdater.class, f, CredentialProviderFactory.class)) continue;
            list.add((T)session.getProvider(CredentialProvider.class, f.getId()));
        }
        return list;

    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider instanceof CredentialInputUpdater) {
                CredentialInputUpdater updater = (CredentialInputUpdater)provider;
                if (updater.supportsCredentialType(input.getType())) {
                    if (updater.updateCredential(realm, user, input)) return;
                }
            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider != null && provider instanceof CredentialInputUpdater) {
                    if (((CredentialInputUpdater)provider).updateCredential(realm, user, input)) return;
                }
            }
        }

        List<CredentialInputUpdater> credentialProviders = getCredentialProviders(realm, CredentialInputUpdater.class);
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
                CredentialInputUpdater updater = (CredentialInputUpdater)provider;
                if (updater.supportsCredentialType(credentialType)) {
                    updater.disableCredentialType(realm, user, credentialType);
                }
            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider != null && provider instanceof CredentialInputUpdater) {
                    ((CredentialInputUpdater)provider).disableCredentialType(realm, user, credentialType);
                }
            }

        }

        List<CredentialInputUpdater> credentialProviders = getCredentialProviders(realm, CredentialInputUpdater.class);
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
                CredentialInputUpdater updater = (CredentialInputUpdater)provider;
                types.addAll(updater.getDisableableCredentialTypes(realm, user));
            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider != null && provider instanceof CredentialInputUpdater) {
                    types.addAll(((CredentialInputUpdater)provider).getDisableableCredentialTypes(realm, user));
                }
            }

        }

        List<CredentialInputUpdater> credentialProviders = getCredentialProviders(realm, CredentialInputUpdater.class);
        for (CredentialInputUpdater updater : credentialProviders) {
            types.addAll(updater.getDisableableCredentialTypes(realm, user));
        }
        return types;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String type) {
        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider instanceof CredentialInputValidator) {
                CredentialInputValidator validator = (CredentialInputValidator)provider;
                if (validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                    return true;
                }
            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
                if (provider != null && provider instanceof CredentialInputValidator) {
                    if (((CredentialInputValidator)provider).isConfiguredFor(realm, user, type)) return true;
                }
            }

        }

        return isConfiguredLocally(realm, user, type);
    }

    @Override
    public boolean isConfiguredLocally(RealmModel realm, UserModel user, String type) {
        List<CredentialInputValidator> credentialProviders = getCredentialProviders(realm, CredentialInputValidator.class);
        for (CredentialInputValidator validator : credentialProviders) {
            if (validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                return true;
            }

        }
        return false;
    }

    @Override
    public CredentialValidationOutput authenticate(KeycloakSession session, RealmModel realm, CredentialInput input) {
        List<CredentialAuthentication> list = UserStorageManager.getStorageProviders(session, realm, CredentialAuthentication.class);
        for (CredentialAuthentication auth : list) {
            if (auth.supportsCredentialAuthenticationFor(input.getType())) {
                CredentialValidationOutput output = auth.authenticate(realm, input);
                if (output != null) return output;
            }
        }

        list = getCredentialProviders(realm, CredentialAuthentication.class);
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
        List<OnUserCache> credentialProviders = getCredentialProviders(realm, OnUserCache.class);
        for (OnUserCache validator : credentialProviders) {
            validator.onCache(realm, user, delegate);
        }
    }

    @Override
    public void close() {

    }
}

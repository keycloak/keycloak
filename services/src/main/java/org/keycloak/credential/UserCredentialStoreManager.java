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
import org.keycloak.component.ComponentModel;
import org.keycloak.component.PrioritizedComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.UserStorageProvider;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
        }

        if (toValidate.isEmpty()) return true;

        List<ComponentModel> components = getCredentialProviderComponents(realm);
        for (ComponentModel component : components) {
            CredentialProviderFactory factory = (CredentialProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CredentialProvider.class, component.getProviderId());
            if (!Types.supports(CredentialInputValidator.class, factory, CredentialProviderFactory.class)) continue;
            Iterator<CredentialInput> it = toValidate.iterator();
            while (it.hasNext()) {
                CredentialInput input = it.next();
                CredentialInputValidator validator = (CredentialInputValidator)session.getAttribute(component.getId());
                if (validator == null) {
                    validator = (CredentialInputValidator)factory.create(session, component);
                    session.setAttribute(component.getId(), validator);
                }
                if (validator.supportsCredentialType(input.getType()) && validator.isValid(realm, user, input)) {
                    it.remove();
                }
            }
        }

        return toValidate.isEmpty();
    }

    protected List<ComponentModel> getCredentialProviderComponents(RealmModel realm) {
        List<ComponentModel> components = realm.getComponents(realm.getId(), CredentialProvider.class.getName());
        if (components.isEmpty()) return components;
        List<ComponentModel> copy = new LinkedList<>();
        copy.addAll(components);
        Collections.sort(copy, PrioritizedComponentModel.comparator);
        return copy;
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
        }

        List<ComponentModel> components = getCredentialProviderComponents(realm);
        for (ComponentModel component : components) {
            CredentialProviderFactory factory = (CredentialProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CredentialProvider.class, component.getProviderId());
            if (!Types.supports(CredentialInputUpdater.class, factory, CredentialProviderFactory.class)) continue;
            CredentialInputUpdater updater = (CredentialInputUpdater)session.getAttribute(component.getId());
            if (updater == null) {
                updater = (CredentialInputUpdater)factory.create(session, component);
                session.setAttribute(component.getId(), updater);
            }
            if (!updater.supportsCredentialType(input.getType())) continue;
            if (updater.updateCredential(realm, user, input)) return;
        }

    }
    @Override
    public void disableCredential(RealmModel realm, UserModel user, String credentialType) {
        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider instanceof CredentialInputUpdater) {
                CredentialInputUpdater updater = (CredentialInputUpdater)provider;
                if (updater.supportsCredentialType(credentialType)) {
                    updater.disableCredentialType(realm, user, credentialType);
                }
            }
        }

        List<ComponentModel> components = getCredentialProviderComponents(realm);
        for (ComponentModel component : components) {
            CredentialProviderFactory factory = (CredentialProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CredentialProvider.class, component.getProviderId());
            if (!Types.supports(CredentialInputUpdater.class, factory, CredentialProviderFactory.class)) continue;
            CredentialInputUpdater updater = (CredentialInputUpdater)session.getAttribute(component.getId());
            if (updater == null) {
                updater = (CredentialInputUpdater)factory.create(session, component);
                session.setAttribute(component.getId(), updater);
            }
            if (!updater.supportsCredentialType(credentialType)) continue;
            updater.disableCredentialType(realm, user, credentialType);
        }

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
        }

        List<ComponentModel> components = getCredentialProviderComponents(realm);
        for (ComponentModel component : components) {
            CredentialProviderFactory factory = (CredentialProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CredentialProvider.class, component.getProviderId());
            if (!Types.supports(CredentialInputUpdater.class, factory, CredentialProviderFactory.class)) continue;
            CredentialInputValidator validator = (CredentialInputValidator)session.getAttribute(component.getId());
            if (validator == null) {
                validator = (CredentialInputValidator)factory.create(session, component);
                session.setAttribute(component.getId(), validator);
            }
            if (validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                return true;
            }
        }
        return false;

    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user) {
        List<ComponentModel> components = getCredentialProviderComponents(realm);
        for (ComponentModel component : components) {
            CredentialProviderFactory factory = (CredentialProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CredentialProvider.class, component.getProviderId());
            if (!Types.supports(OnUserCache.class, factory, CredentialProviderFactory.class)) continue;
            OnUserCache validator = (OnUserCache)session.getAttribute(component.getId());
            if (validator == null) {
                validator = (OnUserCache)factory.create(session, component);
                session.setAttribute(component.getId(), validator);
            }
            validator.onCache(realm, user);
        }

    }

    @Override
    public void close() {

    }
}

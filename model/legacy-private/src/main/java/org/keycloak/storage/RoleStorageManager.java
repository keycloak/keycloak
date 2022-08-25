/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.util.Objects;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LegacyRealmModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.storage.role.RoleLookupProvider;
import org.keycloak.storage.role.RoleStorageProvider;
import org.keycloak.storage.role.RoleStorageProviderFactory;
import org.keycloak.storage.role.RoleStorageProviderModel;
import org.keycloak.utils.ServicesUtils;

public class RoleStorageManager implements RoleProvider {
    private static final Logger logger = Logger.getLogger(RoleStorageManager.class);

    protected KeycloakSession session;

    private final long roleStorageProviderTimeout;

    public RoleStorageManager(KeycloakSession session, long roleStorageProviderTimeout) {
        this.session = session;
        this.roleStorageProviderTimeout = roleStorageProviderTimeout;
    }

    private RoleProvider localStorage() {
        return session.getProvider(RoleProvider.class);
    }

    public static boolean isStorageProviderEnabled(RealmModel realm, String providerId) {
        RoleStorageProviderModel model = getStorageProviderModel(realm, providerId);
        return model.isEnabled();
    }

    public static RoleStorageProviderModel getStorageProviderModel(RealmModel realm, String componentId) {
        ComponentModel model = realm.getComponent(componentId);
        if (model == null) return null;
        return new RoleStorageProviderModel(model);
    }

    public static RoleStorageProvider getStorageProvider(KeycloakSession session, RealmModel realm, String componentId) {
        ComponentModel model = realm.getComponent(componentId);
        if (model == null) return null;
        RoleStorageProviderModel storageModel = new RoleStorageProviderModel(model);
        RoleStorageProviderFactory factory = (RoleStorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(RoleStorageProvider.class, model.getProviderId());
        if (factory == null) {
            throw new ModelException("Could not find RoletStorageProviderFactory for: " + model.getProviderId());
        }
        return getStorageProviderInstance(session, storageModel, factory);
    }

    public static <T> Stream<RoleStorageProviderModel> getStorageProviders(RealmModel realm, KeycloakSession session, Class<T> type) {
        return ((LegacyRealmModel) realm).getRoleStorageProvidersStream()
                .filter(model -> {
                    RoleStorageProviderFactory factory = getRoleStorageProviderFactory(model, session);
                    if (factory == null) {
                        logger.warnv("Configured RoleStorageProvider {0} of provider id {1} does not exist in realm {2}",
                                model.getName(), model.getProviderId(), realm.getName());
                        return false;
                    } else {
                        return Types.supports(type, factory, RoleStorageProviderFactory.class);
                    }
                });
    }

    private static RoleStorageProviderFactory getRoleStorageProviderFactory(RoleStorageProviderModel model, KeycloakSession session) {
        return (RoleStorageProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(RoleStorageProvider.class, model.getProviderId());
    }

    public static RoleStorageProvider getStorageProviderInstance(KeycloakSession session, RoleStorageProviderModel model, RoleStorageProviderFactory factory) {
        RoleStorageProvider instance = (RoleStorageProvider)session.getAttribute(model.getId());
        if (instance != null) return instance;
        instance = factory.create(session, model);
        if (instance == null) {
            throw new IllegalStateException("RoleStorageProvideFactory (of type " + factory.getClass().getName() + ") produced a null instance");
        }
        session.enlistForClose(instance);
        session.setAttribute(model.getId(), instance);
        return instance;
    }


    public static <T> Stream<T> getStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        return getStorageProviders(realm, session, type)
                .map(model -> type.cast(getStorageProviderInstance(session, model, getRoleStorageProviderFactory(model, session))));
    }


    public static <T> Stream<T> getEnabledStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        return getStorageProviders(realm, session, type)
                .filter(RoleStorageProviderModel::isEnabled)
                .map(model -> type.cast(getStorageProviderInstance(session, model, getRoleStorageProviderFactory(model, session))));
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String name) {
        return localStorage().addRealmRole(realm, name);
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        return localStorage().addRealmRole(realm, id, name);
    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        RoleModel realmRole = localStorage().getRealmRole(realm, name);
        if (realmRole != null) return realmRole;
        return getEnabledStorageProviders(session, realm, RoleLookupProvider.class)
                .map(provider -> provider.getRealmRole(realm, name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        if (storageId.getProviderId() == null) {
            return localStorage().getRoleById(realm, id);
        }
        RoleLookupProvider provider = (RoleLookupProvider)getStorageProvider(session, realm, storageId.getProviderId());
        if (provider == null) return null;
        if (! isStorageProviderEnabled(realm, storageId.getProviderId())) return null;
        return provider.getRoleById(realm, id);
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) {
        return localStorage().getRealmRolesStream(realm, first, max);
    }

    @Override
    public Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        return localStorage().getRolesStream(realm, ids, search, first, max);
    }

    /**
     * Obtaining roles from an external role storage is time-bounded. In case the external role storage
     * isn't available at least roles from a local storage are returned. For this purpose
     * the <code>org.keycloak.services.DefaultKeycloakSessionFactory#getRoleStorageProviderTimeout()</code> property is used.
     * Default value is 3000 milliseconds and it's configurable.
     * See <code>org.keycloak.services.DefaultKeycloakSessionFactory</code> for details.
     */
    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        Stream<RoleModel> local = localStorage().searchForRolesStream(realm, search, first, max);
        Stream<RoleModel> ext = getEnabledStorageProviders(session, realm, RoleLookupProvider.class)
                .flatMap(ServicesUtils.timeBound(session,
                        roleStorageProviderTimeout,
                        p -> ((RoleLookupProvider) p).searchForRolesStream(realm, search, first, max)));

        return Stream.concat(local, ext);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        if (!StorageId.isLocalStorage(role.getId())) {
            throw new RuntimeException("Federated roles do not support this operation");
        }
        return localStorage().removeRole(role);
    }

    @Override
    public void removeRoles(RealmModel realm) {
        localStorage().removeRoles(realm);
    }

    @Override
    public void removeRoles(ClientModel client) {
        localStorage().removeRoles(client);
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String name) {
        return localStorage().addClientRole(client, name);
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        return localStorage().addClientRole(client, id, name);
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        RoleModel clientRole = localStorage().getClientRole(client, name);
        if (clientRole != null) return clientRole;
        return getEnabledStorageProviders(session, client.getRealm(), RoleLookupProvider.class)
                .map(provider -> provider.getClientRole(client, name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client) {
        return localStorage().getClientRolesStream(client);
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) {
        return localStorage().getClientRolesStream(client, first, max);
    }

    /**
     * Obtaining roles from an external role storage is time-bounded. In case the external role storage
     * isn't available at least roles from a local storage are returned. For this purpose
     * the <code>org.keycloak.services.DefaultKeycloakSessionFactory#getRoleStorageProviderTimeout()} property is used.
     * Default value is 3000 milliseconds and it's configurable.
     * See <code>org.keycloak.services.DefaultKeycloakSessionFactory</code> for details.
     */
    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        Stream<RoleModel> local = localStorage().searchForClientRolesStream(client, search, first, max);
        Stream<RoleModel> ext = getEnabledStorageProviders(session, client.getRealm(), RoleLookupProvider.class)
                .flatMap(ServicesUtils.timeBound(session,
                        roleStorageProviderTimeout,
                        p -> ((RoleLookupProvider) p).searchForClientRolesStream(client, search, first, max)));

        return Stream.concat(local, ext);
    }

    @Override
    public void close() {
    }
}

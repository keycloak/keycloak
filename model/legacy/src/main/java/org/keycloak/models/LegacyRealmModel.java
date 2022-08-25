/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import org.keycloak.models.RealmModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.client.ClientStorageProviderModel;
import org.keycloak.storage.role.RoleStorageProvider;
import org.keycloak.storage.role.RoleStorageProviderModel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Alexander Schwartz
 */
public interface LegacyRealmModel extends RealmModel {
    /**
     * @deprecated Use {@link #getClientStorageProvidersStream() getClientStorageProvidersStream} instead.
     */
    @Deprecated
    default List<ClientStorageProviderModel> getClientStorageProviders() {
        return getClientStorageProvidersStream().collect(Collectors.toList());
    }

    /**
     * Returns sorted {@link ClientStorageProviderModel ClientStorageProviderModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @return Sorted stream of {@link ClientStorageProviderModel}. Never returns {@code null}.
     */
    default Stream<ClientStorageProviderModel> getClientStorageProvidersStream() {
        return getComponentsStream(getId(), ClientStorageProvider.class.getName())
                .map(ClientStorageProviderModel::new)
                .sorted(ClientStorageProviderModel.comparator);
    }

    /**
     * @deprecated Use {@link #getRoleStorageProvidersStream() getRoleStorageProvidersStream} instead.
     */
    @Deprecated
    default List<RoleStorageProviderModel> getRoleStorageProviders() {
        return getRoleStorageProvidersStream().collect(Collectors.toList());
    }

    /**
     * Returns sorted {@link RoleStorageProviderModel RoleStorageProviderModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @return Sorted stream of {@link RoleStorageProviderModel}. Never returns {@code null}.
     */
    default Stream<RoleStorageProviderModel> getRoleStorageProvidersStream() {
        return getComponentsStream(getId(), RoleStorageProvider.class.getName())
                .map(RoleStorageProviderModel::new)
                .sorted(RoleStorageProviderModel.comparator);
    }

    /**
     * @deprecated Use {@link #getUserStorageProvidersStream() getUserStorageProvidersStream} instead.
     */
    @Deprecated
    default List<UserStorageProviderModel> getUserStorageProviders() {
        return getUserStorageProvidersStream().collect(Collectors.toList());
    }

    /**
     * Returns sorted {@link UserStorageProviderModel UserStorageProviderModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @return Sorted stream of {@link UserStorageProviderModel}. Never returns {@code null}.
     */
    default Stream<UserStorageProviderModel> getUserStorageProvidersStream() {
        return getComponentsStream(getId(), UserStorageProvider.class.getName())
                .map(UserStorageProviderModel::new)
                .sorted(UserStorageProviderModel.comparator);
    }

}

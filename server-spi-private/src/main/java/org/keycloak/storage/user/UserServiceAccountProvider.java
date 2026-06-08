/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.user;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * This is an optional capability interface that is intended to be implemented by any
 * {@code UserStorageProvider} that supports storing service account users.
 *
 * <p>By default, Keycloak stores service account users in local (JPA) storage. Providers
 * that implement this interface can intercept service account creation and lookup,
 * allowing service account users to be stored in an external store.
 *
 * <p>All storage providers that implement this interface will be looped through in priority order.
 * If a method returns null, the next provider is tried. If no provider handles the request,
 * local storage is used as a fallback.
 *
 * <p>Providers implementing this interface should also implement {@link UserRegistrationProvider}
 * so that service account users can be removed when the owning client is deleted or has service
 * accounts disabled.
 */
public interface UserServiceAccountProvider {

    /**
     * Creates a service account user in this storage provider.
     *
     * <p>If this method returns null, then the next storage provider's method will be called.
     * If no storage providers handle the creation, the user will be created in local storage.
     *
     * <p>The returned {@link UserModel} must support {@link UserModel#setEnabled(boolean)} and
     * {@link UserModel#setServiceAccountClientLink(String)}, and must durably persist the client
     * link so that it survives across sessions and is returned by
     * {@link UserModel#getServiceAccountClientLink()} on subsequent lookups.
     *
     * @param realm a reference to the realm
     * @param username the username for the service account (prefixed with "service-account-")
     * @return a model of the created user, or null if this provider does not handle the request
     */
    UserModel addServiceAccountUser(RealmModel realm, String username);

    /**
     * Returns a UserModel representing the service account of the given client.
     *
     * <p>If this method returns null, then the next storage provider will be queried.
     *
     * @param client the client model whose service account user is being looked up
     * @return the service account user model, or null if not managed by this provider
     */
    UserModel getServiceAccount(ClientModel client);
}

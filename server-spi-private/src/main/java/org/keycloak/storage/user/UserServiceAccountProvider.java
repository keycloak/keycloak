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
 * Optional private capability for {@link org.keycloak.storage.UserStorageProvider}
 * implementations that want to own service account users. This extends
 * {@link UserRegistrationProvider} so provider-owned service accounts can be
 * removed through the normal user removal path.
 */
public interface UserServiceAccountProvider extends UserRegistrationProvider {

    /**
     * Creates a service account user in this storage provider.
     *
     * If this provider does not want to handle the given service account user,
     * it should return {@code null} so the next provider or local storage can be
     * tried.
     *
     * @param realm a reference to the realm
     * @param username service account username
     * @return a model of the created service account user, or {@code null}
     */
    UserModel addServiceAccountUser(RealmModel realm, String username);

    /**
     * Returns the service account user for the client if this provider owns it.
     *
     * @param client the client model
     * @return service account user, or {@code null}
     */
    UserModel getServiceAccount(ClientModel client);
}

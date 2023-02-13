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

package org.keycloak.authorization.store;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

/**
 * A {@link ResourceServerStore} is responsible to manage the persistence of {@link ResourceServer} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ResourceServerStore {

    /**
     * <p>Creates a {@link ResourceServer} instance backed by this persistent storage implementation.
     *
     * @param client the client acting as a resource server. Cannot be {@code null}.
     *
     * @return an instance backed by the underlying storage implementation
     */
    ResourceServer create(ClientModel client);

    /**
     * Removes a {@link ResourceServer} instance, with the given client from the persistent storage.
     *
     * @param client the client acting as a resource server. Cannot be {@code null}.
     */
    void delete(ClientModel client);

    /**
     * Returns a {@link ResourceServer} instance based on its identifier.
     *
     *
     * @param realm the realm. Cannot be {@code null}.
     * @param id the identifier of an existing resource server instance
     *
     * @return the resource server instance with the given identifier or null if no instance was found
     */
    ResourceServer findById(RealmModel realm, String id);

    /**
     * Returns a {@link ResourceServer} instance based on a client.
     *
     * @param client the client acting as a resource server. Cannot be {@code null}.
     *
     * @return the resource server instance or null if no instance was found
     */
    ResourceServer findByClient(ClientModel client);
}

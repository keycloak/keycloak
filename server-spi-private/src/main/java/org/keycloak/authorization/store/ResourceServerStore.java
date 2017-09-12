/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.store;

import org.keycloak.authorization.model.ResourceServer;

/**
 * A {@link ResourceServerStore} is responsible to manage the persistence of {@link ResourceServer} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ResourceServerStore {

    /**
     * <p>Creates a {@link ResourceServer} instance backed by this persistent storage implementation.
     *
     * @param clientId the client id acting as a resource server
     *
     * @return an instance backed by the underlying storage implementation
     */
    ResourceServer create(String clientId);

    /**
     * Removes a {@link ResourceServer} instance, with the given {@code id} from the persistent storage.
     *
     * @param id the identifier of an existing resource server instance
     */
    void delete(String id);

    /**
     * Returns a {@link ResourceServer} instance based on its identifier.
     *
     * @param id the identifier of an existing resource server instance
     *
     * @return the resource server instance with the given identifier or null if no instance was found
     */
    ResourceServer findById(String id);
}

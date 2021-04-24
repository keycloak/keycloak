/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.provider.Provider;

/**
 * Provides cache for session mapping for SAML artifacts.
 *
 * For now, it is separate provider as it's a bit different use-case than {@link ActionTokenStoreProvider}, however it may reuse some components (eg. same infinispan cache)
 *
 * @author mhajas
 */
public interface SamlArtifactSessionMappingStoreProvider extends Provider {

    /**
     * Stores the given data and guarantees that data should be available in the store for at least the time specified by {@param lifespanSeconds} parameter
     *
     * @param artifact
     * @param lifespanSeconds
     * @param clientSessionModel
     */
    void put(String artifact, int lifespanSeconds, AuthenticatedClientSessionModel clientSessionModel);


    /**
     * This method returns session mapping associated with the given {@param artifact}
     *
     * @param artifact
     * @return session mapping corresponding to given artifact or {@code null} if it does not exist.
     */
    SamlArtifactSessionMappingModel get(String artifact);

    /**
     * Removes data for the given {@param artifact}  from the store
     * 
     * @param artifact
     */
    void remove(String artifact);
}

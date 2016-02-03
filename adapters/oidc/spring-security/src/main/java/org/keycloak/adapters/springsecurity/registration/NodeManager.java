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

package org.keycloak.adapters.springsecurity.registration;

import org.keycloak.adapters.KeycloakDeployment;

/**
 * Manages registration of application nodes.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public interface NodeManager {

    /**
     * Registers the given deployment with the Keycloak server.
     *
     * @param deployment the deployment to register (required)
     */
    void register(KeycloakDeployment deployment);

    /**
     * Unregisters the give deployment from the Keycloak server
     * .
     * @param deployment the deployment to unregister (required)
     */
    void unregister(KeycloakDeployment deployment);

}

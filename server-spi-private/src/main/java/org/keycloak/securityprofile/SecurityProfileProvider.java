/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.securityprofile;

import java.util.List;

import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;

/**
 * The security profile provider is a default security configuration that enforces a
 * minimum level of security in the keycloak environment. For the moment the class
 * is just used for client policies but it can be extended for password policies
 * or any other security configuration in the future.
 *
 * @author rmartinc
 */
public interface SecurityProfileProvider extends Provider {

    /**
     * Name of the security profile.
     * @return The name
     */
    String getName();

    /**
     * List of default client profiles that the security profile contains.
     * @return The list of client profiles defined
     */
    List<ClientProfileRepresentation> getDefaultClientProfiles();

    /**
     * List of default client policies defined in the security profile.
     * @return The list of client policies defined
     */
    List<ClientPolicyRepresentation> getDefaultClientPolicies();
}

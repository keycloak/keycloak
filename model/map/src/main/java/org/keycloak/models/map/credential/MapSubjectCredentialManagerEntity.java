/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.credential;

import org.keycloak.credential.CredentialInput;

import java.util.List;
import java.util.stream.Stream;

/**
 * Interface for credential management in entities in the map storage.
 *
 * @author Alexander Schwartz
 */
public interface MapSubjectCredentialManagerEntity {

    /**
     * Validate the credentials of a user.
     * Will remove all inputs from the list that have been successfully validated, all remaining entries
     * weren't validated. An empty list signals to the caller that authentication has completed successfully.
     *
     * @param inputs Credential inputs as provided by a user
     */
    void validateCredentials(List<CredentialInput> inputs);

    /**
     * Update the credentials for a user with the input provided by the user for this store.
     * @param input new credentials as provided by the user
     * @return true if the credential has been updated successfully, false otherwise. False might indicate that the
     * credential type isn't supported of the new credentials aren't valid.
     */
    boolean updateCredential(CredentialInput input);

    /**
     * Check if the entity is configured for the given credential type.
     * @param type credential type
     */
    boolean isConfiguredFor(String type);

    /**
     * List the credential types that can be disabled for this user.
     * @return Stream of credential types
     */
    Stream<String> getDisableableCredentialTypesStream();
}

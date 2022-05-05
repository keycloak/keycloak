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

package org.keycloak.models;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public interface SingleUserCredentialManager {

    /**
     * Validates list of credentials.
     */
    boolean isValid(List<CredentialInput> inputs);

    default boolean isValid(CredentialInput... inputs) {
        return isValid(Arrays.asList(inputs));
    }

    /**
     * Updates a credentials of the user.
     */
    boolean updateCredential(CredentialInput input);

    void updateStoredCredential(CredentialModel cred);

    CredentialModel createStoredCredential(CredentialModel cred);

    boolean removeStoredCredentialById(String id);

    CredentialModel getStoredCredentialById(String id);

    Stream<CredentialModel> getStoredCredentialsStream();

    Stream<CredentialModel> getStoredCredentialsByTypeStream(String type);

    CredentialModel getStoredCredentialByNameAndType(String name, String type);

    boolean moveStoredCredentialTo(String id, String newPreviousCredentialId);

    void updateCredentialLabel(String credentialId, String userLabel);

    void disableCredentialType(String credentialType);

    Stream<String> getDisableableCredentialTypesStream();

    boolean isConfiguredFor(String type);

    // TODO: not needed for new store? -> no, will be removed without replacement
    @Deprecated
    boolean isConfiguredLocally(String type);

    Stream<String> getConfiguredUserStorageCredentialTypesStream();

    CredentialModel createCredentialThroughProvider(CredentialModel model);
}

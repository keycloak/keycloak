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
package org.keycloak.storage.federated;

import java.util.stream.Stream;

import org.keycloak.models.UserVerifiableCredentialModel;

public interface UserVerifiableCredentialFederatedStorage {

    /**
     * Adds a verifiable credential for a federated user.
     *
     * @param userId the federated user ID
     * @param credentialModel the credential to add
     * @return the added credential with generated fields populated
     */
    UserVerifiableCredentialModel addVerifiableCredential(String userId, UserVerifiableCredentialModel credentialModel);

    /**
     * Updates a verifiable credential for a federated user.
     *
     * @param userId the federated user ID
     * @param clientScopeId the client scope ID
     * @return the updated credential
     */
    UserVerifiableCredentialModel updateVerifiableCredential(String userId, String clientScopeId);

    /**
     * Removes a verifiable credential for a federated user.
     *
     * @param userId the federated user ID
     * @param clientScopeId the client scope ID to remove
     * @return true if removed, false if not found
     */
    boolean removeVerifiableCredential(String userId, String clientScopeId);

    /**
     * Gets all verifiable credentials for a federated user.
     *
     * @param userId the federated user ID
     * @return stream of verifiable credentials
     */
    Stream<UserVerifiableCredentialModel> getVerifiableCredentialsByUser(String userId);

    /**
     * Gets a verifiable credential for a federated user and client scope.
     *
     * @param userId the federated user ID
     * @param clientScopeId the client scope ID
     * @return the verifiable credential model, or {@code null} if not found
     */
    UserVerifiableCredentialModel getVerifiableCredentialByClientScope(String userId, String clientScopeId);

    /**
     * Gets a verifiable credential by its ID.
     *
     * @param id the ID of the verifiable credential
     * @return the verifiable credential model, or {@code null} if not found
     */
    UserVerifiableCredentialModel getVerifiableCredentialById(String id);

}

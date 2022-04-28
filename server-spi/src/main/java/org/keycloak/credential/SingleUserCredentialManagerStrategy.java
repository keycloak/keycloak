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

package org.keycloak.credential;

import java.util.List;
import java.util.stream.Stream;

/**
 * Use this to implement extendable strategies for the {@link org.keycloak.models.SingleUserCredentialManager}.
 */
public interface SingleUserCredentialManagerStrategy {

    /**
     * Validate the credentials passed as a list. The implementation should remove all credentials that validate
     * successfully from the list. An empty list signals to the caller that authentication has completed successfully.
     */
    void validateCredentials(List<CredentialInput> toValidate);

    /**
     * Update the credential.
     * @return true is the credential was update, false otherwise
     */
    boolean updateCredential(CredentialInput input);

    void updateStoredCredential(CredentialModel cred);

    CredentialModel createStoredCredential(CredentialModel cred) ;

    Boolean removeStoredCredentialById(String id);

    CredentialModel getStoredCredentialById(String id);

    Stream<CredentialModel> getStoredCredentialsStream();

    Stream<CredentialModel> getStoredCredentialsByTypeStream(String type);

    CredentialModel getStoredCredentialByNameAndType(String name, String type);

    boolean moveStoredCredentialTo(String id, String newPreviousCredentialId);
}

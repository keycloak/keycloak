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

package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.provider.Provider;

/**
 * Interface for signing verifiable credentials in the OID4VCI protocol.
 * Implementations must produce a signed credential representation based on the provided
 * credential body and configuration.
 *
 * @param <T> the type of the signed credential representation (e.g., String for JWT, Map for LD-Proof)
 */
public interface CredentialSigner<T> extends Provider {

    /**
     * Default implementation for closing resources. Implementations should override
     * if they hold resources that need to be released.
     */
    @Override
    default void close() {
        // No resources to close by default
    }

    /**
     * Signs a verifiable credential using the provided credential body and configuration.
     *
     * @param credentialBody        the partially built credential representation to be signed
     * @param credentialBuildConfig additional configurations for building and signing the credential
     * @return a signed credential representation
     * @throws CredentialSignerException if signing fails due to invalid inputs, configuration errors,
     *                                   or cryptographic issues
     * @throws IllegalArgumentException if credentialBody or credentialBuildConfig is null
     */
    T signCredential(CredentialBody credentialBody, CredentialBuildConfig credentialBuildConfig)
            throws CredentialSignerException;
}

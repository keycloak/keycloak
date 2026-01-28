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

package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.Provider;

public interface CredentialBuilder extends Provider {

    @Override
    default void close() {
    }

    /**
     * Returns the credential format supported by the builder.
     */
    String getSupportedFormat();

    /**
     * Builds a verifiable credential of a specific format from the basis of
     * an internal representation of the credential.
     *
     * <p>
     * The credential is built incompletely, intended that it would be signed externally.
     * </p>
     *
     * @param verifiableCredential  an internal representation of the credential
     * @param credentialBuildConfig additional configurations for building the credential
     * @return the built verifiable credential of the specific format, ready to be signed
     */
    CredentialBody buildCredentialBody(
            VerifiableCredential verifiableCredential,
            CredentialBuildConfig credentialBuildConfig
    ) throws CredentialBuilderException;
}

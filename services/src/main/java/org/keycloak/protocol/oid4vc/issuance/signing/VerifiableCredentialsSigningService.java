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

import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.CredentialConfigId;
import org.keycloak.protocol.oid4vc.model.VerifiableCredentialType;
import org.keycloak.provider.Provider;

/**
 * Interface to be used for signing verifiable credentials.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public interface VerifiableCredentialsSigningService<T> extends Provider {
    /**
     * Takes a verifiable credential and signs it according to the implementation.
     * Depending on the type of the SigningService, it will return a signed representation of the credential
     *
     * @param vcIssuanceContext verifiable credential to sign and context info
     * @return a signed representation
     */
    T signCredential(VCIssuanceContext vcIssuanceContext) throws VCIssuerException;

    /**
     * Returns the identifier of this service instance, can be either the format alone,
     * or the combination between format, credential type and credential configuration id.
     * @return
     */
    String locator();

    String LOCATION_SEPARATOR = "::";

    /**
     * We are forcing a structure with 3 components. format::type::configId. We assume format is always set, as
     * implementation of this interface always know their format.
     *
     * @param format
     * @param credentialType
     * @param vcConfigId
     * @return
     */
    static String locator(String format, VerifiableCredentialType credentialType, CredentialConfigId vcConfigId){
        return (format == null ? "" : format) + LOCATION_SEPARATOR +
                (credentialType == null ? "" : credentialType.getValue()) + LOCATION_SEPARATOR +
                (vcConfigId == null ? "" : vcConfigId.getValue());
    }
}

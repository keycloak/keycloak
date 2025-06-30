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

import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
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
     * @param verifiableCredential the credential to sign
     * @return a signed representation
     */
    T signCredential(VerifiableCredential verifiableCredential);
}
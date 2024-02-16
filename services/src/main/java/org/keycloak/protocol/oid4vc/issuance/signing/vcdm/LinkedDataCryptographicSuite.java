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

package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

/**
 * Interface for all implementations of LD-Signature Suites
 * <p>
 * {@see https://w3c-ccg.github.io/ld-cryptosuite-registry/}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public interface LinkedDataCryptographicSuite {

    /**
     * Return the signature for the given credential as defined by the suite.
     *
     * @param verifiableCredential the credential to create a signature for
     * @return the signature
     */
    byte[] getSignature(VerifiableCredential verifiableCredential);

    /**
     * The proof type defined by the suite.
     *
     * @return the type
     */
    String getProofType();

}
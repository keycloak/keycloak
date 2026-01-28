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
package org.keycloak.crypto;

import java.security.cert.X509Certificate;
import java.util.List;

public interface SignatureSignerContext {

    String getKid();

    String getAlgorithm();

    String getHashAlgorithm();

    byte[] sign(byte[] data) throws SignatureException;

    /**
     * Returns the X.509 certificate chain associated with this signer, if available.
     * Returns null if certificates are not available (e.g., for MAC-based signers).
     * This allows access to certificates without requiring a separate KeyWrapper parameter.
     *
     * @return List of X.509 certificates, or null if not available
     */
    default List<X509Certificate> getCertificateChain() {
        return null;
    }

}

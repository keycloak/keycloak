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

package org.keycloak.jose.jws;

import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.jose.jws.crypto.SignatureProvider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public enum Algorithm {

    none(null, null),
    HS256(AlgorithmType.HMAC, null),
    HS384(AlgorithmType.HMAC, null),
    HS512(AlgorithmType.HMAC, null),
    RS256(AlgorithmType.RSA, new RSAProvider()),
    RS384(AlgorithmType.RSA, new RSAProvider()),
    RS512(AlgorithmType.RSA, new RSAProvider()),
    PS256(AlgorithmType.RSA, null),
    PS384(AlgorithmType.RSA, null),
    PS512(AlgorithmType.RSA, null),
    ES256(AlgorithmType.ECDSA, null),
    ES384(AlgorithmType.ECDSA, null),
    ES512(AlgorithmType.ECDSA, null)
    ;

    private AlgorithmType type;
    private SignatureProvider provider;

    Algorithm(AlgorithmType type, SignatureProvider provider) {
        this.type = type;
        this.provider = provider;
    }

    public AlgorithmType getType() {
        return type;
    }

    public SignatureProvider getProvider() {
        return provider;
    }
}

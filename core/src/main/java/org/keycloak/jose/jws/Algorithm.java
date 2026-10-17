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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
    ES512(AlgorithmType.ECDSA, null),
    EdDSA(AlgorithmType.EDDSA, null),
    Ed25519(AlgorithmType.EDDSA, null),
    Ed448(AlgorithmType.EDDSA, null),
    ML_DSA_44(AlgorithmType.ML_DSA, null, org.keycloak.crypto.Algorithm.ML_DSA_44),
    ML_DSA_65(AlgorithmType.ML_DSA, null, org.keycloak.crypto.Algorithm.ML_DSA_65),
    ML_DSA_87(AlgorithmType.ML_DSA, null, org.keycloak.crypto.Algorithm.ML_DSA_87)
    ;

    private AlgorithmType type;
    private SignatureProvider provider;
    private String value;

    Algorithm(AlgorithmType type, SignatureProvider provider) {
        this(type, provider, null);
    }

    Algorithm(AlgorithmType type, SignatureProvider provider, String value) {
        this.type = type;
        this.provider = provider;
        this.value = value;
    }

    public AlgorithmType getType() {
        return type;
    }

    public SignatureProvider getProvider() {
        return provider;
    }

    @JsonValue
    public String getValue() {
        return value == null ? name() : value;
    }

    @JsonCreator
    public static Algorithm fromValue(String value) {
        for (Algorithm algorithm : values()) {
            if (algorithm.getValue().equals(value)) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException("Unknown algorithm " + value);
    }
}

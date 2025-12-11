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

import org.keycloak.common.crypto.CryptoConstants;

public interface Algorithm {

    /* RSA signing algorithms  */
    String HS256 = "HS256";
    String HS384 = "HS384";
    String HS512 = "HS512";
    String RS256 = "RS256";
    String RS384 = "RS384";
    String RS512 = "RS512";
    String PS256 = "PS256";
    String PS384 = "PS384";
    String PS512 = "PS512";

    /* ECDSA signing algorithms  */
    String ES256 = "ES256";
    String ES384 = "ES384";
    String ES512 = "ES512";

    /* EdDSA signing algorithms  */
    String EdDSA = "EdDSA";
    /* EdDSA Curve */
    String Ed25519 = "Ed25519";
    String Ed448 = "Ed448";

    /* RSA Encryption Algorithms */
    String RSA1_5 = CryptoConstants.RSA1_5;
    String RSA_OAEP = CryptoConstants.RSA_OAEP;
    String RSA_OAEP_256 = CryptoConstants.RSA_OAEP_256;

    /* AES */
    String AES = "AES";

    String ECDH_ES = CryptoConstants.ECDH_ES;
    String ECDH_ES_A128KW = CryptoConstants.ECDH_ES_A128KW;
    String ECDH_ES_A192KW = CryptoConstants.ECDH_ES_A192KW;
    String ECDH_ES_A256KW = CryptoConstants.ECDH_ES_A256KW;

    String ML_DSA_44 =  "ML-DSA-44";
    String ML_DSA_65 =  "ML-DSA-65";
    String ML_DSA_87 =  "ML-DSA-87";
}

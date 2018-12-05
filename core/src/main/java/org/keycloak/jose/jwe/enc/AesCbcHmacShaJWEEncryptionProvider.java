/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jwe.enc;

import org.keycloak.jose.jwe.JWEConstants;

public class AesCbcHmacShaJWEEncryptionProvider extends AesCbcHmacShaEncryptionProvider {

    private final int expectedCEKLength;
    private final int expectedAesKeyLength;
    private final String hmacShaAlgorithm;
    private final int authenticationTagLength;


    public AesCbcHmacShaJWEEncryptionProvider(String jwaAlgorithmName) {
        if (JWEConstants.A128CBC_HS256.equals(jwaAlgorithmName)) {
            expectedCEKLength = 32;
            expectedAesKeyLength = 16;
            hmacShaAlgorithm = "HMACSHA256";
            authenticationTagLength = 16;
        } else {
            expectedCEKLength = 0;
            expectedAesKeyLength = 0;
            hmacShaAlgorithm = null;
            authenticationTagLength = 0;
        }
    }

    @Override
    public int getExpectedCEKLength() {
        return expectedCEKLength;
    }

    @Override
    protected int getExpectedAesKeyLength() {
        return expectedAesKeyLength;
    }

    @Override
    protected String getHmacShaAlgorithm() {
        return hmacShaAlgorithm;
    }

    @Override
    protected int getAuthenticationTagLength() {
        return authenticationTagLength;
    }

}

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

package org.keycloak.crypto;

public enum ECCurve {
    P256,
    P384,
    P521;

    /**
     * Convert standard EC curve names (and aliases) into this enum.
     */
    public static ECCurve fromStdCrv(String crv) {
        switch (crv) {
            case "P-256":
            case "secp256r1":
                return P256;
            case "P-384":
            case "secp384r1":
                return P384;
            case "P-521":
            case "secp521r1":
                return P521;
            default:
                throw new IllegalArgumentException("Unexpected EC curve: " + crv);
        }
    }
}

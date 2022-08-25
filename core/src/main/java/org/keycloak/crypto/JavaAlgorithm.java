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

public class JavaAlgorithm {

    public static final String RS256 = "SHA256withRSA";
    public static final String RS384 = "SHA384withRSA";
    public static final String RS512 = "SHA512withRSA";
    public static final String HS256 = "HMACSHA256";
    public static final String HS384 = "HMACSHA384";
    public static final String HS512 = "HMACSHA512";
    public static final String ES256 = "SHA256withECDSA";
    public static final String ES384 = "SHA384withECDSA";
    public static final String ES512 = "SHA512withECDSA";
    public static final String PS256 = "SHA256withRSAandMGF1";
    public static final String PS384 = "SHA384withRSAandMGF1";
    public static final String PS512 = "SHA512withRSAandMGF1";
    public static final String AES = "AES";

    public static final String SHA256 = "SHA-256";
    public static final String SHA384 = "SHA-384";
    public static final String SHA512 = "SHA-512";

    public static String getJavaAlgorithm(String algorithm) {
        switch (algorithm) {
            case Algorithm.RS256:
                return RS256;
            case Algorithm.RS384:
                return RS384;
            case Algorithm.RS512:
                return RS512;
            case Algorithm.HS256:
                return HS256;
            case Algorithm.HS384:
                return HS384;
            case Algorithm.HS512:
                return HS512;
            case Algorithm.ES256:
                return ES256;
            case Algorithm.ES384:
                return ES384;
            case Algorithm.ES512:
                return ES512;
            case Algorithm.PS256:
                return PS256;
            case Algorithm.PS384:
                return PS384;
            case Algorithm.PS512:
                return PS512;
            case Algorithm.AES:
                return AES;
            default:
                throw new IllegalArgumentException("Unknown algorithm " + algorithm);
        }
    }


    public static String getJavaAlgorithmForHash(String algorithm) {
        switch (algorithm) {
            case Algorithm.RS256:
                return SHA256;
            case Algorithm.RS384:
                return SHA384;
            case Algorithm.RS512:
                return SHA512;
            case Algorithm.HS256:
                return SHA256;
            case Algorithm.HS384:
                return SHA384;
            case Algorithm.HS512:
                return SHA512;
            case Algorithm.ES256:
                return SHA256;
            case Algorithm.ES384:
                return SHA384;
            case Algorithm.ES512:
                return SHA512;
            case Algorithm.PS256:
                return SHA256;
            case Algorithm.PS384:
                return SHA384;
            case Algorithm.PS512:
                return SHA512;
            case Algorithm.AES:
                return AES;
            default:
                throw new IllegalArgumentException("Unknown algorithm " + algorithm);
        }
    }

    public static boolean isRSAJavaAlgorithm(String algorithm) {
        return getJavaAlgorithm(algorithm).contains("RSA");
    }

    public static boolean isECJavaAlgorithm(String algorithm) {
        return getJavaAlgorithm(algorithm).contains("ECDSA");
    }

    public static boolean isHMACJavaAlgorithm(String algorithm) {
        return getJavaAlgorithm(algorithm).contains("HMAC");
    }
}

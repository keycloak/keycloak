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

    public static String getJavaAlgorithm(String algorithm) {
        switch (algorithm) {
            case Algorithm.RS256:
                return "SHA256withRSA";
            case Algorithm.RS384:
                return "SHA384withRSA";
            case Algorithm.RS512:
                return "SHA512withRSA";
            case Algorithm.HS256:
                return "HMACSHA256";
            case Algorithm.HS384:
                return "HMACSHA384";
            case Algorithm.HS512:
                return "HMACSHA512";
            case Algorithm.AES:
                return "AES";
            default:
                throw new IllegalArgumentException("Unkown algorithm " + algorithm);
        }
    }

}

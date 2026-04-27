/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.broker;

import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwe.JWEConstants;

public class KcOidcBrokerJWEEcdhEsA256KwP521A256GcmTest extends AbstractKcOidcBrokerJWEEcdhEsTest {
    public KcOidcBrokerJWEEcdhEsA256KwP521A256GcmTest() {
        super("P-521", JWEConstants.ECDH_ES_A256KW, JWEConstants.A256GCM, Algorithm.ES512);
    }
}

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

import org.keycloak.jose.jwe.JWEConstants;

/**
 * <p>Extension of the KcOidcBrokerJWETest test to use a different key algorithm (RSA1_5),
 * the default content encryption algorithm (A128CBC-HS256) and the default signature
 * algorithm (RS256 for id token and none/unsigned for user info).</p>
 *
 * @author rmartinc
 */
public class KcOidcBrokerJWEUserInfoJustEncryptedTest extends KcOidcBrokerJWETest {

    public KcOidcBrokerJWEUserInfoJustEncryptedTest() {
        super(JWEConstants.RSA_OAEP_256, null, null);
    }
}

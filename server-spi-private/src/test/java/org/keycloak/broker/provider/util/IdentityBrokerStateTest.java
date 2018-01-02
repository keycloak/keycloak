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

package org.keycloak.broker.provider.util;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class IdentityBrokerStateTest {

    @Test
    public void decodesAlphaNumericClientId() {
        IdentityBrokerState expected = IdentityBrokerState.decoded(randomString(), "abCD1234", randomString());
        IdentityBrokerState actual = IdentityBrokerState.encoded(expected.getEncoded());
        assertEquals(expected, actual);
    }

    @Test
    public void decodesDottedClientId() {
        IdentityBrokerState expected = IdentityBrokerState.decoded(randomString(), "ab.CD.1234", randomString());
        IdentityBrokerState actual = IdentityBrokerState.encoded(expected.getEncoded());
        assertEquals(expected, actual);
    }

    @Test
    public void decodesWithMissingTabId() {
        IdentityBrokerState expected = IdentityBrokerState.decoded(randomString(), "ab.CD.1234", "");
        IdentityBrokerState actual = IdentityBrokerState.encoded(expected.getEncoded());
        assertEquals(expected, actual);
    }

    public String randomString() {
        return UUID.randomUUID().toString();
    }
}
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

package org.keycloak.test;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ResponseTypeTest {

    @Test
    public void testResponseTypes() {
        assertFail(null);
        assertFail("");
        assertFail("foo");
        assertSuccess("code");
        assertSuccess("none");
        assertSuccess("id_token");
        assertSuccess("token");
        assertFail("refresh_token");
        assertSuccess("id_token token");
        assertSuccess("code token");
        assertSuccess("code id_token");
        assertSuccess("code id_token token");
        assertFail("code none");
        assertFail("code refresh_token");
    }

    @Test
    public void testMultipleResponseTypes() {
        OIDCResponseType responseType = OIDCResponseType.parse(Arrays.asList("code", "token"));
        Assert.assertTrue(responseType.hasResponseType("code"));
        Assert.assertFalse(responseType.hasResponseType("none"));
        Assert.assertTrue(responseType.isImplicitOrHybridFlow());
        Assert.assertFalse(responseType.isImplicitFlow());

        responseType = OIDCResponseType.parse(Collections.singletonList("code"));
        Assert.assertTrue(responseType.hasResponseType("code"));
        Assert.assertFalse(responseType.hasResponseType("none"));
        Assert.assertFalse(responseType.isImplicitOrHybridFlow());

        responseType = OIDCResponseType.parse(Arrays.asList("code", "none"));
        Assert.assertTrue(responseType.hasResponseType("code"));
        Assert.assertTrue(responseType.hasResponseType("none"));
        Assert.assertFalse(responseType.isImplicitOrHybridFlow());

        responseType = OIDCResponseType.parse(Arrays.asList("code", "code token"));
        Assert.assertTrue(responseType.hasResponseType("code"));
        Assert.assertFalse(responseType.hasResponseType("none"));
        Assert.assertTrue(responseType.hasResponseType("token"));
        Assert.assertFalse(responseType.hasResponseType("id_token"));
        Assert.assertTrue(responseType.isImplicitOrHybridFlow());
        Assert.assertFalse(responseType.isImplicitFlow());

        responseType = OIDCResponseType.parse(Arrays.asList("id_token", "id_token token"));
        Assert.assertFalse(responseType.hasResponseType("code"));
        Assert.assertTrue(responseType.isImplicitOrHybridFlow());
        Assert.assertTrue(responseType.isImplicitFlow());
    }

    private void assertSuccess(String responseType) {
        OIDCResponseType.parse(responseType);
    }

    private void assertFail(String responseType) {
        try {
            OIDCResponseType.parse(responseType);
            Assert.fail("Not expected to parse '" + responseType + "' with success");
        } catch (IllegalArgumentException expected) {
        }
    }
}

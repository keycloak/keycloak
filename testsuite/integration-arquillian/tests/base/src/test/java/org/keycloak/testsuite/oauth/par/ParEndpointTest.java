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

package org.keycloak.testsuite.oauth.par;

import java.util.HashMap;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedHashMap;

import org.keycloak.protocol.oidc.par.endpoints.ParEndpoint;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS_PARAM;

public class ParEndpointTest {

    @Test
    public void testFlattenDecodedFormParametersRetainAuthorizationDetails() {
        var decodedFormParameters = new MultivaluedHashMap<String, String>();
        String authorizationDetails = "[{\"type\": \"urn:openfinanceuae:account-access-consent:v1.0\",\"foo\":\"bar\"},{\"type\": \"urn:openfinanceuae:account-access-consent:v1.0\",\"gugu\":\"gaga\"}]";
        decodedFormParameters.put(AUTHORIZATION_DETAILS_PARAM, List.of(authorizationDetails));
        var params = new HashMap<String, String>();

        ParEndpoint.flattenDecodedFormParametersToParamsMap(decodedFormParameters, params);

        Assert.assertEquals(authorizationDetails, params.get(AUTHORIZATION_DETAILS_PARAM));
    }

    @Test
    public void testFlattenDecodedFormParametersMultipleValues() {
        var decodedFormParameters = new MultivaluedHashMap<String, String>();
        decodedFormParameters.put("param", List.of("paramValue1", "paramValue2"));
        var params = new HashMap<String, String>();

        ParEndpoint.flattenDecodedFormParametersToParamsMap(decodedFormParameters, params);

        Assert.assertEquals("paramValue1", params.get("param"));
    }

    @Test
    public void testFlattenDecodedFormParametersSingleValue() {
        var decodedFormParameters = new MultivaluedHashMap<String, String>();
        decodedFormParameters.put("param", List.of("single"));
        var params = new HashMap<String, String>();

        ParEndpoint.flattenDecodedFormParametersToParamsMap(decodedFormParameters, params);

        Assert.assertEquals("single", params.get("param"));
    }

    @Test
    public void testFlattenDecodedFormParametersNullValue() {
        var decodedFormParameters = new MultivaluedHashMap<String, String>();
        decodedFormParameters.add("param", null);
        var params = new HashMap<String, String>();

        ParEndpoint.flattenDecodedFormParametersToParamsMap(decodedFormParameters, params);

        Assert.assertNull(params.get("param"));
    }

    @Test
    public void testFlattenDecodedFormParametersValueWithNull() {
        var decodedFormParameters = new MultivaluedHashMap<String, String>();
        decodedFormParameters.add("param", "value");
        decodedFormParameters.add("param", null);
        var params = new HashMap<String, String>();

        ParEndpoint.flattenDecodedFormParametersToParamsMap(decodedFormParameters, params);

        Assert.assertEquals("value", params.get("param"));
    }

    @Test
    public void testFlattenDecodedFormParametersMultipleDistinctValues() {
        var decodedFormParameters = new MultivaluedHashMap<String, String>();
        decodedFormParameters.add("param", "valueAAA");
        decodedFormParameters.add("param", "valueBBB");
        var params = new HashMap<String, String>();

        ParEndpoint.flattenDecodedFormParametersToParamsMap(decodedFormParameters, params);

        Assert.assertEquals("valueAAA", params.get("param"));
    }
}

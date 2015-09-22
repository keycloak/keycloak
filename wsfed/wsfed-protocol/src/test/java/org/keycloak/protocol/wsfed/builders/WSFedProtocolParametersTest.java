/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.wsfed.builders;

import org.keycloak.wsfed.common.WSFedConstants;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;

import static org.junit.Assert.*;

/**
 * Created by dbarentine on 8/21/2015.
 */
public class WSFedProtocolParametersTest {
    @Test
    public void testFromParameters() {
        MultivaluedMap<String, String> requestParams = new MultivaluedMapImpl<>();
        requestParams.add(WSFedConstants.WSFED_ACTION, "action");
        requestParams.add(WSFedConstants.WSFED_REPLY, "reply");
        requestParams.add(WSFedConstants.WSFED_CONTEXT, "context");
        requestParams.add(WSFedConstants.WSFED_POLICY, "policy");
        requestParams.add(WSFedConstants.WSFED_CURRENT_TIME, "time");
        requestParams.add(WSFedConstants.WSFED_FEDERATION_ID, "fedid");
        requestParams.add(WSFedConstants.WSFED_ENCODING, "encoding");
        requestParams.add(WSFedConstants.WSFED_REALM, "realm");
        requestParams.add(WSFedConstants.WSFED_FRESHNESS, "freshness");
        requestParams.add(WSFedConstants.WSFED_AUTHENTICATION_LEVEL, "authlevel");
        requestParams.add(WSFedConstants.WSFED_TOKEN_REQUEST_TYPE, "trt");
        requestParams.add(WSFedConstants.WSFED_HOME_REALM, "homerealm");
        requestParams.add(WSFedConstants.WSFED_REQUEST_URL, "req");
        requestParams.add(WSFedConstants.WSFED_RESULT, "res");
        requestParams.add(WSFedConstants.WSFED_RESULT_URL, "resurl");

        WSFedProtocolParameters params = WSFedProtocolParameters.fromParameters(requestParams);
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_ACTION), params.getWsfed_action());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_REPLY), params.getWsfed_reply());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_CONTEXT), params.getWsfed_context());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_POLICY), params.getWsfed_policy());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_CURRENT_TIME), params.getWsfed_current_time());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_FEDERATION_ID), params.getWsfed_federation_id());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_ENCODING), params.getWsfed_encoding());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_REALM), params.getWsfed_realm());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_FRESHNESS), params.getWsfed_freshness());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_AUTHENTICATION_LEVEL), params.getWsfed_authentication_level());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_TOKEN_REQUEST_TYPE), params.getWsfed_token_request_type());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_HOME_REALM), params.getWsfed_home_realm());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_REQUEST_URL), params.getWsfed_request_url());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_RESULT), params.getWsfed_result());
        assertEquals(requestParams.getFirst(WSFedConstants.WSFED_RESULT_URL), params.getWsfed_result_url());
    }

    @Test
    public void testFromParametersNull() {
        MultivaluedMap<String, String> requestParams = new MultivaluedMapImpl<>();
        WSFedProtocolParameters params = WSFedProtocolParameters.fromParameters(requestParams);
        assertNull(params.getWsfed_action());
        assertNull(params.getWsfed_reply());
        assertNull(params.getWsfed_context());
        assertNull(params.getWsfed_policy());
        assertNull(params.getWsfed_current_time());
        assertNull(params.getWsfed_federation_id());
        assertNull(params.getWsfed_encoding());
        assertNull(params.getWsfed_realm());
        assertNull(params.getWsfed_freshness());
        assertNull(params.getWsfed_authentication_level());
        assertNull(params.getWsfed_token_request_type());
        assertNull(params.getWsfed_home_realm());
        assertNull(params.getWsfed_request_url());
        assertNull(params.getWsfed_result());
        assertNull(params.getWsfed_result_url());
    }

    @Test
    public void testSetters() {
        WSFedProtocolParameters params = new WSFedProtocolParameters();

        params.setWsfed_action("action");
        params.setWsfed_reply("reply");
        params.setWsfed_context("context");
        params.setWsfed_policy("policy");
        params.setWsfed_current_time("time");
        params.setWsfed_federation_id("fedid");
        params.setWsfed_encoding("encoding");
        params.setWsfed_realm("realm");
        params.setWsfed_freshness("freshness");
        params.setWsfed_authentication_level("authlevel");
        params.setWsfed_token_request_type("trt");
        params.setWsfed_home_realm("homerealm");
        params.setWsfed_request_url("req");
        params.setWsfed_result("res");
        params.setWsfed_result_url("resurl");

        assertEquals("action", params.getWsfed_action());
        assertEquals("reply", params.getWsfed_reply());
        assertEquals("context", params.getWsfed_context());
        assertEquals("policy", params.getWsfed_policy());
        assertEquals("time", params.getWsfed_current_time());
        assertEquals("fedid", params.getWsfed_federation_id());
        assertEquals("encoding", params.getWsfed_encoding());
        assertEquals("realm", params.getWsfed_realm());
        assertEquals("freshness", params.getWsfed_freshness());
        assertEquals("authlevel", params.getWsfed_authentication_level());
        assertEquals("trt", params.getWsfed_token_request_type());
        assertEquals("homerealm", params.getWsfed_home_realm());
        assertEquals("req", params.getWsfed_request_url());
        assertEquals("res", params.getWsfed_result());
        assertEquals("resurl", params.getWsfed_result_url());
    }

}
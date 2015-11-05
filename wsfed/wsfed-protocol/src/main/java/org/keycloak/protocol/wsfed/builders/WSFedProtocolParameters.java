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

import javax.ws.rs.core.MultivaluedMap;

public class WSFedProtocolParameters {
    protected String wsfed_action;
    protected String wsfed_reply;
    protected String wsfed_context;
    protected String wsfed_policy;
    protected String wsfed_current_time;
    protected String wsfed_federation_id;
    protected String wsfed_encoding;
    protected String wsfed_realm;
    protected String wsfed_freshness;
    protected String wsfed_authentication_level;
    protected String wsfed_token_request_type;
    protected String wsfed_home_realm;
    protected String wsfed_request_url;
    protected String wsfed_result;
    protected String wsfed_result_url;

    public WSFedProtocolParameters() {
    }

    public static WSFedProtocolParameters fromParameters(MultivaluedMap<String, String> requestParams) {
        WSFedProtocolParameters params = new WSFedProtocolParameters();

        if(requestParams.containsKey(WSFedConstants.WSFED_ACTION)) {
            params.setWsfed_action(requestParams.getFirst(WSFedConstants.WSFED_ACTION));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_REPLY)) {
            params.setWsfed_reply(requestParams.getFirst(WSFedConstants.WSFED_REPLY));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_CONTEXT)) {
            params.setWsfed_context(requestParams.getFirst(WSFedConstants.WSFED_CONTEXT));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_POLICY)) {
            params.setWsfed_policy(requestParams.getFirst(WSFedConstants.WSFED_POLICY));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_CURRENT_TIME)) {
            params.setWsfed_current_time(requestParams.getFirst(WSFedConstants.WSFED_CURRENT_TIME));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_FEDERATION_ID)) {
            params.setWsfed_federation_id(requestParams.getFirst(WSFedConstants.WSFED_FEDERATION_ID));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_ENCODING)) {
            params.setWsfed_encoding(requestParams.getFirst(WSFedConstants.WSFED_ENCODING));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_REALM)) {
            params.setWsfed_realm(requestParams.getFirst(WSFedConstants.WSFED_REALM));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_FRESHNESS)) {
            params.setWsfed_freshness(requestParams.getFirst(WSFedConstants.WSFED_FRESHNESS));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_AUTHENTICATION_LEVEL)) {
            params.setWsfed_authentication_level(requestParams.getFirst(WSFedConstants.WSFED_AUTHENTICATION_LEVEL));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_TOKEN_REQUEST_TYPE)) {
            params.setWsfed_token_request_type(requestParams.getFirst(WSFedConstants.WSFED_TOKEN_REQUEST_TYPE));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_HOME_REALM)) {
            params.setWsfed_home_realm(requestParams.getFirst(WSFedConstants.WSFED_HOME_REALM));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_REQUEST_URL)) {
            params.setWsfed_request_url(requestParams.getFirst(WSFedConstants.WSFED_REQUEST_URL));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_RESULT)) {
            params.setWsfed_result(requestParams.getFirst(WSFedConstants.WSFED_RESULT));
        }

        if(requestParams.containsKey(WSFedConstants.WSFED_RESULT_URL)) {
            params.setWsfed_result_url(requestParams.getFirst(WSFedConstants.WSFED_RESULT_URL));
        }

        return params;
    }

    public String getWsfed_action() {
        return wsfed_action;
    }

    public void setWsfed_action(String wsfed_action) {
        this.wsfed_action = wsfed_action;
    }

    public String getWsfed_reply() {
        return wsfed_reply;
    }

    public void setWsfed_reply(String wsfed_reply) {
        this.wsfed_reply = wsfed_reply;
    }

    public String getWsfed_context() {
        return wsfed_context;
    }

    public void setWsfed_context(String wsfed_context) {
        this.wsfed_context = wsfed_context;
    }

    public String getWsfed_policy() {
        return wsfed_policy;
    }

    public void setWsfed_policy(String wsfed_policy) {
        this.wsfed_policy = wsfed_policy;
    }

    public String getWsfed_current_time() {
        return wsfed_current_time;
    }

    public void setWsfed_current_time(String wsfed_current_time) {
        this.wsfed_current_time = wsfed_current_time;
    }

    public String getWsfed_federation_id() {
        return wsfed_federation_id;
    }

    public void setWsfed_federation_id(String wsfed_federation_id) {
        this.wsfed_federation_id = wsfed_federation_id;
    }

    public String getWsfed_encoding() {
        return wsfed_encoding;
    }

    public void setWsfed_encoding(String wsfed_encoding) {
        this.wsfed_encoding = wsfed_encoding;
    }

    public String getWsfed_realm() {
        return wsfed_realm;
    }

    public void setWsfed_realm(String wsfed_realm) {
        this.wsfed_realm = wsfed_realm;
    }

    public String getWsfed_freshness() {
        return wsfed_freshness;
    }

    public void setWsfed_freshness(String wsfed_freshness) {
        this.wsfed_freshness = wsfed_freshness;
    }

    public String getWsfed_authentication_level() {
        return wsfed_authentication_level;
    }

    public void setWsfed_authentication_level(String wsfed_authentication_level) {
        this.wsfed_authentication_level = wsfed_authentication_level;
    }

    public String getWsfed_token_request_type() {
        return wsfed_token_request_type;
    }

    public void setWsfed_token_request_type(String wsfed_token_request_type) {
        this.wsfed_token_request_type = wsfed_token_request_type;
    }

    public String getWsfed_home_realm() {
        return wsfed_home_realm;
    }

    public void setWsfed_home_realm(String wsfed_home_realm) {
        this.wsfed_home_realm = wsfed_home_realm;
    }

    public String getWsfed_request_url() {
        return wsfed_request_url;
    }

    public void setWsfed_request_url(String wsfed_request_url) {
        this.wsfed_request_url = wsfed_request_url;
    }

    public String getWsfed_result() {
        return wsfed_result;
    }

    public void setWsfed_result(String wsfed_result) {
        this.wsfed_result = wsfed_result;
    }

    public String getWsfed_result_url() {
        return wsfed_result_url;
    }

    public void setWsfed_result_url(String wsfed_result_url) {
        this.wsfed_result_url = wsfed_result_url;
    }
}

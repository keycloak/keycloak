/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */
package org.keycloak.protocol.oidc.grants.ciba.endpoints.request;

import javax.ws.rs.core.MultivaluedMap;

import java.util.Set;

/**
 * Parse the parameters from request body
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
class BackchannelAuthenticationEndpointRequestBodyParser extends BackchannelAuthenticationEndpointRequestParser {

    private final MultivaluedMap<String, String> requestParams;

    private String invalidRequestMessage = null;

    public BackchannelAuthenticationEndpointRequestBodyParser(MultivaluedMap<String, String> requestParams) {
        this.requestParams = requestParams;
    }

    @Override
    protected String getParameter(String paramName) {
        checkDuplicated(requestParams, paramName);
        return requestParams.getFirst(paramName);
    }

    @Override
    protected Integer getIntParameter(String paramName) {
        checkDuplicated(requestParams, paramName);
        String paramVal = requestParams.getFirst(paramName);
        return paramVal==null ? null : Integer.parseInt(paramVal);
    }

    public String getInvalidRequestMessage() {
        return invalidRequestMessage;
    }

    @Override
    protected Set<String> keySet() {
        return requestParams.keySet();
    }

    private void checkDuplicated(MultivaluedMap<String, String> requestParams, String paramName) {
        if (invalidRequestMessage == null) {
            if (requestParams.get(paramName) != null && requestParams.get(paramName).size() != 1) {
                invalidRequestMessage = "duplicated parameter";
            }
        }
    }

}

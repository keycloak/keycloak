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

import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;

/**
 * Parse the parameters from request body
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
class BackchannelAuthenticationEndpointRequestBodyParser extends BackchannelAuthenticationEndpointRequestParser {

    private static final Logger logger = Logger.getLogger(BackchannelAuthenticationEndpointRequestBodyParser.class);

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
        return paramVal==null ? null : Integer.valueOf(paramVal);
    }

    public String getInvalidRequestMessage() {
        return invalidRequestMessage;
    }

    @Override
    protected Set<String> keySet() {
        return requestParams.keySet();
    }

    @Override
    protected void extractAdditionalReqParams(Map<String, String> additionalReqParams) {
        // Reject reserved names supplied as standalone CIBA form parameters.
        // Copying them into additional parameters could overwrite server-controlled
        // authentication request claims during deserialization.
        for (String paramName : keySet()) {
            if (DISABLED_REQ_PARAMS.contains(paramName)) {
                logger.warnf("Reserved protocol claim '%s' is not allowed as a CIBA request parameter.", paramName);
                throw new RuntimeException("Reserved protocol claim '" + paramName + "' is not allowed as a CIBA request parameter.");
            }
        }
        super.extractAdditionalReqParams(additionalReqParams);
    }

    private void checkDuplicated(MultivaluedMap<String, String> requestParams, String paramName) {
        if (invalidRequestMessage == null) {
            if (requestParams.get(paramName) != null && requestParams.get(paramName).size() != 1) {
                invalidRequestMessage = "duplicated parameter";
            }
        }
    }

}

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
 */

package org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.protocol.oidc.grants.ciba.channel.CIBAAuthenticationRequest;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class BackchannelTokenRequestContext implements ClientPolicyContext {

    private final CIBAAuthenticationRequest request;
    private final MultivaluedMap<String, String> requestParameters;

    public BackchannelTokenRequestContext(CIBAAuthenticationRequest request,
            MultivaluedMap<String, String> requestParameters) {
        this.request = request;
        this.requestParameters = requestParameters;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.BACKCHANNEL_TOKEN_REQUEST;
    }

    public CIBAAuthenticationRequest getRequest() {
        return request;
    }

    public MultivaluedMap<String, String> getRequestParameters() {
        return requestParameters;
    }

}

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

package org.keycloak.protocol.oidc.grants.device.clientpolicy.context;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class DeviceTokenResponseContext implements ClientPolicyContext {

    private final OAuth2DeviceCodeModel deviceCodeModel;
    private final MultivaluedMap<String, String> requestParameters;
    private final AuthenticatedClientSessionModel clientSession;
    private final TokenManager.AccessTokenResponseBuilder accessTokenResponseBuilder;

    public DeviceTokenResponseContext(OAuth2DeviceCodeModel deviceCodeModel,
            MultivaluedMap<String, String> requestParameters,
            AuthenticatedClientSessionModel clientSession,
            TokenManager.AccessTokenResponseBuilder accessTokenResponseBuilder) {
        this.deviceCodeModel = deviceCodeModel;
        this.requestParameters = requestParameters;
        this.clientSession = clientSession;
        this.accessTokenResponseBuilder = accessTokenResponseBuilder;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.DEVICE_TOKEN_RESPONSE;
    }

    public OAuth2DeviceCodeModel getDeviceCodeModel() {
        return deviceCodeModel;
    }

    public MultivaluedMap<String, String> getRequestParameters() {
        return requestParameters;
    }

    public TokenManager.AccessTokenResponseBuilder getAccessTokenResponseBuilder() {
        return accessTokenResponseBuilder;
    }

    public AuthenticatedClientSessionModel getClientSession() {
        return clientSession;
    }

}
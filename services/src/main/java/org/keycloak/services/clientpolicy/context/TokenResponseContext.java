/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.context;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class TokenResponseContext implements ClientPolicyContext {

    private final MultivaluedMap<String, String> params;
    private final OAuth2CodeParser.ParseResult parseResult;
    private final ClientSessionContext clientSessionCtx;
    private final TokenManager.AccessTokenResponseBuilder accessTokenResponseBuilder;

    public TokenResponseContext(MultivaluedMap<String, String> params,
            OAuth2CodeParser.ParseResult parseResult,
            ClientSessionContext clientSessionCtx,
            TokenManager.AccessTokenResponseBuilder accessTokenResponseBuilder) {
        this.params = params;
        this.parseResult = parseResult;
        this.clientSessionCtx = clientSessionCtx;
        this.accessTokenResponseBuilder = accessTokenResponseBuilder;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.TOKEN_RESPONSE;
    }

    public MultivaluedMap<String, String> getParams() {
        return params;
    }

    public OAuth2CodeParser.ParseResult getParseResult() {
        return parseResult;
    }

    public TokenManager.AccessTokenResponseBuilder getAccessTokenResponseBuilder() {
        return accessTokenResponseBuilder;
    }

    public ClientSessionContext getClientSessionContext() {
        return clientSessionCtx;
    }

}

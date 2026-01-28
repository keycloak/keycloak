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

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class BackchannelAuthenticationEndpointRequest {

    String scope;
    String clientNotificationToken;
    String acr;
    String loginHintToken;
    String idTokenHint;
    String loginHint;
    String bindingMessage;
    String userCode;
    Integer requestedExpiry;

    String prompt;
    String nonce;
    Integer maxAge;
    String display;
    String uiLocales;
    String claims;

    Map<String, String> additionalReqParams = new HashMap<>();

    String invalidRequestMessage;

    public String getScope() {
        return scope;
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public String getAcr() {
        return acr;
    }

    public String getLoginHintToken() {
        return loginHintToken;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public String getUserCode() {
        return userCode;
    }

    public Integer getRequestedExpiry() {
        return requestedExpiry;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getNonce() {
        return nonce;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public String getDisplay() {
        return display;
    }

    public String getUiLocales() {
        return uiLocales;
    }

    public String getClaims() {
        return claims;
    }

    public Map<String, String> getAdditionalReqParams() {
        return additionalReqParams;
    }


    public String getInvalidRequestMessage() {
        return invalidRequestMessage;
    }
}

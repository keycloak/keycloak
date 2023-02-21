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

package org.keycloak.protocol.oidc.grants.ciba.channel;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthenticationChannelRequest {

    @JsonProperty(CibaGrantType.BINDING_MESSAGE)
    private String bindingMessage;

    @JsonProperty(CibaGrantType.LOGIN_HINT)
    private String loginHint;

    @JsonProperty(CibaGrantType.IS_CONSENT_REQUIRED)
    private Boolean consentRequired;

    @JsonProperty(OAuth2Constants.ACR_VALUES)
    private String acrValues;

    private Map<String, Object> additionalParameters = new HashMap<>();

    private String scope;

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setConsentRequired(Boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public Boolean getConsentRequired() {
        return consentRequired;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(Map<String, Object> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    @JsonAnySetter
    public void setAdditionalParameter(String name, String value) {
        additionalParameters.put(name, value);
    }
}

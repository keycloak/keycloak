package org.keycloak.protocol.ciba.endpoints.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackchannelAuthenticationRequest {

    String invalidRequestMessage;

    String scope;

    @JsonProperty("client_notification_token")
    String clientNotificationToken;

    @JsonProperty("acr_values")
    String acrValues;

    @JsonProperty("login_hint_token")
    String loginHintToken;

    @JsonProperty("id_token_hint")
    String idTokenHint;

    @JsonProperty("login_hint")
    String loginHint;

    @JsonProperty("binding_message")
    String bindingMessage;

    @JsonProperty("user_code")
    String userCode;

    @JsonProperty("requested_expiry")
    String requestedExpiry;

    public String getInvalidRequestMessage() {
        return invalidRequestMessage;
    }

    public void setInvalidRequestMessage(String invalidRequestMessage) {
        this.invalidRequestMessage = invalidRequestMessage;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public String getLoginHintToken() {
        return loginHintToken;
    }

    public void setLoginHintToken(String loginHintToken) {
        this.loginHintToken = loginHintToken;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getRequestedExpiry() {
        return requestedExpiry;
    }

    public void setRequestedExpiry(String requestedExpiry) {
        this.requestedExpiry = requestedExpiry;
    }

}

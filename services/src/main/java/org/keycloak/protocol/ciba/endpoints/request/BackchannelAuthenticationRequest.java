/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.ciba.endpoints.request;

public class BackchannelAuthenticationRequest {

    String invalidRequestMessage;

    String scope;
    String client_notification_token;
    String acr_values;
    String login_hint_token;
    String id_token_hint;
    String login_hint;
    String binding_message;
    String user_code;
    String requested_expiry;

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
        return client_notification_token;
    }

    public void setClientNotificationToken(String client_notification_token) {
        this.client_notification_token = client_notification_token;
    }

    public String getAcrValues() {
        return acr_values;
    }

    public void setAcrValues(String acr_values) {
        this.acr_values = acr_values;
    }

    public String getLoginHintToken() {
        return login_hint_token;
    }

    public void setLoginHintToken(String login_hint_token) {
        this.login_hint_token = login_hint_token;
    }

    public String getIdTokenHint() {
        return id_token_hint;
    }

    public void setIdTokenHint(String id_token_hint) {
        this.id_token_hint = id_token_hint;
    }

    public String getLoginHint() {
        return login_hint;
    }

    public void setLoginHint(String login_hint) {
        this.login_hint = login_hint;
    }

    public String getBindingMessage() {
        return binding_message;
    }

    public void setBindingMessage(String binding_message) {
        this.binding_message = binding_message;
    }

    public String getUserCode() {
        return user_code;
    }

    public void setUserCode(String user_code) {
        this.user_code = user_code;
    }

    public String getRequestedExpiry() {
        return requested_expiry;
    }

    public void setRequestedExpiry(String requested_expiry) {
        this.requested_expiry = requested_expiry;
    }

}

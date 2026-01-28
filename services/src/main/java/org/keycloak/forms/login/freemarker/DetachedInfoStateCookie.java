/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.forms.login.freemarker;

import java.util.List;

import org.keycloak.Token;
import org.keycloak.TokenCategory;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cookie encapsulating data to be displayed on the info/error page. We need this data due the fact that authenticationSession may not exists.
 * This is needed so the info/error page can be restored after user changed language.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DetachedInfoStateCookie implements Token {

    @JsonProperty("mky")
    private String messageKey;

    @JsonProperty("mty")
    private String messageType;

    @JsonProperty("mpar")
    private List<String> messageParameters;

    @JsonProperty("stat")
    private Integer status;

    @JsonProperty("clid")
    private String clientUuid;

    @JsonProperty("st1")
    private String currentUrlState;

    @JsonProperty("st2")
    private String renderedUrlState;

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public List<String> getMessageParameters() {
        return messageParameters;
    }

    public void setMessageParameters(List<String> messageParameters) {
        this.messageParameters = messageParameters;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getCurrentUrlState() {
        return currentUrlState;
    }

    public void setCurrentUrlState(String currentUrlState) {
        this.currentUrlState = currentUrlState;
    }

    public String getRenderedUrlState() {
        return renderedUrlState;
    }

    public void setRenderedUrlState(String renderedUrlState) {
        this.renderedUrlState = renderedUrlState;
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.INTERNAL;
    }

    @Override
    public String toString() {
        return new StringBuilder("DetachedInfoStateCookie [ ")
                .append("messageKey=" + messageKey)
                .append(", messageType=" + messageType)
                .append(", status=" + status)
                .append(", clientUuid=" + clientUuid)
                .append(", messageParameters=" + messageParameters)
                .append(", currentUrlState=" + currentUrlState)
                .append(", renderedUrlState=" + renderedUrlState)
                .append(" ]")
                .toString();
    }
}

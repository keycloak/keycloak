/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.forms.login.freemarker.model;

import org.keycloak.models.ClientModel;

/**
 * Bean for OAuth2 Device Authorization confirmation page.
 *
 */
public class OAuth2DeviceConfirmBean {

    private final ClientModel client;
    private final String userCode;
    private final String code;

    public OAuth2DeviceConfirmBean(ClientModel client, String userCode, String code) {
        this.client = client;
        this.userCode = userCode;
        this.code = code;
    }

    public String getClient() {
        return client.getClientId();
    }

    public String getClientName() {
        String name = client.getName();
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return client.getClientId();
    }

    public String getUserCode() {
        return userCode;
    }

    public String getCode() {
        return code;
    }
}

/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.admin.client;

import static org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS;
import static org.keycloak.OAuth2Constants.PASSWORD;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class Config {

    private String serverUrl;
    private String realm;
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;
    private String grantType;
    private String scope;

    public Config(String serverUrl, String realm, String username, String password, String clientId, String clientSecret) {
        this(serverUrl, realm, username, password, clientId, clientSecret, PASSWORD, null);
    }

    public Config(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, String grantType, String scope) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.grantType = grantType;
        checkGrantType(grantType);
        this.scope = scope;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isPublicClient() {
        return clientSecret == null;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
        checkGrantType(grantType);
    }

    public static void checkGrantType(String grantType) {
        if (grantType != null && !PASSWORD.equals(grantType) && !CLIENT_CREDENTIALS.equals(grantType)) {
            throw new IllegalArgumentException("Unsupported grantType: " + grantType +
                    " (only " + PASSWORD + " and " + CLIENT_CREDENTIALS + " are supported)");
        }
    }
}

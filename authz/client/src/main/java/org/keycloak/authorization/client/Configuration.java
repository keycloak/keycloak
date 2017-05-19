/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
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
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.BasicAuthHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Configuration extends AdapterConfig {

    @JsonIgnore
    private HttpClient httpClient;

    public Configuration() {

    }

    public Configuration(String authServerUrl, String realm, String clientId, Map<String, Object> clientCredentials, HttpClient httpClient) {
        this.authServerUrl = authServerUrl;
        setAuthServerUrl(authServerUrl);
        setRealm(realm);
        setResource(clientId);
        setCredentials(clientCredentials);
        this.httpClient = httpClient;
    }

    @JsonIgnore
    private ClientAuthenticator clientAuthenticator = new ClientAuthenticator() {
        @Override
        public void configureClientCredentials(HashMap<String, String> requestParams, HashMap<String, String> requestHeaders) {
            String secret = (String) getCredentials().get("secret");

            if (secret == null) {
                throw new RuntimeException("Client secret not provided.");
            }

            requestHeaders.put("Authorization", BasicAuthHelper.createHeader(getResource(), secret));
        }
    };

    public HttpClient getHttpClient() {
        if (this.httpClient == null) {
            this.httpClient = HttpClients.createDefault();
        }

        return httpClient;
    }

    public ClientAuthenticator getClientAuthenticator() {
        return this.clientAuthenticator;
    }
}

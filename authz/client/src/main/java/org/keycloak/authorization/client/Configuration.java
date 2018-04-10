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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.BasicAuthHelper;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Configuration extends AdapterConfig {

    @JsonIgnore
    private HttpClient httpClient;

    @JsonIgnore
    private ClientAuthenticator clientAuthenticator = createDefaultClientAuthenticator();

    public Configuration() {

    }

    /**
     * Creates a new instance.
     *
     * @param authServerUrl the server's URL. E.g.: http://{server}:{port}/auth.(not {@code null})
     * @param realm the realm name (not {@code null})
     * @param clientId the client id (not {@code null})
     * @param clientCredentials a map with the client credentials (not {@code null})
     * @param httpClient the {@link HttpClient} instance that should be used when sending requests to the server, or {@code null} if a default instance should be created
     */
    public Configuration(String authServerUrl, String realm, String clientId, Map<String, Object> clientCredentials, HttpClient httpClient) {
        this.authServerUrl = authServerUrl;
        setAuthServerUrl(authServerUrl);
        setRealm(realm);
        setResource(clientId);
        setCredentials(clientCredentials);
        this.httpClient = httpClient;
    }

    public HttpClient getHttpClient() {
        if (this.httpClient == null) {
            this.httpClient = HttpClients.createDefault();
        }
        return httpClient;
    }

    ClientAuthenticator getClientAuthenticator() {
        return this.clientAuthenticator;
    }

    /**
     * Creates a default client authenticator which uses HTTP BASIC and client id and secret to authenticate the client.
     *
     * @return the default client authenticator
     */
    private ClientAuthenticator createDefaultClientAuthenticator() {
        return new ClientAuthenticator() {
            @Override
            public void configureClientCredentials(Map<String, List<String>> requestParams, Map<String, String> requestHeaders) {
                String secret = (String) getCredentials().get("secret");

                if (secret == null) {
                    throw new RuntimeException("Client secret not provided.");
                }

                requestHeaders.put("Authorization", BasicAuthHelper.createHeader(getResource(), secret));
            }
        };
    }
}

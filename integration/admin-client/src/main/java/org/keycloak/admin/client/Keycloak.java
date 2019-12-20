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

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.admin.client.token.TokenManager;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import java.net.URI;

import static org.keycloak.OAuth2Constants.PASSWORD;

/**
 * Provides a Keycloak client. By default, this implementation uses a {@link ResteasyClient RESTEasy client} with the
 * default {@link ResteasyClientBuilder} settings. To customize the underling client, use a {@link KeycloakBuilder} to
 * create a Keycloak client.
 *
 * To read Responses, you can use {@link CreatedResponseUtil} for objects created
 *
 * @author rodrigo.sasaki@icarros.com.br
 * @see KeycloakBuilder
 */
public class Keycloak implements AutoCloseable {
    private final Config config;
    private final TokenManager tokenManager;
    private final String authToken;
    private final ResteasyWebTarget target;
    private final ResteasyClient client;

    Keycloak(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, String grantType, ResteasyClient resteasyClient, String authtoken) {
        config = new Config(serverUrl, realm, username, password, clientId, clientSecret, grantType);
        client = resteasyClient != null ? resteasyClient : newRestEasyClient(null, null, false);
        authToken = authtoken;
        tokenManager = authtoken == null ? new TokenManager(config, client) : null;

        target = client.target(config.getServerUrl());
        target.register(newAuthFilter());
    }

    private BearerAuthFilter newAuthFilter() {
        return authToken != null ? new BearerAuthFilter(authToken) : new BearerAuthFilter(tokenManager);
    }

    private static ResteasyClient newRestEasyClient(ResteasyJackson2Provider customJacksonProvider, SSLContext sslContext, boolean disableTrustManager) {
        ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder()
              .sslContext(sslContext)
              .connectionPoolSize(10);

        if (disableTrustManager) {
            // Disable PKIX path validation errors when running tests using SSL
            clientBuilder.disableTrustManager().hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY);
        }

        if (customJacksonProvider != null) {
            clientBuilder.register(customJacksonProvider, 100);
        }

        return clientBuilder.build();
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, SSLContext sslContext, ResteasyJackson2Provider customJacksonProvider, boolean disableTrustManager, String authToken) {
        return new Keycloak(serverUrl, realm, username, password, clientId, clientSecret, PASSWORD, newRestEasyClient(customJacksonProvider, sslContext, disableTrustManager), authToken);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret) {
        return getInstance(serverUrl, realm, username, password, clientId, clientSecret, null, null, false, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, SSLContext sslContext) {
        return getInstance(serverUrl, realm, username, password, clientId, clientSecret, sslContext, null, false, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, SSLContext sslContext, ResteasyJackson2Provider customJacksonProvider) {
        return getInstance(serverUrl, realm, username, password, clientId, clientSecret, sslContext, customJacksonProvider, false, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId) {
        return getInstance(serverUrl, realm, username, password, clientId, null, null, null, false, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, SSLContext sslContext) {
        return getInstance(serverUrl, realm, username, password, clientId, null, sslContext, null, false, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String clientId, String authToken) {
        return getInstance(serverUrl, realm, null, null, clientId, null, null, null, false, authToken);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String clientId, String authToken, SSLContext sllSslContext) {
        return getInstance(serverUrl, realm, null, null, clientId, null, sllSslContext, null, false, authToken);
    }

    public RealmsResource realms() {
        return target.proxy(RealmsResource.class);
    }

    public RealmResource realm(String realmName) {
        return realms().realm(realmName);
    }

    public ServerInfoResource serverInfo() {
        return target.proxy(ServerInfoResource.class);
    }

    public TokenManager tokenManager() {
        return tokenManager;
    }

    /**
     * Create a secure proxy based on an absolute URI.
     * All set up with appropriate token
     *
     * @param proxyClass
     * @param absoluteURI
     * @param <T>
     * @return
     */
    public <T> T proxy(Class<T> proxyClass, URI absoluteURI) {
        return client.target(absoluteURI).register(newAuthFilter()).proxy(proxyClass);
    }

    /**
     * Closes the underlying client. After calling this method, this <code>Keycloak</code> instance cannot be reused.
     */
    @Override
    public void close() {
        client.close();
    }

    /**
     * @return true if the underlying client is closed.
     */
    public boolean isClosed() {
        return client.isClosed();
    }
}

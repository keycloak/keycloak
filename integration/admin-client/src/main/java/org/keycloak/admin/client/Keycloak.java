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

import javax.ws.rs.client.WebTarget;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.admin.client.spi.ResteasyClientProvider;
import org.keycloak.admin.client.token.TokenManager;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.Iterator;
import java.util.ServiceLoader;

import static org.keycloak.OAuth2Constants.PASSWORD;

/**
 * Provides a Keycloak client. By default, this implementation uses a the default RestEasy client builder settings.
 * To customize the underling client, use a {@link KeycloakBuilder} to create a Keycloak client.
 *
 * To read Responses, you can use {@link CreatedResponseUtil} for objects created
 *
 * @author rodrigo.sasaki@icarros.com.br
 * @see KeycloakBuilder
 */
public class Keycloak implements AutoCloseable {

    private static volatile ResteasyClientProvider CLIENT_PROVIDER = resolveResteasyClientProvider();

    private static ResteasyClientProvider resolveResteasyClientProvider() {
        Iterator<ResteasyClientProvider> providers = ServiceLoader.load(ResteasyClientProvider.class).iterator();

        if (providers.hasNext()) {
            ResteasyClientProvider provider = providers.next();

            if (providers.hasNext()) {
                throw new IllegalArgumentException("Multiple " + ResteasyClientProvider.class + " implementations found");
            }

            return provider;
        }

        return createDefaultResteasyClientProvider();
    }

    private static ResteasyClientProvider createDefaultResteasyClientProvider() {
        try {
            return (ResteasyClientProvider) Keycloak.class.getClassLoader().loadClass("org.keycloak.admin.client.spi.ResteasyClientClassicProvider").getDeclaredConstructor().newInstance();
        } catch (Exception cause) {
            throw new RuntimeException("Could not instantiate default client provider", cause);
        }
    }

    public static void setClientProvider(ResteasyClientProvider provider) {
        CLIENT_PROVIDER = provider;
    }

    public static ResteasyClientProvider getClientProvider() {
        return CLIENT_PROVIDER;
    }

    private final Config config;
    private final TokenManager tokenManager;
    private final String authToken;
    private final WebTarget target;
    private final Client client;
    private boolean closed = false;

    Keycloak(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, String grantType, Client resteasyClient, String authtoken, String scope) {
        config = new Config(serverUrl, realm, username, password, clientId, clientSecret, grantType, scope);
        client = resteasyClient != null ? resteasyClient : newRestEasyClient(null, null, false);
        authToken = authtoken;
        tokenManager = authtoken == null ? new TokenManager(config, client) : null;

        target = client.target(config.getServerUrl());
        target.register(newAuthFilter());
    }

    private static Client newRestEasyClient(Object customJacksonProvider, SSLContext sslContext, boolean disableTrustManager) {
        return CLIENT_PROVIDER.newRestEasyClient(customJacksonProvider, sslContext, disableTrustManager);
    }

    private BearerAuthFilter newAuthFilter() {
        return authToken != null ? new BearerAuthFilter(authToken) : new BearerAuthFilter(tokenManager);
    }
    
    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, SSLContext sslContext, Object customJacksonProvider, boolean disableTrustManager, String authToken, String scope) {
        return new Keycloak(serverUrl, realm, username, password, clientId, clientSecret, PASSWORD, newRestEasyClient(customJacksonProvider, sslContext, disableTrustManager), authToken, scope);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, SSLContext sslContext, Object customJacksonProvider, boolean disableTrustManager, String authToken) {
        return new Keycloak(serverUrl, realm, username, password, clientId, clientSecret, PASSWORD, newRestEasyClient(customJacksonProvider, sslContext, disableTrustManager), authToken, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret) {
        return getInstance(serverUrl, realm, username, password, clientId, clientSecret, null, null, false, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, SSLContext sslContext) {
        return getInstance(serverUrl, realm, username, password, clientId, clientSecret, sslContext, null, false, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, SSLContext sslContext, Object customJacksonProvider) {
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
        return CLIENT_PROVIDER.targetProxy(target, RealmsResource.class);
    }

    public RealmResource realm(String realmName) {
        return realms().realm(realmName);
    }

    public ServerInfoResource serverInfo() {
        return CLIENT_PROVIDER.targetProxy(target, ServerInfoResource.class);
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
        WebTarget register = client.target(absoluteURI).register(newAuthFilter());
        return CLIENT_PROVIDER.targetProxy(register, proxyClass);
    }

    /**
     * Closes the underlying client. After calling this method, this <code>Keycloak</code> instance cannot be reused.
     */
    @Override
    public void close() {
        closed = true;
        client.close();
    }

    /**
     * @return true if the underlying client is closed.
     */
    public boolean isClosed() {
        return closed;
    }
}

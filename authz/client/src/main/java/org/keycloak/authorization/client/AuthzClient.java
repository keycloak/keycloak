/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.client;

import java.io.IOException;
import java.io.InputStream;

import org.keycloak.authorization.client.representation.ServerConfiguration;
import org.keycloak.authorization.client.resource.AuthorizationResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.authorization.client.util.TokenCallable;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.representations.AccessTokenResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.keycloak.constants.ServiceUrlConstants.AUTHZ_DISCOVERY_URL;

/**
 * <p>This is class serves as an entry point for clients looking for access to Keycloak Authorization Services.
 *
 * <p>When creating a new instances make sure you have a Keycloak Server running at the location specified in the client
 * configuration. The client tries to obtain server configuration by invoking the UMA Discovery Endpoint, usually available
 * from the server at <i>http(s)://{server}:{port}/auth/realms/{realm}/.well-known/uma-configuration</i>.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthzClient {

    private final Http http;
    private TokenCallable patSupplier;

    /**
     * <p>Creates a new instance.
     *
     * <p>This method expects a <code>keycloak.json</code> in the classpath, otherwise an exception will be thrown.
     *
     * @return a new instance
     * @throws RuntimeException in case there is no <code>keycloak.json</code> file in the classpath or the file could not be parsed
     */
    public static AuthzClient create() throws RuntimeException {
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("keycloak.json");

        return create(configStream);
    }

    /**
     * <p>Creates a new instance.
     *
     * @param configStream the input stream with the configuration data
     * @return a new instance
     */
    public static AuthzClient create(InputStream configStream) throws RuntimeException {
        if (configStream == null) {
            throw new IllegalArgumentException("Config input stream can not be null");
        }

        try {
            ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());

            mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

            return create(mapper.readValue(configStream, Configuration.class));
        } catch (IOException e) {
            throw new RuntimeException("Could not parse configuration.", e);
        }
    }

    /**
     * <p>Creates a new instance.
     *
     * @param configuration the client configuration
     * @return a new instance
     */
    public static AuthzClient create(Configuration configuration) {
        CryptoIntegration.init(AuthzClient.class.getClassLoader());
        return new AuthzClient(configuration);
    }

    private final ServerConfiguration serverConfiguration;
    private final Configuration configuration;

    /**
     * <p>Creates a {@link ProtectionResource} instance which can be used to access the Protection API.
     *
     * <p>When using this method, the PAT (the access token with the uma_protection scope) is obtained for the client
     * itself, using any of the supported credential types (client/secret, jwt, etc).
     *
     * @return a {@link ProtectionResource}
     */
    public ProtectionResource protection() {
        return new ProtectionResource(this.http, this.serverConfiguration, configuration, createPatSupplier());
    }

    /**
     * <p>Creates a {@link ProtectionResource} instance which can be used to access the Protection API.
     *
     * @param accessToken the PAT (the access token with the uma_protection scope)
     * @return a {@link ProtectionResource}
     */
    public ProtectionResource protection(final String accessToken) {
        return new ProtectionResource(this.http, this.serverConfiguration, configuration, new TokenCallable(http, configuration, serverConfiguration) {
            @Override
            public String call() {
                return accessToken;
            }

            @Override
            protected boolean isRetry() {
                return false;
            }
        });
    }

    /**
     * <p>Creates a {@link ProtectionResource} instance which can be used to access the Protection API.
     *
     * <p>When using this method, the PAT (the access token with the uma_protection scope) is obtained for a given user.
     *
     * @return a {@link ProtectionResource}
     */
    public ProtectionResource protection(String userName, String password) {
        return new ProtectionResource(this.http, this.serverConfiguration, configuration, createPatSupplier(userName, password));
    }

    /**
     * <p>Creates a {@link AuthorizationResource} instance which can be used to obtain permissions from the server.
     *
     * @return a {@link AuthorizationResource}
     */
    public AuthorizationResource authorization() {
        return new AuthorizationResource(configuration, serverConfiguration, this.http, null);
    }

    /**
     * <p>Creates a {@link AuthorizationResource} instance which can be used to obtain permissions from the server.
     *
     * @param accessToken the Access Token that will be used as a bearer to access the token endpoint
     * @return a {@link AuthorizationResource}
     */
    public AuthorizationResource authorization(final String accessToken) {
        return new AuthorizationResource(configuration, serverConfiguration, this.http, new TokenCallable(http, configuration, serverConfiguration) {
            @Override
            public String call() {
                return accessToken;
            }

            @Override
            protected boolean isRetry() {
                return false;
            }
        });
    }

    /**
     * <p>Creates a {@link AuthorizationResource} instance which can be used to obtain permissions from the server.
     *
     * @param userName an ID Token or Access Token representing an identity and/or access context
     * @param password
     * @return a {@link AuthorizationResource}
     */
    public AuthorizationResource authorization(final String userName, final String password) {
        return authorization(userName, password, null);
    }

    public AuthorizationResource authorization(final String userName, final String password, final String scope) {
        return new AuthorizationResource(configuration, serverConfiguration, this.http,
            createRefreshableAccessTokenSupplier(userName, password, scope));
    }

    /**
     * Obtains an access token using the client credentials.
     *
     * @return an {@link AccessTokenResponse}
     */
    public AccessTokenResponse obtainAccessToken() {
        return this.http.<AccessTokenResponse>post(this.serverConfiguration.getTokenEndpoint())
                .authentication()
                    .client()
                .response()
                    .json(AccessTokenResponse.class)
                .execute();
    }

    /**
     * Obtains an access token using the resource owner credentials.
     *
     * @return an {@link AccessTokenResponse}
     */
    public AccessTokenResponse obtainAccessToken(String userName, String password) {
        return this.http.<AccessTokenResponse>post(this.serverConfiguration.getTokenEndpoint())
                .authentication()
                    .oauth2ResourceOwnerPassword(userName, password)
                .response()
                    .json(AccessTokenResponse.class)
                .execute();
    }

    /**
     * Returns the configuration obtained from the server at the UMA Discovery Endpoint.
     *
     * @return the {@link ServerConfiguration}
     */
    public ServerConfiguration getServerConfiguration() {
        return this.serverConfiguration;
    }

    /**
     * Obtains the client configuration
     *
     * @return the {@link Configuration}
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    private AuthzClient(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Client configuration can not be null.");
        }

        String configurationUrl = configuration.getAuthServerUrl();

        if (configurationUrl == null) {
            throw new IllegalArgumentException("Configuration URL can not be null.");
        }

        configurationUrl = KeycloakUriBuilder.fromUri(configurationUrl).clone().path(AUTHZ_DISCOVERY_URL).build(configuration.getRealm()).toString();
        this.configuration = configuration;

        this.http = new Http(configuration, configuration.getClientCredentialsProvider());

        try {
            this.serverConfiguration = this.http.<ServerConfiguration>get(configurationUrl)
                    .response().json(ServerConfiguration.class)
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain configuration from server [" + configurationUrl + "].", e);
        }
    }

    public TokenCallable createPatSupplier(String userName, String password) {
        if (patSupplier == null) {
            patSupplier = createRefreshableAccessTokenSupplier(userName, password);
        }
        return patSupplier;
    }

    public TokenCallable createPatSupplier() {
        return createPatSupplier(null, null);
    }

    private TokenCallable createRefreshableAccessTokenSupplier(final String userName, final String password) {
        return createRefreshableAccessTokenSupplier(userName, password, null);
    }

    private TokenCallable createRefreshableAccessTokenSupplier(final String userName, final String password,
        final String scope) {
        return new TokenCallable(userName, password, scope, http, configuration, serverConfiguration);
    }
}

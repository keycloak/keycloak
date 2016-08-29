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

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.admin.client.token.TokenManager;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.keycloak.OAuth2Constants.PASSWORD;

/**
 * Provides a Keycloak client. By default, this implementation uses a {@link Client JAX-RS client} with the
 * default {@link ClientBuilder} settings. To customize the underling client, use a {@link KeycloakBuilder} to
 * create a Keycloak client.
 *
 * @author rodrigo.sasaki@icarros.com.br
 * @author Luigi De Masi luigi.demasi@extrasys.it
 * @see KeycloakBuilder
 */
public class Keycloak {
    private final Config config;
    private final TokenManager tokenManager;
    private  WebClient client;

    Keycloak(String serverUrl, String realm, String username, String password, String clientId, String clientSecret, String grantType, WebClient client) {
        config = new Config(serverUrl, realm, username, password, clientId, clientSecret, grantType);
        tokenManager = new TokenManager(config);
        List<Object> providers = new ArrayList<>();
        providers.add(new BearerAuthFilter(tokenManager));
        providers.add(new JacksonJaxbJsonProvider());
        this.client = client != null ? client :  WebClient.create(serverUrl,providers);

    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret) {
        return new Keycloak(serverUrl, realm, username, password, clientId, clientSecret, PASSWORD, null);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId) {
        return new Keycloak(serverUrl, realm, username, password, clientId, null, PASSWORD, null);
    }

    public RealmsResource realms() {
        return JAXRSClientFactory.fromClient(client, RealmsResource.class);
    }

    public RealmResource realm(String realmName) {
        return realms().realm(realmName);
    }

    public ServerInfoResource serverInfo() {
        return JAXRSClientFactory.fromClient(client,ServerInfoResource.class);
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
        List providers = new ArrayList();
        providers.add(new BearerAuthFilter(tokenManager));
        providers.add(new JacksonJaxbJsonProvider());
        WebClient.create(absoluteURI.toString(),providers);
        return JAXRSClientFactory.fromClient(client,proxyClass);
    }

    /**
     * Closes the underlying client. After calling this method, this <code>Keycloak</code> instance cannot be reused.
     */
    public void close() {
        client.close();
    }
}

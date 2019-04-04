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
package org.keycloak.authorization.client.util;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.methods.RequestBuilder;
import org.jboss.logging.Logger;
import org.keycloak.authorization.client.ClientAuthenticator;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.ServerConfiguration;
import org.keycloak.common.util.KeycloakUriBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Http {

    private final Configuration configuration;
    private final ClientAuthenticator authenticator;
    private ServerConfiguration serverConfiguration; // Where is this used ?
    private static final Logger LOGGER = Logger.getLogger(Http.class);

    public Http(Configuration configuration, ClientAuthenticator authenticator) {
        this.configuration = configuration;
        this.authenticator = authenticator;
    }

    public <R> HttpMethod<R> get(String path) {
        return method(RequestBuilder.get().setUri(serverUrl(path)));
    }

    public <R> HttpMethod<R> post(String path) {
        return method(RequestBuilder.post().setUri(serverUrl(path)));
    }

    public <R> HttpMethod<R> put(String path) {
        return method(RequestBuilder.put().setUri(serverUrl(path)));
    }

    public <R> HttpMethod<R> delete(String path) {
        return method(RequestBuilder.delete().setUri(serverUrl(path)));
    }

    private <R> HttpMethod<R> method(RequestBuilder builder) {
        return new HttpMethod(this.configuration, authenticator, builder);
    }

    public void setServerConfiguration(ServerConfiguration serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }

    public String serverUrl(String endpointUrl) {
        LOGGER.debug("Input Endpoint Url : ["+endpointUrl+"]");

        if (configuration.getAuthServerBackchannelUrl() != null) {
            String host = URI.create(configuration.getAuthServerBackchannelUrl()).getHost();
              if (host != null) {
                LOGGER.debug("Host: "+ host);
                KeycloakUriBuilder keycloakUriBuilder = KeycloakUriBuilder.fromUri(endpointUrl);
                final String internalEndpoint = keycloakUriBuilder.host(host).build().toString();
                LOGGER.debug("Endpoint ['"+endpointUrl+"'] is converted to the internal ['"+internalEndpoint+"']");
                return internalEndpoint;
              }
        }

        return endpointUrl;
    }
}

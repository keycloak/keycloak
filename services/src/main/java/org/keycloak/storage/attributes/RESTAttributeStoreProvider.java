/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.attributes;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.ProcessingException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.VerificationException;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientBuilder;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.theme.TemplatingUtil;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.*;
import java.security.KeyStore;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Ben Cresitello-Dittmar
 *
 * This class implements the {@link AttributeStoreProvider} interface to fetch attributes from an external REST API
 */
public class RESTAttributeStoreProvider implements AttributeStoreProvider
{
    private static final Logger logger = Logger.getLogger(RESTAttributeStoreProvider.class);

    protected final KeycloakSession session;
    protected final ComponentModel component;

    protected final RESTAttributeStoreProviderConfig config;

    public RESTAttributeStoreProvider(RESTAttributeStoreProviderFactory factory, KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.component = model;
        try {
            this.config = RESTAttributeStoreProviderConfig.parse(session, model);
        } catch (VerificationException e){
            throw new RuntimeException("failed to parse component configuration: %s", e);
        }
    }

    public void close() {}

    /**
     * Request attributes from the configured REST API for the specified user
     * @param session The keycloak session
     * @param realm The realm the request is taking place in
     * @param user The user to fetch attributes from the external data store for
     * @return The attributes
     * @throws ProcessingException thrown if attributes for the specified user cannot be retrieved
     */
    public Map<String, Object> getAttributes(KeycloakSession session, RealmModel realm, UserModel user) throws ProcessingException {
        CloseableHttpClient httpClient = getHttpClient(session);

        // prepare request
        HttpUriRequest request = createHttpRequest(user);
        config.getHeaders().forEach((k,v) -> request.setHeader(new BasicHeader(k, v)));

        String content = sendRequest(httpClient, request);

        return parseResponse(content);
    }

    /**
     * Helper function to create the REST API request for the given user. This formats the configured URL with the user
     * attributes and create a request object of the appropriate method.
     * @param user The user to get attributes for
     * @return The initialized request object
     */
    private HttpUriRequest createHttpRequest(UserModel user){
        URI formattedUrl = encodeUrl(TemplatingUtil.resolveVariables(config.getUrl(), getUrlProperties(user)));

        return switch (config.getMethod()) {
            case GET -> new HttpGet(formattedUrl);
            case POST -> new HttpPost(formattedUrl);
        };
    }

    /**
     * Helper function to send the request and return the response content
     * @param httpClient The http client to use to send the request
     * @param request The request to execute
     * @return The response content (unparsed)
     * @throws ProcessingException thrown when the request fails (response code is not 2xx)
     */
    private String sendRequest(CloseableHttpClient httpClient, HttpUriRequest request) throws ProcessingException {
        CloseableHttpResponse res;
        int status;
        String content;
        try {
            res = httpClient.execute(request);
            status = res.getStatusLine().getStatusCode();
            content = EntityUtils.toString(res.getEntity());
        } catch (IOException e) {
            throw new ProcessingException(String.format("failed to request attributes from %s: %s", request.getURI(), e));
        }

        if (!(status >= 200 && status < 300)){
            throw new ProcessingException(String.format("http request failed with status %s: %s", status, content));
        }

        return content;
    }

    /**
     * Helper function to parse the response content. Attempts to deserialize the content as a JSON-like object
     * @param content The response content
     * @return The attributes parsed from the response content
     * @throws ProcessingException thrown if the attributes cannot be parsed as a JSON-like object
     */
    private Map<String, Object> parseResponse(String content) throws ProcessingException {
        Map<String, Object> attributes;
        try {
            attributes = JsonSerialization.readValue(content, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e){
            throw new ProcessingException(String.format("failed to parse response JSON (%s): %s", content, e));
        }

        return attributes;
    }

    /**
     * Helper function to encode the URL
     * @param rawUrl The unencoded URL string
     * @return The encoded URL
     */
    private URI encodeUrl(String rawUrl){
        URI uri;
        try {
            URL url = new URL(rawUrl);
            uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        } catch (MalformedURLException | URISyntaxException e){
            throw new ProcessingException(String.format("failed to encode URL (%s): %s", rawUrl, e));
        }
        return uri;
    }


    /**
     * Helper function to get the user attributes as a set of properties. Used to template the configured URL.
     * @param user The user
     * @return The user attributes as a set of properties
     */
    private Properties getUrlProperties(UserModel user){
        Properties props = new Properties();
        user.getAttributes().forEach((n, v) -> {
            String val = user.getFirstAttribute(n);
            if (val != null){
                props.setProperty(n, val);
            }
        });

        logger.debugf("providing properties to formatter: %s", props);

        return props;
    }

    /**
     * Helper function to get the http client from the keycloak session. The default http client is returned unless a TLS
     * client certificate is configured in the provider configuration.
     * @param session The keycloak session
     * @return the http client
     */
    private CloseableHttpClient getHttpClient(KeycloakSession session){
        if (config.getTlsClientCert() == null){
            logger.debugf("no TLS client certificate set, using default http client");
            return session.getProvider(HttpClientProvider.class).getHttpClient();
        } else {
            logger.debugf("TLS client certificate set, creating custom http client");
            return createHttpClient(config.getTlsClientCert(), session.getProvider(TruststoreProvider.class));
        }
    }

    /**
     * Helper function to create an HTTP client using the specified truststore and key pair for TLS client authenticaiton
     * @param tlsClientCert The client certificate to use for the TLS connection
     * @param trustStoreProvider The truststore used to verify requests from this client
     * @return The initialized and configured http client
     */
    private CloseableHttpClient createHttpClient(KeyStore tlsClientCert, TruststoreProvider trustStoreProvider){
        HttpClientBuilder builder = new HttpClientBuilder();
        builder.socketTimeout(5000L, TimeUnit.MILLISECONDS)
                .establishConnectionTimeout(-1L, TimeUnit.MILLISECONDS)
                .maxPooledPerRoute(64)
                .connectionPoolSize(128)
                .reuseConnections(true)
                .connectionTTL(-1L, TimeUnit.MILLISECONDS)
                .maxConnectionIdleTime(900000L, TimeUnit.MILLISECONDS)
                .disableCookies(true)
                .expectContinueEnabled(false)
                .reuseConnections(true);

        builder.hostnameVerification(trustStoreProvider.getPolicy());
        builder.trustStore(trustStoreProvider.getTruststore());

        builder.keyStore(tlsClientCert, "");

        return builder.build();
    }
}

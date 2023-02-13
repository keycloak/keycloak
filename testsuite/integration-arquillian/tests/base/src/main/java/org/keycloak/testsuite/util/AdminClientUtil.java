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

package org.keycloak.testsuite.util;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.net.ssl.HostnameVerifier;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ClientHttpEngineBuilder43;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.models.Constants;

import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.utils.io.IOUtil.PROJECT_BUILD_DIRECTORY;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;


public class AdminClientUtil {

    public static final int NUMBER_OF_CONNECTIONS = 10;

    public static Keycloak createAdminClient(boolean ignoreUnknownProperties, String authServerContextRoot) throws Exception {
        return createAdminClient(ignoreUnknownProperties, authServerContextRoot, MASTER, ADMIN, ADMIN,
            Constants.ADMIN_CLI_CLIENT_ID, null, null);

    }

    public static Keycloak createAdminClient(boolean ignoreUnknownProperties, String realmName, String username,
        String password, String clientId, String clientSecret) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        return createAdminClient(ignoreUnknownProperties, getAuthServerContextRoot(), realmName, username, password,
            clientId, clientSecret, null);
    }

    public static Keycloak createAdminClient(boolean ignoreUnknownProperties, String authServerContextRoot, String realmName,
        String username, String password, String clientId, String clientSecret, String scope)
        throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {

        ResteasyClient resteasyClient = createResteasyClient(ignoreUnknownProperties, null);

        return KeycloakBuilder.builder()
                .serverUrl(authServerContextRoot + "/auth")
                .realm(realmName)
                .username(username)
                .password(password)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .resteasyClient(resteasyClient)
                .scope(scope).build();
    }

    public static Keycloak createAdminClientWithClientCredentials(String realmName, String clientId, String clientSecret, String scope)
        throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {

        boolean ignoreUnknownProperties = false;
        ResteasyClient resteasyClient = createResteasyClient(ignoreUnknownProperties, null);

        return KeycloakBuilder.builder()
                .serverUrl(getAuthServerContextRoot() + "/auth")
                .realm(realmName)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .resteasyClient(resteasyClient)
                .scope(scope).build();
    }

    public static Keycloak createAdminClient() throws Exception {
        return createAdminClient(false, getAuthServerContextRoot());
    }

    public static Keycloak createAdminClient(boolean ignoreUnknownProperties) throws Exception {
        return createAdminClient(ignoreUnknownProperties, getAuthServerContextRoot());
    }

    public static ResteasyClient createResteasyClient() {
        try {
            return createResteasyClient(false, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ResteasyClient createResteasyClient(boolean ignoreUnknownProperties, Boolean followRedirects) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ResteasyClientBuilder.newBuilder();

        if ("true".equals(System.getProperty("auth.server.ssl.required"))) {
            File trustore = new File(PROJECT_BUILD_DIRECTORY, "dependency/keystore/keycloak.truststore");
            resteasyClientBuilder.sslContext(getSSLContextWithTrustore(trustore, "secret"));

            System.setProperty("javax.net.ssl.trustStore", trustore.getAbsolutePath());
        }

        // We need to ignore unknown JSON properties e.g. in the adapter configuration representation
        // during adapter backward compatibility testing
        if (ignoreUnknownProperties) {
            // We need to use anonymous class to avoid the following error from RESTEasy:
            // Provider class org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider is already registered.  2nd registration is being ignored.
            ResteasyJackson2Provider jacksonProvider = new ResteasyJackson2Provider() {};
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            jacksonProvider.setMapper(objectMapper);
            resteasyClientBuilder.register(jacksonProvider, 100);
        }

        resteasyClientBuilder
                .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.WILDCARD)
                .connectionPoolSize(NUMBER_OF_CONNECTIONS)
                .httpEngine(getCustomClientHttpEngine(resteasyClientBuilder, 1, followRedirects));

        return resteasyClientBuilder.build();
    }

    private static SSLContext getSSLContextWithTrustore(File file, String password) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        if (!file.isFile()) {
            throw new RuntimeException("Truststore file not found: " + file.getAbsolutePath());
        }
        SSLContext theContext = SSLContexts.custom()
                .useProtocol("TLS")
                .loadTrustMaterial(file, password == null ? null : password.toCharArray())
                .build();
        return theContext;
    }

    public static ClientHttpEngine getCustomClientHttpEngine(ResteasyClientBuilder resteasyClientBuilder, int validateAfterInactivity, Boolean followRedirects) {
        return new CustomClientHttpEngineBuilder43(validateAfterInactivity, followRedirects).resteasyClientBuilder(resteasyClientBuilder).build();
    }

    /**
     * Adds a possibility to pass validateAfterInactivity parameter into underlying ConnectionManager. The parameter affects how
     * long the connection is being used without testing if it became stale, default value is 2000ms
     */
    private static class CustomClientHttpEngineBuilder43 extends ClientHttpEngineBuilder43 {

        private final int validateAfterInactivity;
        private final Boolean followRedirects;

        private CustomClientHttpEngineBuilder43(int validateAfterInactivity, Boolean followRedirects) {
            this.validateAfterInactivity = validateAfterInactivity;
            this.followRedirects = followRedirects;
        }

        @Override
        protected ClientHttpEngine createEngine(final HttpClientConnectionManager cm, final RequestConfig.Builder rcBuilder,
                final HttpHost defaultProxy, final int responseBufferSize, final HostnameVerifier verifier, final SSLContext theContext) {
            final ClientHttpEngine engine;
            if (cm instanceof PoolingHttpClientConnectionManager) {
                PoolingHttpClientConnectionManager pcm = (PoolingHttpClientConnectionManager) cm;
                pcm.setValidateAfterInactivity(validateAfterInactivity);
                engine = super.createEngine(pcm, rcBuilder, defaultProxy, responseBufferSize, verifier, theContext);
            } else {
                engine = super.createEngine(cm, rcBuilder, defaultProxy, responseBufferSize, verifier, theContext);
            }
            if (followRedirects != null) {
                engine.setFollowRedirects(followRedirects);
            }
            return engine;
        }
    }
   
}

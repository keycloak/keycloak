/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
import org.jboss.logging.Logger;
import org.keycloak.common.VerificationException;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ben Cresitello-Dittmar
 */
public class RESTAttributeStoreProviderConfig {
    private static final Logger logger = Logger.getLogger(RESTAttributeStoreProviderConfig.class);

    public enum HTTPMethodTypes { GET, POST }

    private String url;
    private  HTTPMethodTypes method;
    private KeyStore tlsClientCert;
    private Map<String, String> headers;

    /**
     * Get the URL to request attributes from
     * @return The URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the URL to request attributes from
     * @param url The URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get the HTTP method of the REST API request
     * @return The HTTP method
     */
    public HTTPMethodTypes getMethod() {
        return method;
    }

    /**
     * Set the HTTP method to use for the REST API request
     * @param method The HTTP method
     */
    public void setMethod(HTTPMethodTypes method) {
        this.method = method;
    }

    /**
     * Get the keypair used to perform mTLS authentication for the REST API request
     * @return The keypair
     */
    public KeyStore getTlsClientCert() {
        return tlsClientCert;
    }

    /**
     * Set the keypair used to perform mTLS authentication for the REST API request
     * @param tlsClientCert The keypair
     */
    public void setTlsClientCert(KeyStore tlsClientCert) {
        this.tlsClientCert = tlsClientCert;
    }

    /**
     * Get the headers to set on the REST API request
     * @return The headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Set the headers to set on the REST API request
     * @param headers The headers
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Helper function to parse the component model configuration
     * @param session The keycloak session
     * @param model The component model representing the {@link RESTAttributeStoreProvider} instance
     * @return the parsed configuration
     * @throws VerificationException thrown when the component model represents an invalid configuration
     */
    public static RESTAttributeStoreProviderConfig parse(KeycloakSession session, ComponentModel model) throws VerificationException{
        RealmModel realm = session.getContext().getRealm();
        RESTAttributeStoreProviderConfig parsed = new RESTAttributeStoreProviderConfig();

        String url = model.getConfig().getFirst(ProviderConfig.URL);
        if (url == null || url.isEmpty()){
            throw new VerificationException(String.format("%s must be set", ProviderConfig.URL));
        }
        parsed.setUrl(url);

        String method = model.getConfig().getFirst(ProviderConfig.METHOD);
        HTTPMethodTypes parsedMethod;
        if (method == null || method.isEmpty()){
            throw new VerificationException(String.format("%s must be set", ProviderConfig.METHOD));
        }
        try {
            parsedMethod = HTTPMethodTypes.valueOf(method);
        } catch (IllegalArgumentException e){
            throw new VerificationException(String.format("invalid HTTP method '%s'", method));
        }
        parsed.setMethod(parsedMethod);

        String headers = model.getConfig().getFirst(ProviderConfig.HEADERS);
        Map<String, String> parsedHeaders = new HashMap<>();
        if (headers != null) {
            try {
                List<Map<String, String>> val = JsonSerialization.readValue(headers, new TypeReference<List<Map<String, String>>>() {
                });
                val.forEach(e -> parsedHeaders.put(e.get("key"), e.get("value")));
            } catch (IOException e) {
                throw new VerificationException(String.format("invalid config value for '%s': %s", ProviderConfig.HEADERS, headers));
            }
        }
        parsed.setHeaders(parsedHeaders);

        String clientCertificate = model.getConfig().getFirst(ProviderConfig.CLIENT_CERTIFICATE);
        if (clientCertificate != null && !clientCertificate.isEmpty()){
            KeyStore keyStore = getTlsClientAuthKeystore(session, realm, clientCertificate);
            if (keyStore == null) {
                throw new VerificationException(String.format("unable to load key %s", clientCertificate));
            }
            parsed.setTlsClientCert(keyStore);
        }

        return parsed;
    }

    /**
     * Helper function to get a {@link KeyStore} instance for the specified key ID
     * @param session the keycloak session
     * @param realm the keycloak realm
     * @param id the id of the keycloak key
     * @return
     */
    private static KeyStore getTlsClientAuthKeystore(KeycloakSession session, RealmModel realm, String id){
        // fetch the configured key by KID
        Optional<KeyWrapper> authKey = session.keys().getKeysStream(realm)
                .filter(key -> key.getStatus().isActive() && key.getKid().equals(id))
                .findFirst();

        // verify key was found
        if (authKey.isEmpty()) {
            logger.warnf("key %s could not be found", id);
            return null;
        }

        if (authKey.get().getUse() != KeyUse.SIG){
            logger.warnf("key must be a signing key");
            return null;
        }


        X509Certificate[] clientCertChain;
        KeyStore keyStore;

        // get the certificate
        clientCertChain = new X509Certificate[] { authKey.get().getCertificate() };

        // initialize the keystore
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            // add the client cert and key to keystore, set password to blank password
            keyStore.setKeyEntry("client-cert", authKey.get().getPrivateKey(), "".toCharArray(), clientCertChain);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
            logger.warnf("failed to create keystore from key %s: %s", id, ex);
            return null;
        }

        return keyStore;
    }

    /**
     * Helper function to build the provider configuration
     * @return The provider configuration
     */
    public static List<ProviderConfigProperty> buildProviderConfig(){
        return ProviderConfigurationBuilder.create()
                .property().name("displayName")
                .label("Display Name")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The display name of this provider instance.")
                .required(true)
                .add()
                .property().name(ProviderConfig.URL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("URL")
                .helpText("The URL to request attributes from.")
                .required(true)
                .add()
                .property().name(ProviderConfig.METHOD)
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(Arrays.stream(HTTPMethodTypes.values()).map(Enum::name).collect(Collectors.toList()))
                .label("Method")
                .helpText("The http request method.")
                .required(true)
                .add()
                .property().name(ProviderConfig.CLIENT_CERTIFICATE)
                .type(ProviderConfigProperty.KEY_TYPE)
                .label("Client Certificate")
                .helpText("The client certificate to use for the SSL connection.")
                .required(false)
                .add()
                .property().name(ProviderConfig.HEADERS)
                .type(ProviderConfigProperty.MAP_TYPE)
                .label("Request Headers")
                .helpText("Headers to include in the request.")
                .defaultValue("[]")
                .required(false)
                .add()
                .build();
    }

    public static class ProviderConfig {
        /**
         * the url to request attributes from
         */
        public static final String URL = "url";

        /**
         * the HTTP method of the REST API request
         */
        public static final String METHOD = "method";

        /**
         * certificate to use for mTLS authentication
         */
        public static final String CLIENT_CERTIFICATE = "client-certificate";

        /**
         * the headers to set on the request
         */
        public static final String HEADERS = "headers";
    }
}

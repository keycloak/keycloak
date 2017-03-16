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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.util.BasicAuthHelper;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Configuration {

    @JsonIgnore
    private HttpClient httpClient;

    @JsonProperty("auth-server-url")
    protected String authServerUrl;

    @JsonProperty("no-check-certificate")
    protected Boolean noCertCheck;

    @JsonProperty("realm")
    protected String realm;

    @JsonProperty("resource")
    protected String clientId;

    @JsonProperty("credentials")
    protected Map<String, Object> clientCredentials = new HashMap<>();

    public Configuration() {

    }

    public Configuration(String authServerUrl, String realm, String clientId, Map<String, Object> clientCredentials, HttpClient httpClient) {
        this.authServerUrl = authServerUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientCredentials = clientCredentials;
        this.httpClient = httpClient;
    }

    @JsonIgnore
    private ClientAuthenticator clientAuthenticator = new ClientAuthenticator() {
        @Override
        public void configureClientCredentials(HashMap<String, String> requestParams, HashMap<String, String> requestHeaders) {
            String secret = (String) clientCredentials.get("secret");

            if (secret == null) {
                throw new RuntimeException("Client secret not provided.");
            }

            requestHeaders.put("Authorization", BasicAuthHelper.createHeader(clientId, secret));
        }
    };

    public HttpClient getHttpClient() {
        if (this.httpClient == null) {
            if(authServerUrl.startsWith("https") && noCertCheck) {
                try {
                    // Install a TrustManager that ignores certificate checks
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    TrustManager[] trustManagers = {new TrustAllManager()};
                    sslContext.init(null, trustManagers, null);
                    SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new AnyHostnameVerifier());
                    this.httpClient = HttpClients.custom()
                            .setSSLSocketFactory(sslConnectionSocketFactory)
                            .build();

                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create HttpsClient", e);
                }
            } else {
                this.httpClient = HttpClients.createDefault();
            }
        }

        return httpClient;
    }

    public Boolean getNoCertCheck() {
        return noCertCheck;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public ClientAuthenticator getClientAuthenticator() {
        return this.clientAuthenticator;
    }

    public Map<String, Object> getClientCredentials() {
        return clientCredentials;
    }

    public String getRealm() {
        return realm;
    }

    private static class TrustAllManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            //ignore
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            //ignore
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
    private static class AnyHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }
}

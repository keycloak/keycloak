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

package org.keycloak.client.registration.cli.util;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.client.registration.cli.common.EndpointType;
import org.keycloak.util.JsonSerialization;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class HttpUtil {

    public static final String APPLICATION_XML = "application/xml";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String UTF_8 = "utf-8";

    private static HttpClient httpClient;
    private static SSLConnectionSocketFactory sslsf;
    private static final AtomicBoolean tlsWarningEmitted = new AtomicBoolean();

    public static InputStream doGet(String url, String acceptType, String authorization) {
        try {
            HttpGet request = new HttpGet(url);
            request.setHeader(HttpHeaders.ACCEPT, acceptType);
            return doRequest(authorization, request);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send request - " + e.getMessage(), e);
        }
    }

    public static InputStream doPost(String url, String contentType, String acceptType, String content, String authorization) {
        try {
            return doPostOrPut(contentType, acceptType, content, authorization, new HttpPost(url));
        } catch (IOException e) {
            throw new RuntimeException("Failed to send request - " + e.getMessage(), e);
        }
    }

    public static InputStream doPut(String url, String contentType, String acceptType, String content, String authorization) {
        try {
            return doPostOrPut(contentType, acceptType, content, authorization, new HttpPut(url));
        } catch (IOException e) {
            throw new RuntimeException("Failed to send request - " + e.getMessage(), e);
        }
    }

    public static void doDelete(String url, String authorization) {
        try {
            HttpDelete request = new HttpDelete(url);
            doRequest(authorization, request);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send request - " + e.getMessage(), e);
        }
    }

    private static InputStream doPostOrPut(String contentType, String acceptType, String content, String authorization, HttpEntityEnclosingRequestBase request) throws IOException {
        request.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        request.setHeader(HttpHeaders.ACCEPT, acceptType);
        if (content != null) {
            request.setEntity(new StringEntity(content));
        }

        return doRequest(authorization, request);
    }

    private static InputStream doRequest(String authorization, HttpRequestBase request) throws IOException {
        addAuth(request, authorization);

        HttpResponse response = getHttpClient().execute(request);
        InputStream responseStream = null;
        if (response.getEntity() != null) {
            responseStream = response.getEntity().getContent();
        }

        int code = response.getStatusLine().getStatusCode();
        if (code >= 200 && code < 300) {
            return responseStream;
        } else {
            Map<String, String> error = null;
            try {
                Header header = response.getEntity().getContentType();
                if (header != null && APPLICATION_JSON.equals(header.getValue())) {
                    error = JsonSerialization.readValue(responseStream, Map.class);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to read error response - " + e.getMessage(), e);
            } finally {
                responseStream.close();
            }

            String message = null;
            if (error != null) {
                message = error.get("error_description") + " [" + error.get("error") + "]";
            }
            throw new RuntimeException(message != null ? message : response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
        }
    }

    private static void addAuth(HttpRequestBase request, String authorization) {
        if (authorization != null) {
            request.setHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            if (sslsf != null) {
                httpClient = HttpClientBuilder.create().useSystemProperties().setSSLSocketFactory(sslsf).build();
            } else {
                httpClient = HttpClientBuilder.create().useSystemProperties().build();
            }
        }
        return httpClient;
    }

    public static String getExpectedContentType(EndpointType type) {
        switch (type) {
            case DEFAULT:
            case OIDC:
                return APPLICATION_JSON;
            case SAML2:
                return APPLICATION_XML;
            default:
                throw new RuntimeException("Unsupported endpoint type: " + type);
        }
    }

    public static String urlencode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to urlencode", e);
        }
    }

    public static void setTruststore(File file, String password) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        if (!file.isFile()) {
            throw new RuntimeException("Truststore file not found: " + file.getAbsolutePath());
        }
        SSLContext theContext = SSLContexts.custom()
                .useProtocol("TLS")
                .loadTrustMaterial(file, password == null ? null : password.toCharArray(), TrustSelfSignedStrategy.INSTANCE)
                .build();
        sslsf = new SSLConnectionSocketFactory(theContext);
    }

    public static void setSkipCertificateValidation() {
        if (!tlsWarningEmitted.getAndSet(true)) {
            // Since this is a static util, it may happen that TLS is setup many times in one command
            // invocation (e.g. when a command requires logging in). However, we would like to
            // prevent this warning from appearing multiple times. That's why we need to guard it with a boolean.
            System.err.println("The server is configured to use TLS but there is no truststore specified.");
            System.err.println("The tool will skip certificate validation. This is highly discouraged for production use cases");
        }

        SSLContextBuilder builder = new SSLContextBuilder();
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslsf = new SSLConnectionSocketFactory(builder.build());
        } catch (Exception e) {
            throw new RuntimeException("Failed setting up TLS", e);
        }
    }
}

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

package org.keycloak.broker.provider.util;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author David Klassen (daviddd.kl@gmail.com)
 */
public class SimpleHttp {

    private String url;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> params;

    private SSLSocketFactory sslFactory;
    private HostnameVerifier hostnameVerifier;

    protected SimpleHttp(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public static SimpleHttp doGet(String url) {
        return new SimpleHttp(url, "GET");
    }

    public static SimpleHttp doPost(String url) {
        return new SimpleHttp(url, "POST");
    }

    public SimpleHttp header(String name, String value) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(name, value);
        return this;
    }

    public SimpleHttp param(String name, String value) {
        if (params == null) {
            params = new HashMap<String, String>();
        }
        params.put(name, value);
        return this;
    }

    public SimpleHttp sslFactory(SSLSocketFactory factory) {
        sslFactory = factory;
        return this;
    }

    public SimpleHttp hostnameVerifier(HostnameVerifier verifier) {
        hostnameVerifier = verifier;
        return this;
    }

    public String asString() throws IOException {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        setupTruststoreIfApplicable(httpClientBuilder);
        CloseableHttpClient httpClient = httpClientBuilder.build();

        CloseableHttpResponse response = makeRequest(httpClient);

        InputStream is;
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                is = entity.getContent();
                try {
                    HeaderIterator it = response.headerIterator();
                    while (it.hasNext()) {
                        Header header = it.nextHeader();
                        if (header.getName().equals("Content-Encoding") && header.getValue().equals("gzip")) {
                            is = new GZIPInputStream(is);
                        }
                    }
                    return toString(is);
                } finally {
                    is.close();
                }
            }
            return null;
        } finally {
            response.close();
            httpClient.close();
        }
    }

    public int asStatus() throws IOException {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        setupTruststoreIfApplicable(httpClientBuilder);
        CloseableHttpClient httpClient = httpClientBuilder.build();

        CloseableHttpResponse response = makeRequest(httpClient);

        try {
            StatusLine statusLine = response.getStatusLine();
            return statusLine.getStatusCode();
        } finally {
            response.close();
            httpClient.close();
        }
    }

    private String toString(InputStream is) throws IOException {
        InputStreamReader reader = new InputStreamReader(is);

        StringWriter writer = new StringWriter();

        char[] buffer = new char[1024 * 4];
        for (int n = reader.read(buffer); n != -1; n = reader.read(buffer)) {
            writer.write(buffer, 0, n);
        }

        return writer.toString();
    }

    private void setupTruststoreIfApplicable(HttpClientBuilder httpClientBuilder) {
        if (sslFactory != null) {
            org.apache.http.conn.ssl.SSLSocketFactory apacheSSLSocketFactory = new org.apache.http.conn.ssl.SSLSocketFactory(sslFactory, null);
            httpClientBuilder.setSSLSocketFactory(apacheSSLSocketFactory);
            if (hostnameVerifier != null) {
                httpClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
            }
        }
    }

    private URI generateURIfromURLandParameter(String url) {
        URI uri = null;

        try {
            URIBuilder uriBuilder = new URIBuilder(url);

            if (params != null) {
                for (Map.Entry<String, String> p : params.entrySet()) {
                    uriBuilder.setParameter(p.getKey(), p.getValue());
                }
            }

            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
        }

        return uri;
    }

    private CloseableHttpResponse makeRequest(CloseableHttpClient httpClient) {
        boolean get = method.equals("GET");

        URI uri = generateURIfromURLandParameter(url);

        HttpRequestBase httpRequest = new HttpPost(uri);
        if (get) {
            httpRequest = new HttpGet(uri);
        }

        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                httpRequest.setHeader(h.getKey(), h.getValue());
            }
        }

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpRequest);
        } catch (IOException e) {
        }
        return response;
    }
}

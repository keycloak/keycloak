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

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        HttpClient httpClient = httpClientBuilder.build();

        HttpResponse response = makeRequest(httpClient);

        InputStream is;
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
                if (is != null) {
                    is.close();
                }
            }
        }
        return null;
    }

    public int asStatus() throws IOException {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        setupTruststoreIfApplicable(httpClientBuilder);
        HttpClient httpClient = httpClientBuilder.build();

        HttpResponse response = makeRequest(httpClient);

        return response.getStatusLine().getStatusCode();
    }

    private HttpResponse makeRequest(HttpClient httpClient) throws IOException {
        boolean get = method.equals("GET");
        boolean post = method.equals("POST");

        HttpRequestBase httpRequest = new HttpPost(url);
        if (get) {
            httpRequest = new HttpGet(appendParameterToUrl(url));
        }

        if (post) {
            ((HttpPost) httpRequest).setEntity(getFormEntityFromParameter());
        }

        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                httpRequest.setHeader(h.getKey(), h.getValue());
            }
        }

        return httpClient.execute(httpRequest);
    }

    private void setupTruststoreIfApplicable(HttpClientBuilder httpClientBuilder) {
        if (sslFactory != null) {
            org.apache.http.conn.ssl.SSLSocketFactory apacheSSLSocketFactory =
                    new org.apache.http.conn.ssl.SSLSocketFactory(sslFactory, null);
            httpClientBuilder.setSSLSocketFactory(apacheSSLSocketFactory);
            if (hostnameVerifier != null) {
                httpClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
            }
        }
    }

    private URI appendParameterToUrl(String url) throws IOException {
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

    private UrlEncodedFormEntity getFormEntityFromParameter() throws IOException{
        List<NameValuePair> urlParameters = new ArrayList<>();

        if (params != null) {
            for (Map.Entry<String, String> p : params.entrySet()) {
                urlParameters.add(new BasicNameValuePair(p.getKey(), p.getValue()));
            }
        }

        return new UrlEncodedFormEntity(urlParameters);
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
}

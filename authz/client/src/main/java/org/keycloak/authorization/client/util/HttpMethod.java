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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.keycloak.authorization.client.Configuration;
import org.keycloak.protocol.oidc.client.authentication.ClientCredentialsProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class HttpMethod<R> {

    private static final Logger logger = Logger.getLogger(HttpMethod.class.getName());

    private final HttpClient httpClient;
    final RequestBuilder builder;
    final Configuration configuration;
    final Map<String, String> headers;
    final Map<String, List<String>> params;
    private final ClientCredentialsProvider authenticator;
    private HttpMethodResponse<R> response;

    public HttpMethod(Configuration configuration, ClientCredentialsProvider authenticator, RequestBuilder builder) {
        this(configuration, authenticator, builder, new HashMap<String, List<String>>(), new HashMap<String, String>());
    }

    public HttpMethod(Configuration configuration, ClientCredentialsProvider authenticator, RequestBuilder builder, Map<String, List<String>> params, Map<String, String> headers) {
        this.configuration = configuration;
        this.httpClient = configuration.getHttpClient();
        this.authenticator = authenticator;
        this.builder = builder;
        this.params = params;
        this.headers = headers;
    }

    public void execute() {
        execute(new HttpResponseProcessor<R>() {
            @Override
            public R process(byte[] entity) {
                return null;
            }
        });
    }

    public R execute(HttpResponseProcessor<R> responseProcessor) {
        byte[] bytes = null;

        try {
            for (Map.Entry<String, String> header : this.headers.entrySet()) {
                this.builder.setHeader(header.getKey(), header.getValue());
            }

            preExecute(this.builder);

            HttpResponse response = this.httpClient.execute(this.builder.build());
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                bytes = EntityUtils.toByteArray(entity);
            }

            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if(logger.isLoggable(Level.FINE)) {
                logger.fine( "Response from server: " + statusCode + " / " + statusLine.getReasonPhrase() +  " / Body : " + new String(bytes != null? bytes: new byte[0]));
            }

            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpResponseException("Unexpected response from server: " + statusCode + " / " + statusLine.getReasonPhrase(), statusCode, statusLine.getReasonPhrase(), bytes);
            }

            if (bytes == null) {
                return null;
            }

            return responseProcessor.process(bytes);
        } catch (HttpResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error executing http method [" + builder.getMethod() + "]. Response : " + String.valueOf(bytes), e);
        }
    }

    protected void preExecute(RequestBuilder builder) {
        for (Map.Entry<String, List<String>> param : params.entrySet()) {
            for (String value : param.getValue()) {
                builder.addParameter(param.getKey(), value);
            }
        }
    }

    public HttpMethod<R> authorizationBearer(String bearer) {
        this.builder.addHeader("Authorization", "Bearer " + bearer);
        return this;
    }

    public HttpMethodResponse<R> response() {
        this.response = new HttpMethodResponse(this);
        return this.response;
    }

    public HttpMethodAuthenticator<R> authentication() {
        return new HttpMethodAuthenticator<R>(this, authenticator);
    }

    public HttpMethod<R> param(String name, String value) {
        if (value != null) {
            List<String> values = params.get(name);

            if (values == null || !values.isEmpty()) {
                values = new ArrayList<>();
                params.put(name, values);
            }

            values.add(value);
        }
        return this;
    }

    public HttpMethod<R> params(String name, String value) {
        if (value != null) {
            List<String> values = params.get(name);

            if (values == null) {
                values = new ArrayList<>();
                params.put(name, values);
            }

            values.add(value);
        }
        return this;
    }

    public HttpMethod<R> json(byte[] entity) {
        this.builder.addHeader("Content-Type", "application/json");
        this.builder.setEntity(new ByteArrayEntity(entity));
        return this;
    }

    public HttpMethod<R> form() {
        return new HttpMethod<R>(this.configuration, authenticator, this.builder, this.params, this.headers) {
            @Override
            protected void preExecute(RequestBuilder builder) {
                if (params != null) {
                    List<NameValuePair> formparams = new ArrayList<>();

                    for (Map.Entry<String, List<String>> param : params.entrySet()) {
                        for (String value : param.getValue()) {
                            formparams.add(new BasicNameValuePair(param.getKey(), value));
                        }
                    }

                    builder.setEntity(new UrlEncodedFormEntity(formparams, StandardCharsets.UTF_8));
                }
            }
        };
    }
}

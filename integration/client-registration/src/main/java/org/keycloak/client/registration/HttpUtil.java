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

package org.keycloak.client.registration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.keycloak.common.util.StreamUtil;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
class HttpUtil {

    private HttpClient httpClient;

    private String baseUri;

    private Auth auth;

    HttpUtil(HttpClient httpClient, String baseUri) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
    }

    void setAuth(Auth auth) {
        this.auth = auth;
    }

    InputStream doPost(String content, String contentType, Charset charset, String acceptType, String... path) throws ClientRegistrationException {
        try {
            HttpPost request = new HttpPost(getUrl(baseUri, path));

            request.setHeader(HttpHeaders.CONTENT_TYPE, contentType(contentType, charset));
            request.setHeader(HttpHeaders.ACCEPT, acceptType);
            request.setEntity(new StringEntity(content, charset));

            addAuth(request);

            HttpResponse response = httpClient.execute(request);
            InputStream responseStream = null;
            if (response.getEntity() != null) {
                responseStream = response.getEntity().getContent();
            }

            if (response.getStatusLine().getStatusCode() == 201) {
                return responseStream;
            } else {
                throw httpErrorException(response, responseStream);
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }
    
    private String contentType(String contentType, Charset charset) {
    	return contentType + ";charset=" + charset.name();
    }

    InputStream doGet(String acceptType, String... path) throws ClientRegistrationException {
        try {
            HttpGet request = new HttpGet(getUrl(baseUri, path));

            request.setHeader(HttpHeaders.ACCEPT, acceptType);

            addAuth(request);

            HttpResponse response = httpClient.execute(request);
            InputStream responseStream = null;
            if (response.getEntity() != null) {
                responseStream = response.getEntity().getContent();
            }

            if (response.getStatusLine().getStatusCode() == 200) {
                return responseStream;
            } else if (response.getStatusLine().getStatusCode() == 404) {
                responseStream.close();
                return null;
            } else {
                throw httpErrorException(response, responseStream);
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    InputStream doPut(String content, String contentType, Charset charset, String acceptType, String... path) throws ClientRegistrationException {
        try {
            HttpPut request = new HttpPut(getUrl(baseUri, path));

            request.setHeader(HttpHeaders.CONTENT_TYPE, contentType(contentType, charset));
            request.setHeader(HttpHeaders.ACCEPT, acceptType);
            request.setEntity(new StringEntity(content, charset));

            addAuth(request);

            HttpResponse response = httpClient.execute(request);

            InputStream responseStream = null;
            if (response.getEntity() != null) {
                responseStream = response.getEntity().getContent();
            }

            if (response.getStatusLine().getStatusCode() == 200) {
                return responseStream;
            } else {
                throw httpErrorException(response, responseStream);
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    void doDelete(String... path) throws ClientRegistrationException {
        try {
            HttpDelete request = new HttpDelete(getUrl(baseUri, path));

            addAuth(request);

            HttpResponse response = httpClient.execute(request);
            InputStream responseStream = null;
            if (response.getEntity() != null) {
                responseStream = response.getEntity().getContent();
            }

            if (response.getStatusLine().getStatusCode() != 204) {
                throw httpErrorException(response, responseStream);
            }
        } catch (IOException e) {
            throw new ClientRegistrationException("Failed to send request", e);
        }
    }

    void close() throws ClientRegistrationException {
        if (httpClient instanceof CloseableHttpClient) {
            try {
                ((CloseableHttpClient) httpClient).close();
            } catch (IOException e) {
                throw new ClientRegistrationException("Failed to close http client", e);
            }
        }
    }

    static String getUrl(String baseUri, String... path) {
        StringBuilder s = new StringBuilder();
        s.append(baseUri);
        for (String p : path) {
            s.append('/');
            s.append(p);
        }
        return s.toString();
    }

    private void addAuth(HttpRequestBase request) {
        if (auth != null) {
            auth.addAuth(request);
        }
    }

    private HttpErrorException httpErrorException(HttpResponse response, InputStream responseStream) throws IOException {
        if (responseStream != null) {
            String errorResponse = StreamUtil.readString(responseStream);
            return new HttpErrorException(response.getStatusLine(), errorResponse);
        } else {
            return new HttpErrorException(response.getStatusLine(), null);
        }
    }

}

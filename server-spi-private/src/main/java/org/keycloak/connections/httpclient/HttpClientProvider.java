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

package org.keycloak.connections.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.keycloak.provider.Provider;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface HttpClientProvider extends Provider {
    /**
     * @deprecated Use {@link org.keycloak.http.simple.SimpleHttp#create(org.keycloak.models.KeycloakSession)} for
     * general HTTP calls, or the provider helper methods ({@link #getString}, {@link #getInputStream},
     * {@link #postText}, {@link #postBinary}) for simple operations. This method exposes an Apache HTTP Client type
     * and will be removed when the Apache dependency is dropped.
     */
    @Deprecated(since = "27.0", forRemoval = true)
    CloseableHttpClient getHttpClient();

    /**
     * Helper method
     *
     * @param uri
     * @param text
     * @return http response status
     * @throws IOException
     */
    public int postText(String uri, String text) throws IOException;

    /**
     * Helper method to retrieve the contents of a URL as a String.
     * Decoding response with the correct character set is performed according to the headers returned in the server's response.
     * To retrieve binary data, use {@link #getInputStream(String)}
     * 
     * Implementations should limit the amount of data returned to avoid an {@link OutOfMemoryError}.
     *
     * @param uri URI with data to receive.
     * @return Body of the response as a String.
     * @throws IOException On network errors, no content being returned or a non-2xx HTTP status code
     */
    String getString(String uri) throws IOException;

    /**
     * Helper method to retrieve the contents of a URL as an InputStream.
     * Use this to retrieve binary data where no additional HTTP headers need to be considered.
     * The caller is required to close the returned InputStream to prevent a resource leak.
     * <p>
     * To retrieve strings that depend on their encoding, use {@link #getString(String)}
     *
     * @param uri URI with data to receive.
     * @return Body of the response as an InputStream. The caller is required to close the returned InputStream to prevent a resource leak.
     * @throws IOException On network errors, no content being returned or a non-2xx HTTP status code.
     */
    InputStream getInputStream(String uri) throws IOException;

    /**
     * Retrieve the contents of a URL as an InputStream, sending additional HTTP headers with the request.
     * Use this when cache-control or other custom headers are required (e.g., CRL downloads).
     *
     * @param uri URI with data to receive.
     * @param headers additional HTTP headers to include in the request.
     * @return Body of the response as an InputStream. The caller is required to close it.
     * @throws IOException On network errors, no content being returned or a non-2xx HTTP status code.
     */
    default InputStream getInputStream(String uri, Map<String, String> headers) throws IOException {
        return getInputStream(uri);
    }

    /**
     * Send a binary POST request and return the response body as a byte array.
     * Use this for binary protocols (e.g., OCSP) where request and response are not text-based.
     *
     * @param uri URI to POST to.
     * @param body the binary request body.
     * @param headers HTTP headers to include (e.g., Content-Type, Accept).
     * @return response body as byte array.
     * @throws IOException On network errors or a non-2xx HTTP status code.
     */
    default byte[] postBinary(String uri, byte[] body, Map<String, String> headers) throws IOException {
        throw new IOException("postBinary not implemented by this provider");
    }

    /**
     * Helper method.
     * The caller is required to close the returned InputStream to prevent a resource leak.

     * @deprecated For String content, use  {@link #getString(String)}, for binary data use {@link #getInputStream(String)}.
     * To be removed in Keycloak 27.
     *
     * @param uri URI with data to receive.
     * @return Body of the response as an InputStream. The caller is required to close the returned InputStream to prevent a resource leak.
     * @throws IOException On network errors, no content being returned or a non-2xx HTTP status code.
     */
    @Deprecated
    default InputStream get(String uri) throws IOException {
        return getInputStream(uri);
    }

    long DEFAULT_MAX_CONSUMED_RESPONSE_SIZE = 10_000_000L;

    /**
     * Get the configured limit for the response size.
     *
     * @return number of bytes
     */
    default long getMaxConsumedResponseSize() {
        return DEFAULT_MAX_CONSUMED_RESPONSE_SIZE;
    }

}

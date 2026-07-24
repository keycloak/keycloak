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

import org.keycloak.provider.Provider;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface HttpClientProvider extends Provider {
    /**
     * Returns the {@code CloseableHttpClient} that can be freely used.
     * <p>
     * <b>The returned {@code HttpClient} instance must never be {@code close()}d by the caller.</b>
     * <p>
     * Closing the {@code HttpClient} instance is responsibility of this provider. However,
     * the objects created via the returned {@code HttpClient} need to be closed properly
     * by the code that instantiated them.
     * @return 
     */
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

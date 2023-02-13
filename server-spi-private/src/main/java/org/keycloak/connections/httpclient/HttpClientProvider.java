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

import org.keycloak.provider.Provider;

import java.io.IOException;
import java.io.InputStream;
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
     * Helper method
     *
     * @param uri
     * @return response stream, you must close this stream or leaks will happen
     * @throws IOException
     */
    public InputStream get(String uri) throws IOException;
}

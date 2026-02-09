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

package org.keycloak.connections.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

/**
 * Limit the amount of data read to prevent a {@link OutOfMemoryError}.
 *
 * @author Alexander Schwartz
 */
class SafeHttpEntity implements HttpEntity {

    private final HttpEntity delegate;
    private final long maxConsumedResponseSize;

    SafeHttpEntity(HttpEntity delegate, long maxConsumedResponseSize) {
        this.delegate = delegate;
        this.maxConsumedResponseSize = maxConsumedResponseSize;
    }

    @Override
    public boolean isRepeatable() {
        return delegate.isRepeatable();
    }

    @Override
    public boolean isChunked() {
        return delegate.isChunked();
    }

    @Override
    public long getContentLength() {
        return delegate.getContentLength();
    }

    @Override
    public Header getContentType() {
        return delegate.getContentType();
    }

    @Override
    public Header getContentEncoding() {
        return delegate.getContentEncoding();
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        return new SafeInputStream(delegate.getContent(), maxConsumedResponseSize);
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        delegate.writeTo(outputStream);
    }

    @Override
    public boolean isStreaming() {
        return delegate.isStreaming();
    }

    @Override
    @Deprecated
    public void consumeContent() throws IOException {
        delegate.consumeContent();
    }

}

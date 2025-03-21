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

/**
 * Limit the amount of data read to prevent a {@link OutOfMemoryError}.
 *
 * @author Alexander Schwartz
 */
public class SafeInputStream extends InputStream {

    private long bytesConsumed;
    private final InputStream delegate;
    private final long maxBytesToConsume;

    public SafeInputStream(InputStream delegate, long maxBytesToConsume) {
        this.delegate = delegate;
        this.maxBytesToConsume = maxBytesToConsume;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int sizeRead = delegate.read(b, off, len);
        if (sizeRead > 0) {
            bytesConsumed += sizeRead;
        }
        checkConsumedBytes();
        return sizeRead;
    }

    private void checkConsumedBytes() throws IOException {
        if (bytesConsumed > maxBytesToConsume) {
            throw new IOException(String.format("Response is at least %s bytes in size, with max bytes to be consumed being %d", bytesConsumed, maxBytesToConsume));
        }
    }

    @Override
    public int read() throws IOException {
        int result = delegate.read();
        if (result > 0) {
            ++bytesConsumed;
        }
        checkConsumedBytes();
        return result;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}

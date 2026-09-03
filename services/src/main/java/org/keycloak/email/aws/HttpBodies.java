/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.email.aws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a response body without letting the peer decide how much memory to take.
 * <p>
 * Shared by both transports rather than written twice: the bound is not about SES, whose answers are
 * a few hundred bytes, but about the credential endpoints — a peer that announces an enormous
 * {@code Content-Length}, or streams a body that never ends, would otherwise pin a request thread and
 * its heap inside a Keycloak transaction.
 */
final class HttpBodies {

    /** Four orders of magnitude above any answer these endpoints legitimately return. */
    static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    private HttpBodies() {
    }

    static byte[] readBounded(InputStream content) throws IOException {
        if (content == null) {
            return new byte[0];
        }
        try (InputStream in = content) {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                if (body.size() + read > MAX_RESPONSE_BYTES) {
                    throw new IOException("Response body exceeded " + MAX_RESPONSE_BYTES + " bytes");
                }
                body.write(chunk, 0, read);
            }
            return body.toByteArray();
        }
    }
}

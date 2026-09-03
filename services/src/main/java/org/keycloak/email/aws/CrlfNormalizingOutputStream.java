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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Rewrites bare {@code LF} into {@code CRLF} on the way out, leaving existing {@code CRLF} pairs
 * untouched.
 * <p>
 * This is not cosmetic. {@link jakarta.mail.internet.MimeMessage#writeTo} copies the bytes of a
 * {@code 7bit} body part verbatim, so the line endings of a Keycloak FreeMarker template survive as
 * bare LF. Over SMTP that never mattered, because the mail implementation wraps the socket in a
 * CRLF-translating stream before the message reaches the server; when the same bytes are base64'd
 * into an SES {@code Content.Raw} payload nothing performs that translation, and the result is a
 * message that violates RFC 5322 line-ending rules — with DKIM signing computed over it.
 */
final class CrlfNormalizingOutputStream extends FilterOutputStream {

    private int previousByte = -1;

    CrlfNormalizingOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        // The same translation the mail implementation applies on the SMTP socket: a bare CR, a bare
        // LF and a CRLF pair all end the line as CRLF. Reproducing it exactly is the point — the
        // bytes SES receives have to be the bytes the SMTP transport would have sent.
        if (b == '\r') {
            writeLineEnding();
        } else if (b == '\n') {
            if (previousByte != '\r') {
                writeLineEnding();
            }
        } else {
            out.write(b);
        }
        previousByte = b;
    }

    private void writeLineEnding() throws IOException {
        out.write('\r');
        out.write('\n');
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        // FilterOutputStream's inherited implementation is byte-at-a-time through write(int), which
        // is what this class needs; it is spelled out here so a future "optimisation" that forwards
        // the array straight to the delegate cannot silently bypass the translation.
        for (int i = 0; i < length; i++) {
            write(buffer[offset + i]);
        }
    }
}

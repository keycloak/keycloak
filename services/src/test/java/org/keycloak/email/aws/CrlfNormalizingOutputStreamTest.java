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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * The line-ending rewrite that stands between a Keycloak template and a DKIM-signed SES payload.
 * <p>
 * Every case here is one jakarta.mail actually produces while serialising a message: bare LFs inside
 * a {@code 7bit} body part, CRLF pairs in the headers it writes itself, and both arriving in
 * arbitrary chunks because the body is copied through a buffer whose boundaries nobody controls.
 * Getting any of them wrong corrupts the message in a way SES accepts and only the recipient sees.
 */
class CrlfNormalizingOutputStreamTest {

    @Test
    void turnsABareLineFeedIntoCrlf() throws Exception {
        assertThat(normalized("first\nsecond\n"), is("first\r\nsecond\r\n"));
    }

    /** Doubling the CR here would put a blank line between every pair of header lines. */
    @Test
    void leavesAnExistingCrlfPairUntouched() throws Exception {
        assertThat(normalized("Subject: hello\r\nTo: user@example.com\r\n"),
                is("Subject: hello\r\nTo: user@example.com\r\n"));
    }

    /**
     * A bare CR ends a line too, and becomes CRLF — which is what the mail implementation's own
     * CRLF stream does on the SMTP socket. Parity with that stream is the whole contract here: the
     * bytes handed to SES must be the bytes the SMTP transport would have put on the wire.
     */
    @Test
    void turnsABareCarriageReturnIntoCrlf() throws Exception {
        assertThat(normalized("before\rafter"), is("before\r\nafter"));
    }

    /**
     * The chunk boundary case. jakarta.mail streams a body through a buffer, so a CRLF pair can be
     * split across two writes; a stream that only looked at the current buffer would emit CRCRLF and
     * break every message longer than the buffer.
     */
    @Test
    void doesNotDoubleTheCarriageReturnWhenCrlfIsSplitAcrossTwoWrites() throws Exception {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (OutputStream out = new CrlfNormalizingOutputStream(sink)) {
            out.write("line\r".getBytes(StandardCharsets.UTF_8));
            out.write("\nnext".getBytes(StandardCharsets.UTF_8));
        }

        assertThat(sink.toString(StandardCharsets.UTF_8), is("line\r\nnext"));
    }

    /**
     * {@code FilterOutputStream} is free to forward an array write straight to the delegate, and a
     * subclass that let it would translate byte-at-a-time writes only — which is precisely the path
     * jakarta.mail does not take for message bodies.
     */
    @Test
    void translatesArrayWritesExactlyAsItTranslatesSingleBytes() throws Exception {
        String input = "a\nb\r\nc\rd\n\n";

        ByteArrayOutputStream oneByteAtATime = new ByteArrayOutputStream();
        try (OutputStream out = new CrlfNormalizingOutputStream(oneByteAtATime)) {
            for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
                out.write(b);
            }
        }

        assertThat(normalized(input), is("a\r\nb\r\nc\r\nd\r\n\r\n"));
        assertThat(oneByteAtATime.toString(StandardCharsets.UTF_8), is("a\r\nb\r\nc\r\nd\r\n\r\n"));
    }

    /** Only the requested window may be written: the surrounding bytes are another part's. */
    @Test
    void writesOnlyTheRequestedWindowOfTheArray() throws Exception {
        byte[] buffer = "XXline\nYY".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();

        try (OutputStream out = new CrlfNormalizingOutputStream(sink)) {
            out.write(buffer, 2, 5);
        }

        assertThat(sink.toString(StandardCharsets.UTF_8), is("line\r\n"));
    }

    @Test
    void writesNothingForEmptyInput() throws Exception {
        assertThat(normalized(""), is(""));
    }

    private static String normalized(String input) throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (OutputStream out = new CrlfNormalizingOutputStream(sink)) {
            out.write(input.getBytes(StandardCharsets.UTF_8));
        }
        return sink.toString(StandardCharsets.UTF_8);
    }
}

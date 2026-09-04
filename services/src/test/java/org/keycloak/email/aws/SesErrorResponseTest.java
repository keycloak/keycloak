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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * How an SES refusal is read off the wire.
 * <p>
 * SES states the same three facts — what it called the error, what it means, which request it was —
 * in several different places depending on the operation and on whatever sits between Keycloak and
 * AWS. Everything asserted here is a shape AWS actually sends; missing one of them turns an
 * actionable "Email address is not verified" into "Amazon SES rejected the message (HTTP 400)".
 */
class SesErrorResponseTest {

    @Test
    void readsTheErrorNameFromTheAmznErrorTypeHeader() {
        SesErrorResponse error = SesErrorResponse.parse(
                response(400, Map.of("x-amzn-ErrorType", "MessageRejected"), "{}"));

        assertThat(error.status(), is(400));
        assertThat(error.errorCode(), is("MessageRejected"));
    }

    /**
     * AWS decorates {@code x-amzn-ErrorType} two different ways — a trailing {@code :requestId} and a
     * leading {@code namespace#} — and which one arrives is not something the caller controls. An
     * unstripped value never matches a comparison such as the throttling check below.
     */
    @Test
    void stripsTheRequestIdAndTheNamespaceAwsDecoratesTheErrorTypeHeaderWith() {
        assertThat(SesErrorResponse.parse(response(400, Map.of("x-amzn-ErrorType", "MessageRejected:1234"), "{}"))
                .errorCode(), is("MessageRejected"));
        assertThat(SesErrorResponse.parse(
                response(429, Map.of("x-amzn-ErrorType", "com.amazonaws.ses#TooManyRequestsException"), "{}"))
                .errorCode(), is("TooManyRequestsException"));
    }

    /**
     * Without the header the name is in the body, under a field name that varies with the protocol
     * SES answered in: {@code __type} for JSON, {@code code}/{@code Code} for the query and REST-XML
     * shapes a proxy may still return.
     */
    @Test
    void fallsBackToTheErrorNameCarriedInTheBody() {
        assertThat(SesErrorResponse.parse(response(400, Map.of(), "{\"__type\":\"com.amazonaws.ses#MessageRejected\"}"))
                .errorCode(), is("MessageRejected"));
        assertThat(SesErrorResponse.parse(response(400, Map.of(), "{\"code\":\"AccountSuspendedException\"}"))
                .errorCode(), is("AccountSuspendedException"));
        assertThat(SesErrorResponse.parse(response(400, Map.of(), "{\"Code\":\"SendingPausedException\"}"))
                .errorCode(), is("SendingPausedException"));
    }

    /**
     * SES writes the human text under {@code message} for some operations and {@code Message} for
     * others. Reading a single casing yields an empty error text exactly when an administrator needs
     * it, since the sentence AWS puts there is usually the whole diagnosis.
     */
    @Test
    void readsTheHumanMessageInEitherCasing() {
        assertThat(SesErrorResponse.parse(response(400, Map.of(), "{\"message\":\"Email address is not verified\"}"))
                .message(), is("Email address is not verified"));
        assertThat(SesErrorResponse.parse(response(400, Map.of(), "{\"Message\":\"Email address is not verified\"}"))
                .message(), is("Email address is not verified"));
    }

    /** The request id is the only handle AWS support can follow, and AWS does not send it in a fixed casing. */
    @Test
    void findsTheRequestIdWhateverCasingAwsSendsItIn() {
        SesErrorResponse error = SesErrorResponse.parse(
                response(400, Map.of("X-Amzn-Requestid", "7b0c2f4e-2f1a-4c86-9f4d-1a2b3c4d5e6f"), "{}"));

        assertThat(error.requestId(), is("7b0c2f4e-2f1a-4c86-9f4d-1a2b3c4d5e6f"));
    }

    /**
     * A load balancer or a corporate proxy in front of SES answers in HTML, not JSON. Parsing must
     * not throw there: the status and the request id are still the diagnosis, and an exception raised
     * while reporting an error would replace it with an unrelated stack trace.
     */
    @Test
    void survivesAnHtmlErrorPageFromAProxy() {
        SesErrorResponse error = SesErrorResponse.parse(response(503,
                Map.of("x-amzn-RequestId", "proxy-1"),
                "<html><head><title>503 Service Unavailable</title></head><body></body></html>"));

        assertThat(error.status(), is(503));
        assertThat(error.errorCode(), is(nullValue()));
        assertThat(error.message(), is(nullValue()));
        assertThat(error.requestId(), is("proxy-1"));
        assertThat(error.describeForLog(), is("status=503 error=unknown awsRequestId=proxy-1"));
    }

    /**
     * Throttling has to be recognised by error name, not by status: SES returns
     * {@code LimitExceededException} with HTTP 400, so classifying on the status alone tells an
     * administrator to fix a configuration when the account has simply hit its sending quota.
     */
    @Test
    void classifiesThrottlingByErrorNameAndNotOnlyByStatus() {
        assertThat(throttling(429, "{}"), is(true));
        assertThat(throttling(400, "{\"__type\":\"TooManyRequestsException\"}"), is(true));
        assertThat(throttling(400, "{\"__type\":\"ThrottlingException\"}"), is(true));
        assertThat(throttling(400, "{\"__type\":\"LimitExceededException\"}"), is(true));
        assertThat(throttling(400, "{\"__type\":\"MessageRejected\"}"), is(false));
    }

    /** The admin-facing string: everything needed to act, including the id to quote to AWS support. */
    @Test
    void describeCarriesStatusErrorNameMessageAndRequestId() {
        SesErrorResponse error = SesErrorResponse.parse(response(400,
                Map.of("x-amzn-ErrorType", "MessageRejected:9f2", "x-amzn-RequestId", "1a2b3c"),
                "{\"message\":\"Email address is not verified in region EU-CENTRAL-1\"}"));

        String description = error.describe();

        assertThat(description, containsString("400"));
        assertThat(description, containsString("MessageRejected"));
        assertThat(description, containsString("Email address is not verified in region EU-CENTRAL-1"));
        assertThat(description, containsString("1a2b3c"));
    }

    /**
     * The log line deliberately drops the human message: AWS quotes the rejected address in it, and
     * the server log is not where a user's email address should end up. Status, error name and
     * request id are what a log reader needs anyway.
     */
    @Test
    void describeForLogLeavesTheHumanMessageOut() {
        SesErrorResponse error = SesErrorResponse.parse(response(400,
                Map.of("x-amzn-ErrorType", "MessageRejected", "x-amzn-RequestId", "1a2b3c"),
                "{\"message\":\"Email address is not verified: alice@example.com\"}"));

        String logLine = error.describeForLog();

        assertThat(logLine, is("status=400 error=MessageRejected awsRequestId=1a2b3c"));
        assertThat(logLine, not(containsString("alice@example.com")));
    }

    private static boolean throttling(int status, String body) {
        return SesErrorResponse.parse(response(status, Map.of(), body)).isThrottling();
    }

    /**
     * AWS names the recipient in its rejection text, and this message does not stay in the
     * administrator's browser: the callers of the email SPI log the whole exception, so the address
     * would land in the server log on every failed send.
     */
    @Test
    void redactsAddressesFromTheAwsText() {
        SesErrorResponse error = SesErrorResponse.parse(new AwsHttpResponse(400,
                Map.of("x-amzn-ErrorType", "MessageRejected", "x-amzn-RequestId", "req-42"),
                ("{\"message\":\"Email address is not verified. The following identities failed the check"
                        + " in region EU-CENTRAL-1: utente@example.com\"}").getBytes(StandardCharsets.UTF_8)));

        String described = error.describe();

        assertThat(described, not(containsString("utente@example.com")));
        assertThat(described, containsString("<address redacted>"));
        assertThat(described, containsString("Email address is not verified"));
        assertThat(described, containsString("MessageRejected"));
        assertThat(described, containsString("req-42"));
    }

    private static AwsHttpResponse response(int status, Map<String, String> headers, String body) {
        return new AwsHttpResponse(status, headers, body.getBytes(StandardCharsets.UTF_8));
    }
}

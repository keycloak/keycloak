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

import java.io.IOException;
import java.util.regex.Pattern;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * An error answer from SES, reduced to the three things worth acting on: what AWS called it, what it
 * said, and the request id AWS support needs to trace it.
 *
 * @param status    HTTP status code
 * @param errorCode the AWS exception name ({@code MessageRejected}, {@code TooManyRequestsException}…),
 *                  or {@code null} when the response carried none
 * @param message   the human-readable text AWS returned, or {@code null}
 * @param requestId the value of {@code x-amzn-RequestId} — the only handle AWS support can follow
 */
record SesErrorResponse(int status, String errorCode, String message, String requestId) {

    private static final Pattern ADDRESS = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    /**
     * Extracts the error from a non-2xx response.
     * <p>
     * Three quirks of the SES wire format are handled here rather than at the call site. The
     * exception name arrives in the {@code x-amzn-ErrorType} header, decorated: AWS appends
     * {@code :} plus a request id, or prefixes a {@code #}-separated namespace, and both have to be
     * stripped. When the header is absent the same name may appear in the body as {@code __type},
     * {@code code} or {@code Code}. And the message itself is under {@code message} <em>or</em>
     * {@code Message} depending on the operation — reading only one casing yields empty error text
     * exactly when an operator most needs it.
     */
    static SesErrorResponse parse(AwsHttpResponse response) {
        String errorCode = sanitizeErrorType(response.header("x-amzn-ErrorType"));
        String message = null;

        if (response.body().length > 0) {
            try {
                // A null document is defensive rather than reachable — Jackson answers an empty body
                // with MissingNode — and lands in the same place as a body that is not JSON at all:
                // the status and the request id still carry the diagnosis.
                JsonNode body = JsonSerialization.mapper.readTree(response.body());
                if (body != null) {
                    if (errorCode == null) {
                        errorCode = sanitizeErrorType(firstText(body, "__type", "code", "Code"));
                    }
                    message = firstText(body, "message", "Message");
                }
            } catch (IOException e) {
                // A body that is not JSON is itself unremarkable — a proxy or a load balancer in
                // front of SES answers in HTML. The status code and the request id still carry the
                // diagnosis, so this is not worth failing over.
                message = null;
            }
        }
        return new SesErrorResponse(response.status(), errorCode, message, response.header("x-amzn-RequestId"));
    }

    private static String firstText(JsonNode body, String... fields) {
        for (String field : fields) {
            JsonNode value = body.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static String sanitizeErrorType(String errorType) {
        if (errorType == null || errorType.isBlank()) {
            return null;
        }
        String sanitized = errorType;
        int colon = sanitized.indexOf(':');
        if (colon >= 0) {
            sanitized = sanitized.substring(0, colon);
        }
        int hash = sanitized.lastIndexOf('#');
        if (hash >= 0) {
            sanitized = sanitized.substring(hash + 1);
        }
        return sanitized.isBlank() ? null : sanitized.trim();
    }

    /**
     * Whether SES is asking for the send to slow down rather than refusing it.
     * <p>
     * Nothing retries on it — see {@code AwsSesEmailSenderProvider} for why — but the distinction is
     * what tells an administrator staring at a failed invitation whether to fix a configuration or
     * to ask AWS for a higher sending rate.
     */
    boolean isThrottling() {
        return status == 429
                || "TooManyRequestsException".equals(errorCode)
                || "ThrottlingException".equals(errorCode)
                || "LimitExceededException".equals(errorCode);
    }

    /** Admin-facing summary. Carries no credential material: AWS error text never contains any. */
    String describe() {
        StringBuilder description = new StringBuilder("Amazon SES rejected the message (HTTP ").append(status);
        if (errorCode != null) {
            description.append(", ").append(errorCode);
        }
        description.append(')');
        if (message != null) {
            description.append(": ").append(redactAddresses(message));
        }
        if (isThrottling()) {
            description.append(" [the account's SES sending rate or quota was exceeded]");
        }
        if (requestId != null) {
            description.append(" [aws-request-id: ").append(requestId).append(']');
        }
        return description.toString();
    }

    /**
     * Removes anything address-shaped from AWS's text.
     * <p>
     * The text itself is what makes a failure actionable — "Email address is not verified" tells an
     * administrator exactly what to fix — but it names the recipient, and this message does not stay
     * in the administrator's browser: the callers of the email SPI log the whole exception, so
     * without this the address ends up in the server log for every failed send.
     */
    private static String redactAddresses(String message) {
        return ADDRESS.matcher(message).replaceAll("<address redacted>");
    }

    /** Log-facing summary: status, error name and request id only — never the message, which can name a recipient. */
    String describeForLog() {
        return "status=" + status + " error=" + (errorCode == null ? "unknown" : errorCode)
                + " awsRequestId=" + (requestId == null ? "none" : requestId);
    }
}

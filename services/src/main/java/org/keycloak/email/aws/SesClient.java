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
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.email.EmailException;
import org.keycloak.email.aws.credentials.AwsCredentials;
import org.keycloak.email.aws.credentials.AwsCredentialsException;
import org.keycloak.email.aws.credentials.AwsCredentialsProviderChain;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * The SES v2 {@code SendEmail} call: resolve credentials, build the JSON body, sign it, post it,
 * turn the answer into a message id or an {@link EmailException}.
 * <p>
 * The message is always sent as {@code Content.Raw}. SES could compose the MIME itself from a
 * {@code Content.Simple} subject and body, but then the message a recipient receives would be
 * SES's rendering rather than Keycloak's, and every difference from the SMTP sender — header order,
 * encodings, the {@code Reply-To} Keycloak sets — would be invisible until someone noticed it in
 * production. Sending the same bytes Keycloak's SMTP transport would have sent keeps the two
 * transports interchangeable.
 */
final class SesClient {

    private static final Logger logger = Logger.getLogger(SesClient.class);

    private final AwsSesConfig config;
    private final AwsCredentialsProviderChain credentialsChain;
    private final AwsHttpTransport directTransport;
    private final AwsV4Signer signer;

    SesClient(AwsSesConfig config, AwsCredentialsProviderChain credentialsChain) {
        this(config, credentialsChain, new DirectHttpTransport());
    }

    SesClient(AwsSesConfig config, AwsCredentialsProviderChain credentialsChain, AwsHttpTransport directTransport) {
        this.config = config;
        this.credentialsChain = credentialsChain;
        this.directTransport = directTransport;
        this.signer = new AwsV4Signer(config.region(), AwsSesConfig.SIGNING_SERVICE);
    }

    /**
     * @param feedbackForwardingAddress the realm's {@code envelopeFrom}, or {@code null}
     * @return the SES message id — an acknowledgement that SES accepted the message for sending,
     *         which is not the same thing as delivery
     */
    String sendEmail(AwsHttpTransport transport, SesRawMessage message, String feedbackForwardingAddress,
                     int connectTimeoutMillis, int readTimeoutMillis, Instant now) throws EmailException {
        byte[] body = requestBody(message, feedbackForwardingAddress);

        AwsCredentials credentials;
        try {
            credentials = credentialsChain.resolve(transport, directTransport, now);
        } catch (AwsCredentialsException e) {
            throw new EmailException(e.getMessage(), e);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", config.hostHeader());
        headers.put("Content-Type", "application/json");

        AwsHttpRequest request = new AwsHttpRequest("POST", config.sendEmailUri(), headers, body,
                connectTimeoutMillis, readTimeoutMillis);
        request = request.withHeaders(signer.sign(request, credentials, now));

        AwsHttpResponse response;
        try {
            response = transport.exchange(request);
        } catch (IOException e) {
            // Deliberately not retried: SES SendEmail has no idempotency token, so a request that
            // failed after reaching AWS cannot be distinguished from one that never arrived, and
            // resending it would deliver the same activation email twice. Keycloak's SMTP sender
            // does not retry either.
            throw new EmailException("Could not reach Amazon SES at " + config.hostHeader() + ": " + e.getMessage(), e);
        }

        if (!response.isSuccessful()) {
            SesErrorResponse error = SesErrorResponse.parse(response);
            logger.warnf("Amazon SES refused an email: %s", error.describeForLog());
            throw new EmailException(error.describe());
        }
        return messageId(response);
    }

    private byte[] requestBody(SesRawMessage message, String feedbackForwardingAddress) throws EmailException {
        Map<String, Object> destination = new LinkedHashMap<>();
        destination.put("ToAddresses", List.of(message.recipient()));

        Map<String, Object> raw = new LinkedHashMap<>();
        // Base64 is the caller's job on the plain HTTPS API — only the AWS SDKs encode it for you.
        raw.put("Data", Base64.getEncoder().encodeToString(message.mime()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("FromEmailAddress", message.fromHeaderValue());
        payload.put("Destination", destination);
        payload.put("Content", Map.of("Raw", raw));
        if (feedbackForwardingAddress != null && !feedbackForwardingAddress.isBlank()) {
            // The realm's envelopeFrom is Keycloak's bounce address, and this is the SES parameter
            // with that meaning. Note the mapping is partial: SES fixes the SMTP envelope sender to
            // the identity's MAIL FROM domain, so the SPF-alignment half of envelopeFrom cannot be
            // honoured over the API — only the bounce routing.
            payload.put("FeedbackForwardingEmailAddress", feedbackForwardingAddress);
        }
        if (config.configurationSetName() != null) {
            payload.put("ConfigurationSetName", config.configurationSetName());
        }
        // ReplyToAddresses is deliberately absent: Keycloak already writes a Reply-To header into the
        // MIME, and AWS documents no interaction between the parameter and a raw message's headers.

        try {
            return JsonSerialization.writeValueAsBytes(payload);
        } catch (IOException e) {
            throw new EmailException("Failed to serialise the Amazon SES request", e);
        }
    }

    private String messageId(AwsHttpResponse response) throws EmailException {
        try {
            JsonNode body = JsonSerialization.mapper.readTree(response.body());
            // The null check is defensive rather than reachable: Jackson answers an empty document
            // with MissingNode, not null. It is here so this class does not depend on that.
            String messageId = body == null ? null : body.path("MessageId").asText(null);
            if (messageId == null || messageId.isBlank()) {
                throw new EmailException("Amazon SES accepted the message but returned no message id");
            }
            return messageId;
        } catch (IOException e) {
            throw new EmailException("Could not read the Amazon SES response", e);
        }
    }
}

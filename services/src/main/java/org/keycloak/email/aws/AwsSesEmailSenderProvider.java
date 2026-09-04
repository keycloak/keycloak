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

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;

import static org.keycloak.utils.StringUtil.isNotBlank;

/**
 * Sends Keycloak's transactional email through the Amazon SES v2 API, authenticated with AWS SigV4.
 * <p>
 * The reason this exists is authentication, not transport. Keycloak's SMTP sender can already reach
 * SES — through the SMTP interface, which requires a dedicated, long-lived SMTP username and password
 * derived from an IAM user's secret key. That credential cannot be an assumed role, cannot be rotated
 * by AWS, and has to be stored somewhere in the identity provider that is meant to hold no static
 * secrets. Signing the API call instead lets the server use whatever credential its platform already
 * gives it: an EKS service account, an ECS task role, an EC2 instance profile, or — where there is no
 * role to assume — an IAM user's key pair supplied through the standard AWS environment variables.
 * <p>
 * Everything a recipient can observe is unchanged from the SMTP sender: the same
 * {@code multipart/alternative} message, the same headers, built from the same realm SMTP settings.
 * Only the transport differs.
 */
public class AwsSesEmailSenderProvider implements EmailSenderProvider {

    private static final Logger logger = Logger.getLogger(AwsSesEmailSenderProvider.class);

    /**
     * {@code envelopeFrom} maps only partially onto SES; warning about it on every send would be
     * noise, and warning never would leave the gap undiscovered, so it is said once per server.
     */
    private static final AtomicBoolean ENVELOPE_FROM_WARNING_EMITTED = new AtomicBoolean();

    private final AwsSesConfig sesConfig;
    private final SesClient client;
    private final AwsHttpTransport transport;
    private final Clock clock;

    AwsSesEmailSenderProvider(AwsSesConfig sesConfig, SesClient client, AwsHttpTransport transport, Clock clock) {
        this.sesConfig = sesConfig;
        this.client = client;
        this.transport = transport;
        this.clock = clock;
    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody, String htmlBody)
            throws EmailException {
        String address = user.getEmail();
        if (address == null) {
            throw new EmailException("No email address configured for the user");
        }
        send(config, address, subject, textBody, htmlBody);
    }

    @Override
    public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody)
            throws EmailException {
        SesRawMessage message = SesRawMessage.compose(config, address, subject, textBody, htmlBody);

        String messageId = client.sendEmail(transport, message, feedbackForwardingAddress(config),
                timeout(config.get("connectionTimeout"), sesConfig.connectTimeoutMillis()),
                timeout(config.get("timeout"), sesConfig.readTimeoutMillis()),
                clock.instant());

        // No recipient address in the log line: a Keycloak server logs at DEBUG in plenty of
        // deployments, and who was sent a password reset is exactly the sort of thing that should not
        // accumulate there. "Accepted", not "delivered" — SES can accept a message it never delivers.
        logger.debugf("Amazon SES accepted an email for delivery (messageId=%s)", messageId);
    }

    /**
     * Static validation of the realm's email settings, called by newer servers when an administrator
     * saves or imports a realm.
     * <p>
     * Declared without {@code @Override} on purpose. {@code EmailSenderProvider.validate} was added
     * to the SPI after 26.0 and — because it was backported — is present in 26.2.8+ and 26.3.3+ but
     * absent from 26.0.x, 26.1.x and 26.3.0–26.3.2. Annotating it would make this class refuse to
     * compile against the older SPI; declaring it plainly satisfies the newer servers, which call it,
     * and is dead weight on the older ones, which do not. A class that omitted it would instead fail
     * with an {@code AbstractMethodError} the first time an administrator saved a realm.
     * <p>
     * Only addresses are checked, and nothing here touches the network: this runs on the realm-update
     * request path, and an SES deployment legitimately has no host, port, user or password to verify.
     */
    public void validate(Map<String, String> config) throws EmailException {
        SesRawMessage.requireSendableAddress(config.get("from"), "sender");
        if (isNotBlank(config.get("replyTo"))) {
            SesRawMessage.requireSendableAddress(config.get("replyTo"), "reply-to");
        }
        if (isNotBlank(config.get("envelopeFrom"))) {
            SesRawMessage.requireSendableAddress(config.get("envelopeFrom"), "envelope-from");
        }
    }

    @Override
    public void close() {
        // The HTTP client belongs to Keycloak's HttpClientProvider and must outlive this provider.
    }

    private String feedbackForwardingAddress(Map<String, String> config) throws EmailException {
        String envelopeFrom = config.get("envelopeFrom");
        if (!isNotBlank(envelopeFrom)) {
            return null;
        }
        // Validated and converted like every other address rather than passed through: validate() is
        // not called at all on servers older than 26.2.8, so this is the only check an internationalised
        // or malformed envelope-from gets before it reaches AWS.
        String sendable = SesRawMessage.requireSendableAddress(envelopeFrom, "envelope-from");
        if (ENVELOPE_FROM_WARNING_EMITTED.compareAndSet(false, true)) {
            logger.warn("The realm sets an envelope-from address, which Amazon SES honours only for bounce and"
                    + " complaint routing: the SMTP envelope sender is fixed by the sending identity's MAIL FROM"
                    + " domain, so SPF alignment cannot be driven from the realm over the SES API. The address must"
                    + " itself be a verified SES identity or SES will refuse the message.");
        }
        return sendable;
    }

    /**
     * Realm-level timeouts win over the server-wide ones, mirroring the SMTP sender, so an
     * administrator who has already tuned the realm's mail timeouts keeps them after the switch.
     */
    private static int timeout(String realmValue, int fallback) {
        if (realmValue == null || realmValue.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(realmValue.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

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
import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;

import org.keycloak.email.EmailException;
import org.keycloak.utils.EmailValidationUtil;

import static org.keycloak.utils.StringUtil.isNotBlank;

/**
 * The MIME message handed to SES as {@code Content.Raw}, together with the two envelope values that
 * must agree with its headers.
 * <p>
 * Everything about the message construction mirrors Keycloak's SMTP sender
 * ({@code DefaultEmailSenderProvider}) deliberately: same {@code multipart/alternative} with the
 * text part first, same RFC 2047 subject encoding, same rule that {@code Reply-To} defaults to the
 * {@code From} address, same raw {@code To} header. Switching a realm from SMTP to SES must change
 * the transport and nothing a recipient can see.
 */
final class SesRawMessage {

    private final InternetAddress from;
    private final String recipient;
    private final byte[] mime;

    private SesRawMessage(InternetAddress from, String recipient, byte[] mime) {
        this.from = from;
        this.recipient = recipient;
        this.mime = mime;
    }

    /**
     * The exact string used as the MIME {@code From} header, to be sent as SES's
     * {@code FromEmailAddress}.
     * <p>
     * Both come from this one object rather than from two separate renderings of the configuration:
     * AWS documents the parameter as the envelope sender and the header as the message header, but
     * never documents what happens when the two disagree — nor which identity IAM authorises then.
     * Deriving both from a single {@link InternetAddress} removes the question instead of betting on
     * an answer.
     */
    String fromHeaderValue() {
        return from.toString();
    }

    /** The single envelope recipient, identical to the {@code To} header. */
    String recipient() {
        return recipient;
    }

    byte[] mime() {
        return mime;
    }

    static SesRawMessage compose(Map<String, String> config, String address, String subject,
                                 String textBody, String htmlBody) throws EmailException {
        String fromAddress = requireSendableAddress(config.get("from"), "sender");
        String recipient = requireSendableAddress(address, "recipient");

        Properties properties = new Properties();
        // Without mail.from, saveChanges() builds the Message-ID from
        // InetAddress.getLocalHost().getHostName(): a blocking reverse lookup on every email whose
        // result — the container's internal hostname — then travels to the recipient. SES replaces
        // the Message-ID anyway, so there is nothing to lose and a hostname leak to avoid.
        properties.setProperty("mail.from", fromAddress);
        Session session = Session.getInstance(properties);

        try {
            InternetAddress from = toInternetAddress(fromAddress, config.get("fromDisplayName"));

            MimeMessage message = new MimeMessage(session);
            message.setFrom(from);
            message.setReplyTo(new Address[]{from});

            String replyTo = config.get("replyTo");
            if (isNotBlank(replyTo)) {
                String replyToAddress = requireSendableAddress(replyTo, "reply-to");
                message.setReplyTo(new Address[]{toInternetAddress(replyToAddress, config.get("replyToDisplayName"))});
            }

            message.setHeader("To", recipient);
            message.setSubject(MimeUtility.encodeText(subject, StandardCharsets.UTF_8.name(), null));
            message.setContent(multipartBody(textBody, htmlBody));
            message.saveChanges();
            message.setSentDate(new Date());

            return new SesRawMessage(from, recipient, serialize(message));
        } catch (UnsupportedEncodingException e) {
            throw new EmailException("Failed to encode email address", e);
        } catch (AddressException e) {
            throw new EmailException("Invalid email address format", e);
        } catch (MessagingException | IOException e) {
            throw new EmailException("Failed to compose the email message", e);
        }
    }

    private static byte[] serialize(Message message) throws IOException, MessagingException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (OutputStream out = new CrlfNormalizingOutputStream(buffer)) {
            message.writeTo(out);
        }
        return buffer.toByteArray();
    }

    private static Multipart multipartBody(String textBody, String htmlBody) throws MessagingException {
        Multipart multipart = new MimeMultipart("alternative");
        if (textBody != null) {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(textBody, StandardCharsets.UTF_8.name());
            multipart.addBodyPart(textPart);
        }
        if (htmlBody != null) {
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);
        }
        return multipart;
    }

    private static InternetAddress toInternetAddress(String email, String displayName)
            throws UnsupportedEncodingException, AddressException {
        if (displayName == null || displayName.trim().isEmpty()) {
            return new InternetAddress(email);
        }
        return new InternetAddress(email, displayName, StandardCharsets.UTF_8.name());
    }

    /**
     * Renders a rejected address for an error message.
     * <p>
     * The address is only ever quoted back when it has already failed validation, which is exactly
     * the case where it may contain line breaks: this message reaches the server log, and a raw CR or
     * LF in it would let the offending value forge a log line of its own.
     */
    private static String forMessage(String address) {
        String sanitized = address.length() > 120 ? address.substring(0, 120) + "…" : address;
        return sanitized.replaceAll("[\\p{Cntrl}]", "?");
    }

    /**
     * Validates an address and returns the form that can actually be sent.
     * <p>
     * An internationalised domain is converted to its ASCII (punycode) equivalent, which is what
     * Keycloak's own SMTP sender does — {@code utente@società.it} is delivered as
     * {@code utente@xn--societ-nta.it} rather than rejected. A non-ASCII <em>local</em> part cannot be
     * rescued that way: it needs the SMTPUTF8 extension, which Amazon SES does not implement ("the
     * email address string must be 7-bit ASCII"), so it is refused here with a message that says why
     * instead of coming back later as an opaque {@code MessageRejected}.
     */
    static String requireSendableAddress(String address, String role) throws EmailException {
        if (address == null || address.isBlank()) {
            throw new EmailException("No " + role + " address configured");
        }
        if (!EmailValidationUtil.isValidEmail(address)) {
            throw new EmailException("Invalid " + role + " address '" + forMessage(address) + "'");
        }
        String asciiAddress = toAsciiDomain(address);
        if (asciiAddress == null || !StandardCharsets.US_ASCII.newEncoder().canEncode(asciiAddress)) {
            throw new EmailException("Amazon SES does not support internationalised (non 7-bit ASCII) "
                    + role + " addresses, but '" + forMessage(address) + "' has a local part or a domain"
                    + " that cannot be expressed in ASCII");
        }
        return asciiAddress;
    }

    /**
     * Punycodes the domain part, leaving the local part alone. Returns {@code null} when the domain
     * cannot be converted, which {@link IDN#toASCII} signals by throwing.
     */
    private static String toAsciiDomain(String address) {
        int at = address.lastIndexOf('@');
        if (at < 0) {
            return address;
        }
        try {
            return address.substring(0, at + 1) + IDN.toASCII(address.substring(at + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

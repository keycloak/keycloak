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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;

import org.keycloak.email.EmailException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins what a recipient can see, because that is exactly what must not change when a realm is moved
 * from SMTP to SES.
 * <p>
 * The produced bytes are parsed back with {@link MimeMessage} and asserted on the parsed message
 * rather than matched as text: it is a stronger check than string matching — a boundary, a folded
 * header or a transfer encoding cannot be got wrong and still parse — and it proves the bytes are
 * parseable at all, which is the one property SES will not tell us about until a real recipient
 * complains.
 */
class SesRawMessageTest {

    private static final String FROM_ADDRESS = "noreply@aldotime.it";
    private static final String RECIPIENT = "utente@example.com";

    /**
     * Several lines of plain ASCII: the case that makes jakarta.mail pick {@code 7bit} and copy the
     * body verbatim, bare line feeds and all.
     */
    private static final String ASCII_BODY = "Ciao,\nprima riga\nseconda riga\nterza riga\nSaluti\n";

    private final Map<String, String> config = new LinkedHashMap<>(Map.of(
            "from", FROM_ADDRESS,
            "fromDisplayName", "AldoTime"));

    @Test
    void buildsAMultipartAlternativeWithThePlainTextPartBeforeTheHtmlPart() throws Exception {
        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password",
                "Reset your password: https://aldotime.it/reset\n", "<p>Reset your password</p>");

        MimeMessage parsed = parse(message.mime());
        assertThat(parsed.isMimeType("multipart/alternative"), is(true));
        MimeMultipart parts = (MimeMultipart) parsed.getContent();
        assertThat(parts.getCount(), is(2));
        // The order is the whole point of multipart/alternative: the client shows the LAST part it
        // can render, so text first, html second. Swapped, every recipient sees plain text.
        assertThat(parts.getBodyPart(0).getContentType(), is("text/plain; charset=UTF-8"));
        // CRLF, not LF: the body reaches the recipient with MIME line endings, which is what
        // serialisesEveryLineEndingAsCrlfBecauseSesGetsTheBytesVerbatim() pins byte by byte.
        assertThat(parts.getBodyPart(0).getContent(), is("Reset your password: https://aldotime.it/reset\r\n"));
        assertThat(parts.getBodyPart(1).getContentType(), is("text/html; charset=UTF-8"));
        assertThat(parts.getBodyPart(1).getContent(), is("<p>Reset your password</p>"));
    }

    @Test
    void keepsASinglePlainTextPartWhenTheRealmHasNoHtmlTemplate() throws Exception {
        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        MimeMultipart parts = (MimeMultipart) parse(message.mime()).getContent();
        assertThat(parts.getCount(), is(1));
        assertThat(parts.getBodyPart(0).getContentType(), is("text/plain; charset=UTF-8"));
        assertThat(parts.getBodyPart(0).getContent(),
                is("Ciao,\r\nprima riga\r\nseconda riga\r\nterza riga\r\nSaluti\r\n"));
    }

    @Test
    void keepsASingleHtmlPartWhenThereIsNoTextBody() throws Exception {
        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password",
                null, "<p>Reset your password</p>");

        MimeMultipart parts = (MimeMultipart) parse(message.mime()).getContent();
        assertThat(parts.getCount(), is(1));
        assertThat(parts.getBodyPart(0).getContentType(), is("text/html; charset=UTF-8"));
        assertThat(parts.getBodyPart(0).getContent(), is("<p>Reset your password</p>"));
    }

    /**
     * A template that renders neither body is a bug further up in Keycloak, and it must surface as an
     * {@link EmailException} the caller reports — never as an accepted, empty email that SES bills
     * for and the recipient cannot read. jakarta.mail refuses to serialise a multipart with no parts,
     * which is the same thing that would happen on the SMTP path.
     */
    @Test
    void refusesToComposeAMessageWithNoBodyAtAll() {
        EmailException failure = assertThrows(EmailException.class,
                () -> SesRawMessage.compose(config, RECIPIENT, "Reset your password", null, null));

        assertThat(failure.getMessage(), is("Failed to compose the email message"));
    }

    /**
     * The header the recipient sees and the {@code FromEmailAddress} SES is asked to authorise come
     * from the same value. If they could drift, IAM would be authorising one identity while the
     * message claimed another.
     */
    @Test
    void sendsTheSameFromAddressInTheHeaderAsItSendsToSes() throws Exception {
        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        assertThat(message.fromHeaderValue(), is("AldoTime <noreply@aldotime.it>"));
        MimeMessage parsed = parse(message.mime());
        // equals() on InternetAddress compares the address only, so the display name is asserted
        // separately rather than through raw string equality on a header jakarta.mail may fold.
        assertThat(new InternetAddress(MimeUtility.unfold(parsed.getHeader("From", null))),
                is(new InternetAddress(message.fromHeaderValue())));
        assertThat(((InternetAddress) parsed.getFrom()[0]).getPersonal(), is("AldoTime"));
    }

    @Test
    void omitsTheDisplayNameWhenTheRealmConfiguresNone() throws Exception {
        config.remove("fromDisplayName");

        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        assertThat(message.fromHeaderValue(), is("noreply@aldotime.it"));
        assertThat(((InternetAddress) parse(message.mime()).getFrom()[0]).getPersonal(), is(nullValue()));
    }

    /**
     * The admin console posts a cleared field as an empty string rather than omitting the key, so
     * "blank" — not "absent" — is the shape a realm that never set a display name actually has. Taken
     * literally it becomes a quoted run of spaces in front of the sender address, on every email the
     * realm sends.
     */
    @Test
    void treatsABlankDisplayNameAsNoDisplayName() throws Exception {
        config.put("fromDisplayName", "   ");

        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        assertThat(message.fromHeaderValue(), is("noreply@aldotime.it"));
        assertThat(((InternetAddress) parse(message.mime()).getFrom()[0]).getPersonal(), is(nullValue()));
    }

    /**
     * A display name long enough to fold, in the alphabet an Italian or German realm actually uses.
     * The header then spans two lines and no longer equals {@code fromHeaderValue()} byte for byte —
     * the address and the name still have to survive, which a naive string comparison would miss and
     * a naive implementation (concatenating the name without RFC 2047 encoding) would corrupt.
     */
    @Test
    void foldsALongNonAsciiDisplayNameWithoutLosingTheNameOrTheAddress() throws Exception {
        String displayName = "AldoTime — Gestione presenze e ferie del personale";
        config.put("fromDisplayName", displayName);

        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        assertThat(message.fromHeaderValue(), is("=?UTF-8?Q?AldoTime_=E2=80=94_Gestione_presenze_e_ferie_del_"
                + "personale?= <noreply@aldotime.it>"));
        MimeMessage parsed = parse(message.mime());
        assertThat(parsed.getHeader("From", null), containsString("\r\n"));
        assertThat(new InternetAddress(MimeUtility.unfold(parsed.getHeader("From", null))),
                is(new InternetAddress(message.fromHeaderValue())));
        assertThat(((InternetAddress) parsed.getFrom()[0]).getPersonal(), is(displayName));
    }

    /**
     * Keycloak's SMTP sender always writes a {@code Reply-To}, defaulting it to the sender. Dropping
     * it when the realm configures none would be a visible change: replies would go wherever the
     * client decided instead of to the address the realm publishes.
     */
    @Test
    void defaultsReplyToToTheFromAddressWhenTheRealmSetsNone() throws Exception {
        config.put("replyTo", "  ");

        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        MimeMessage parsed = parse(message.mime());
        assertThat(parsed.getReplyTo().length, is(1));
        assertThat(((InternetAddress) parsed.getReplyTo()[0]).getAddress(), is(FROM_ADDRESS));
        assertThat(((InternetAddress) parsed.getReplyTo()[0]).getPersonal(), is("AldoTime"));
    }

    @Test
    void usesTheConfiguredReplyToAddressAndDisplayName() throws Exception {
        config.put("replyTo", "supporto@aldotime.it");
        config.put("replyToDisplayName", "Supporto AldoTime");

        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        MimeMessage parsed = parse(message.mime());
        assertThat(parsed.getReplyTo().length, is(1));
        assertThat(((InternetAddress) parsed.getReplyTo()[0]).getAddress(), is("supporto@aldotime.it"));
        assertThat(((InternetAddress) parsed.getReplyTo()[0]).getPersonal(), is("Supporto AldoTime"));
        // The From address is not the reply address, and must not have been overwritten by it.
        assertThat(((InternetAddress) parsed.getFrom()[0]).getAddress(), is(FROM_ADDRESS));
    }

    /**
     * SES takes the envelope recipient from {@code Destination.ToAddresses} and does not read the
     * {@code To} header. A mismatch delivers the message to one address while showing another.
     */
    @Test
    void sendsTheSameRecipientInTheToHeaderAsInTheEnvelope() throws Exception {
        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        assertThat(message.recipient(), is(RECIPIENT));
        assertThat(parse(message.mime()).getHeader("To", null), is(RECIPIENT));
    }

    /**
     * A header is an ASCII byte stream: an accented subject has to be RFC 2047 encoded, not written
     * as UTF-8 bytes. SES accepts the raw message either way, and the recipient sees mojibake.
     */
    @Test
    void encodesANonAsciiSubjectAsRfc2047AndDecodesBackToTheOriginal() throws Exception {
        String subject = "Reimposta la password – AldoTime";

        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, subject, ASCII_BODY, null);

        String rawBytes = new String(message.mime(), StandardCharsets.ISO_8859_1);
        assertThat(rawBytes, containsString("=?UTF-8?"));
        assertThat(rawBytes, not(containsString(latin1View("–"))));
        assertThat(parse(message.mime()).getSubject(), is(subject));
    }

    /**
     * A subject is interpolated from realm settings and message-bundle parameters, so a CR LF can
     * reach it. It must come out as a folded header, never as a second header: the raw bytes go to
     * SES as they are, and an injected {@code Bcc} would be delivered.
     */
    @Test
    void foldsCarriageReturnsInsideASubjectInsteadOfStartingANewHeader() throws Exception {
        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT,
                "Reimposta la password\r\nBcc: attaccante@example.com", ASCII_BODY, null);

        MimeMessage parsed = parse(message.mime());
        assertThat(parsed.getHeader("Bcc"), is(nullValue()));
        assertThat(parsed.getAllRecipients().length, is(1));
        assertThat(parsed.getSubject(), is("Reimposta la password Bcc: attaccante@example.com"));
    }

    @Test
    void carriesNonAsciiBodiesThroughTheRoundTripUnchanged() throws Exception {
        String text = "Buongiorno Loré,\npuò reimpostare la password qui: → https://aldotime.it/reset\n";
        String html = "<p>Buongiorno Loré, può reimpostare la password <a href=\"#\">qui →</a></p>";

        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", text, html);

        MimeMultipart parts = (MimeMultipart) parse(message.mime()).getContent();
        assertThat(parts.getBodyPart(0).getContent(),
                is("Buongiorno Loré,\r\npuò reimpostare la password qui: → https://aldotime.it/reset\r\n"));
        assertThat(parts.getBodyPart(1).getContent(), is(html));
    }

    /**
     * The one thing SMTP was silently doing for us. jakarta.mail copies a {@code 7bit} part verbatim,
     * so a template's bare LFs stay bare; over SMTP the mail implementation wraps the socket in a
     * CRLF-translating stream, while an SES {@code Content.Raw} payload is base64'd and delivered
     * exactly as given. The result would be a message that breaks RFC 5322 line endings, with DKIM
     * signed over it — and the second assertion is the proof that this body really is the case that
     * produces bare LFs, so the first one cannot pass by accident.
     */
    @Test
    void serialisesEveryLineEndingAsCrlfBecauseSesGetsTheBytesVerbatim() throws Exception {
        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        MimeMultipart parts = (MimeMultipart) parse(message.mime()).getContent();
        assertThat(parts.getBodyPart(0).getHeader("Content-Transfer-Encoding")[0], is("7bit"));
        assertThat(bareLineFeeds(message.mime()), is(0));
        assertThat(bareLineFeeds(sameBodyPartWrittenWithoutNormalisation()), is(5)); // one per body line
    }

    /**
     * Without {@code mail.from}, jakarta.mail builds the {@code Message-ID} from
     * {@code InetAddress.getLocalHost().getHostName()}: a blocking reverse lookup whose result — a
     * container id such as {@code keycloak-7d9f8c4b6-x2k9p} — would then be published to every
     * recipient. The domain on the right of the {@code @} has to be the sender's.
     */
    @Test
    void buildsTheMessageIdFromTheSenderDomainRatherThanTheLocalHostname() throws Exception {
        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        assertThat(parse(message.mime()).getHeader("Message-ID", null), endsWith("@aldotime.it>"));
    }

    @Test
    void rejectsAMissingOrBlankSenderAddress() {
        config.remove("from");
        assertThat(composeFailure().getMessage(), is("No sender address configured"));

        config.put("from", "   ");
        assertThat(composeFailure().getMessage(), is("No sender address configured"));
    }

    @Test
    void rejectsASyntacticallyInvalidSenderAddress() {
        config.put("from", "noreply.aldotime.it");

        assertThat(composeFailure().getMessage(), is("Invalid sender address 'noreply.aldotime.it'"));
    }

    @Test
    void rejectsASyntacticallyInvalidRecipientAddress() {
        EmailException failure = assertThrows(EmailException.class,
                () -> SesRawMessage.compose(config, "utente.example.com", "Reset your password", ASCII_BODY, null));

        assertThat(failure.getMessage(), is("Invalid recipient address 'utente.example.com'"));
    }

    @Test
    void rejectsASyntacticallyInvalidReplyToAddress() {
        config.put("replyTo", "supporto.aldotime.it");

        assertThat(composeFailure().getMessage(), is("Invalid reply-to address 'supporto.aldotime.it'"));
    }

    /**
     * The {@code To} header is written from the address verbatim, so an address carrying a line break
     * would append headers of the attacker's choosing to a message SES sends without further parsing.
     * The address validation is what stops it, and this test is here so a future relaxation of that
     * validation cannot pass unnoticed.
     */
    @Test
    void rejectsARecipientAddressThatCarriesAnInjectedHeader() {
        EmailException failure = assertThrows(EmailException.class,
                () -> SesRawMessage.compose(config, "utente@example.com\r\nBcc: attaccante@example.com",
                        "Reset your password", ASCII_BODY, null));

        assertThat(failure.getMessage(), containsString("Invalid recipient address"));
    }

    /**
     * An internationalised DOMAIN is punycoded rather than refused, which is what Keycloak's SMTP
     * sender does: refusing it would make the switch to SES a visible regression for any realm
     * whose users have an IDN address.
     */
    @Test
    void punycodesAnInternationalisedRecipientDomain() throws Exception {
        SesRawMessage message = SesRawMessage.compose(config, "utente@società.it", "Reset your password",
                ASCII_BODY, null);

        assertThat(message.recipient(), is("utente@xn--societ-nta.it"));
        assertThat(parse(message.mime()).getHeader("To", null), is("utente@xn--societ-nta.it"));
    }

    @Test
    void punycodesAnInternationalisedSenderDomain() throws Exception {
        config.put("from", "noreply@münchen.example");

        SesRawMessage message = SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null);

        assertThat(message.fromHeaderValue(), containsString("noreply@xn--mnchen-3ya.example"));
    }

    /**
     * A non-ASCII LOCAL part cannot be punycoded — it needs the SMTPUTF8 extension, which Amazon SES
     * does not implement. Rejecting it here produces a message an administrator can act on; letting
     * it through produces an opaque {@code MessageRejected} from AWS.
     */
    @Test
    void rejectsAnInternationalisedSenderLocalPartUpFront() {
        config.put("from", "nöreply@aldotime.it");

        assertThat(composeFailure().getMessage(),
                is("Amazon SES does not support internationalised (non 7-bit ASCII) sender addresses,"
                        + " but 'nöreply@aldotime.it' has a local part or a domain that cannot be expressed in ASCII"));
    }

    @Test
    void rejectsAnInternationalisedRecipientLocalPartUpFront() {
        EmailException failure = assertThrows(EmailException.class,
                () -> SesRawMessage.compose(config, "ütente@example.com", "Reset your password", ASCII_BODY, null));

        assertThat(failure.getMessage(),
                is("Amazon SES does not support internationalised (non 7-bit ASCII) recipient addresses,"
                        + " but 'ütente@example.com' has a local part or a domain that cannot be expressed in ASCII"));
    }

    /**
     * The rejected address is quoted back into a message that goes straight to the server log, and a
     * rejected address is precisely the kind that contains a line break. Left raw, the value writes
     * log lines of its own — a forged "WARN ... authentication succeeded" underneath the real entry —
     * which is why this asserts the shape of the whole message and not merely that the address was
     * mentioned: one line, no CR, no LF.
     */
    @Test
    void keepsARejectedAddressOnASingleLogLine() {
        EmailException failure = assertThrows(EmailException.class,
                () -> SesRawMessage.compose(config, "utente@example.com\r\nWARN  [org.keycloak] login succeeded",
                        "Reset your password", ASCII_BODY, null));

        String message = failure.getMessage();
        assertThat(message, not(containsString("\r")));
        assertThat(message, not(containsString("\n")));
        assertThat(message.lines().count(), is(1L));
        // The control characters are replaced rather than dropped, so nothing is silently glued
        // together and the operator can still see what was sent.
        assertThat(message, containsString("'utente@example.com??WARN  [org.keycloak] login succeeded'"));
    }

    /**
     * An address is attacker-supplied on the self-registration path, and the whole of it is quoted
     * back. Without the cap, a megabyte of junk in the registration form becomes a megabyte of log —
     * the cheapest way there is to push the entries that matter out of a retention window.
     */
    @Test
    void truncatesAnAbsurdlyLongRejectedAddress() {
        String longAddress = "u".repeat(5000) + ".example.com";

        EmailException failure = assertThrows(EmailException.class,
                () -> SesRawMessage.compose(config, longAddress, "Reset your password", ASCII_BODY, null));

        String quoted = failure.getMessage().split("'")[1];
        assertThat(quoted, startsWith("u".repeat(120)));
        // 120 characters and the ellipsis that says there were more.
        assertThat(quoted, hasLength(121));
        assertThat(quoted, endsWith("…"));
    }

    private EmailException composeFailure() {
        return assertThrows(EmailException.class,
                () -> SesRawMessage.compose(config, RECIPIENT, "Reset your password", ASCII_BODY, null));
    }

    private static byte[] sameBodyPartWrittenWithoutNormalisation() throws MessagingException, IOException {
        MimeBodyPart part = new MimeBodyPart();
        part.setText(ASCII_BODY, StandardCharsets.UTF_8.name());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        part.writeTo(out);
        return out.toByteArray();
    }

    private static int bareLineFeeds(byte[] bytes) {
        int count = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '\n' && (i == 0 || bytes[i - 1] != '\r')) {
                count++;
            }
        }
        return count;
    }

    /** The UTF-8 bytes of {@code text} seen as Latin-1, i.e. how they would look in the raw message. */
    private static String latin1View(String text) {
        return new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    private static MimeMessage parse(byte[] mime) throws MessagingException {
        return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(mime));
    }
}

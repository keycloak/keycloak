/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.email;

import com.google.common.io.ByteStreams;
import com.sun.mail.smtp.SMTPMessage;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.truststore.HostnameVerificationPolicy;
import org.keycloak.truststore.JSSETruststoreConfigurator;
import org.keycloak.vault.VaultStringSecret;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.EncodingAware;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultEmailSenderProvider implements EmailSenderProvider {

    private static final Logger logger = Logger.getLogger(DefaultEmailSenderProvider.class);

    private final KeycloakSession session;

    public DefaultEmailSenderProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody, String htmlBody) throws EmailException {
        send(config, user, subject, textBody, htmlBody, null);
    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody, String htmlBody, Map<String, InputStream> embeddables) throws EmailException {
        Transport transport = null;
        try {
            String address = retrieveEmailAddress(user);

            Properties props = new Properties();

            if (config.containsKey("host")) {
                props.setProperty("mail.smtp.host", config.get("host"));
            }

            boolean auth = "true".equals(config.get("auth"));
            boolean ssl = "true".equals(config.get("ssl"));
            boolean starttls = "true".equals(config.get("starttls"));

            if (config.containsKey("port") && config.get("port") != null) {
                props.setProperty("mail.smtp.port", config.get("port"));
            }

            if (auth) {
                props.setProperty("mail.smtp.auth", "true");
            }

            if (ssl) {
                props.setProperty("mail.smtp.ssl.enable", "true");
            }

            if (starttls) {
                props.setProperty("mail.smtp.starttls.enable", "true");
            }

            if (ssl || starttls) {
                setupTruststore(props);
            }

            props.setProperty("mail.smtp.timeout", "10000");
            props.setProperty("mail.smtp.connectiontimeout", "10000");

            String from = config.get("from");
            String fromDisplayName = config.get("fromDisplayName");
            String replyTo = config.get("replyTo");
            String replyToDisplayName = config.get("replyToDisplayName");
            String envelopeFrom = config.get("envelopeFrom");

            Session session = Session.getInstance(props);

            Multipart multipart = new MimeMultipart("alternative");

            if (textBody != null) {
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(textBody, "UTF-8");
                multipart.addBodyPart(textPart);
            }

            if (htmlBody != null) {
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
                
                if (embeddables == null || embeddables.isEmpty()) {
                    multipart.addBodyPart(htmlPart);
                } else {
                    MimeMultipart related = new MimeMultipart("related");
                    MimeBodyPart relatedBodyPart = new MimeBodyPart();
                    relatedBodyPart.setContent(related);
                    multipart.addBodyPart(relatedBodyPart);
                    related.addBodyPart(htmlPart);
                    
                    for (Map.Entry<String, InputStream> entry: embeddables.entrySet()) {
                        MimeBodyPart embeddedPart = new MimeBodyPart();
                        embeddedPart.setHeader("Content-ID", "<" + entry.getKey() + ">");
                        embeddedPart.setDataHandler(new DataHandler(new StreamDataSource(entry.getValue(), entry.getKey())));
                        embeddedPart.setDisposition(BodyPart.INLINE);
                        related.addBodyPart(embeddedPart);
                    }
                }
            }

            SMTPMessage msg = new SMTPMessage(session);
            msg.setFrom(toInternetAddress(from, fromDisplayName));

            msg.setReplyTo(new Address[]{toInternetAddress(from, fromDisplayName)});
            if (replyTo != null && !replyTo.isEmpty()) {
                msg.setReplyTo(new Address[]{toInternetAddress(replyTo, replyToDisplayName)});
            }
            if (envelopeFrom != null && !envelopeFrom.isEmpty()) {
                msg.setEnvelopeFrom(envelopeFrom);
            }

            msg.setHeader("To", address);
            msg.setSubject(subject, "utf-8");
            msg.setContent(multipart);
            msg.saveChanges();
            msg.setSentDate(new Date());

            transport = session.getTransport("smtp");
            if (auth) {
                try (VaultStringSecret vaultStringSecret = this.session.vault().getStringSecret(config.get("password"))) {
                    transport.connect(config.get("user"), vaultStringSecret.get().orElse(config.get("password")));
                }
            } else {
                transport.connect();
            }
            transport.sendMessage(msg, new InternetAddress[]{new InternetAddress(address)});
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
            throw new EmailException(e);
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    logger.warn("Failed to close transport", e);
                }
            }
        }
    }

    protected InternetAddress toInternetAddress(String email, String displayName) throws UnsupportedEncodingException, AddressException, EmailException {
        if (email == null || "".equals(email.trim())) {
            throw new EmailException("Please provide a valid address", null);
        }
        if (displayName == null || "".equals(displayName.trim())) {
            return new InternetAddress(email);
        }
        return new InternetAddress(email, displayName, "utf-8");
    }

    protected String retrieveEmailAddress(UserModel user) {
        return user.getEmail();
    }

    private void setupTruststore(Properties props) throws NoSuchAlgorithmException, KeyManagementException {

        JSSETruststoreConfigurator configurator = new JSSETruststoreConfigurator(session);

        SSLSocketFactory factory = configurator.getSSLSocketFactory();
        if (factory != null) {
            props.put("mail.smtp.ssl.socketFactory", factory);
            if (configurator.getProvider().getPolicy() == HostnameVerificationPolicy.ANY) {
                props.setProperty("mail.smtp.ssl.trust", "*");
            }
        }
    }

    @Override
    public void close() {

    }

    private static class StreamDataSource implements DataSource, EncodingAware {

        private InputStream stream;
        private String name;
        private String contentType;

        public StreamDataSource(InputStream is, String name) {
            this.name = name;
            try {
                stream = new ByteArrayInputStream(ByteStreams.toByteArray(is)) {
                    //inputstream can be used multiple times (MimeUtility.getEncoding() from old javax.mail package), so close will reset
                    public void close() throws IOException {
                        pos = 0;
                        mark = 0;
                    };
                };
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }

            try {
                contentType = URLConnection.guessContentTypeFromStream(stream);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return stream;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getEncoding() {
            return "base64";
        }
    }

}

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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.mail.internet.MimeUtility;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.truststore.JSSETruststoreConfigurator;
import org.keycloak.vault.VaultStringSecret;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeMessage;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import static org.keycloak.utils.StringUtil.isNotBlank;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultEmailSenderProvider implements EmailSenderProvider {

    private static final Logger logger = Logger.getLogger(DefaultEmailSenderProvider.class);
    private static final String SUPPORTED_SSL_PROTOCOLS = getSupportedSslProtocols();

    private final KeycloakSession session;

    public DefaultEmailSenderProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody, String htmlBody) throws EmailException {
        String address = retrieveEmailAddress(user);
        if (address == null) {
            throw new EmailException("No email address configured for the user");
        }
        send(config, address, subject, textBody, htmlBody);
    }

    @Override
    public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {
        Transport transport = null;
        try {
            String from = config.get("from");
            if (from == null) {
                throw new EmailException("No sender address configured in the realm settings for emails");
            }
            String fromDisplayName = config.get("fromDisplayName");
            String replyTo = config.get("replyTo");
            String replyToDisplayName = config.get("replyToDisplayName");

            Properties props = buildSMTPProperties(config);
            Multipart multipart = buildMultipartBody(textBody, htmlBody);

            Session session = Session.getInstance(props);

            Message msg = buildMessage(address, subject, session, from, fromDisplayName, replyTo, replyToDisplayName, multipart);

            transport = session.getTransport("smtp");
            if (isAuthConfigured(config)) {

                switch(config.getOrDefault("authType", "basic")) {

                    case "basic": {
                        //password
                        connectWithPassword(config, transport);
                        break;
                    }

                    case "token": {
                        //token
                        connectWithToken(config, transport);
                        break;
                    }


                }

            } else {
                transport.connect();
            }
            transport.sendMessage(msg, new InternetAddress[]{new InternetAddress(address)});

        } catch (EmailException e) {
            throw e;
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
            throw new EmailException("Error when attempting to send the email to the server. More information is available in the server log.", e);
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

    private void connectWithPassword(Map<String, String> config, Transport transport) throws MessagingException {
        try (VaultStringSecret vaultStringSecret = this.session.vault().getStringSecret(config.get("password"))) {
            transport.connect(config.get("user"), vaultStringSecret.get().orElse(config.get("password")));
        }
    }

    private void connectWithToken(Map<String, String> config, Transport transport) throws IOException, MessagingException {
        try (VaultStringSecret vaultStringSecret = this.session.vault().getStringSecret(config.get("authTokenClientSecret"))) {
            String clientSecret = vaultStringSecret.get().orElse(config.get("authTokenClientSecret"));

            JsonNode response = getToken(config, clientSecret);

            if(response.has("access_token")) {
                String token = response.get("access_token").asText();
                transport.connect(config.get("user"), token);
            }
        }
    }

    private JsonNode getToken(Map<String, String> config, String clientSecret) throws IOException {
        JsonNode response = SimpleHttp.doPost(config.get("authTokenUrl"), this.session)
                .param("client_id", config.get("authTokenClientId"))
                .param("client_secret", clientSecret)
                .param("scope", config.get("authTokenScope"))
                .param("grant_type", "client_credentials").asJson();
        return response;
    }

    private static boolean isStarttlsConfigured(Map<String, String> config) {
        return "true".equals(config.get("starttls"));
    }

    private static boolean isSslConfigured(Map<String, String> config) {
        return "true".equals(config.get("ssl"));
    }

    private static boolean isDebugEnabled(Map<String, String> config) {
        return "true".equals(config.get("debug"));
    }

    private boolean isAuthConfigured(Map<String, String> config) {
        return "true".equals(config.get("auth"));
    }

    private boolean isAuthTypeTokenConfigured(Map<String, String> config) {
        return "token".equals(config.get("authType"));
    }

    private Message buildMessage(String address, String subject, Session session, String from, String fromDisplayName, String replyTo, String replyToDisplayName, Multipart multipart) throws MessagingException, UnsupportedEncodingException, EmailException {
        Message msg = new MimeMessage(session);

        msg.setFrom(toInternetAddress(from, fromDisplayName));
        msg.setReplyTo(new Address[]{toInternetAddress(from, fromDisplayName)});

        if (isNotBlank(replyTo)) {
            msg.setReplyTo(new Address[]{toInternetAddress(replyTo, replyToDisplayName)});
        }

        msg.setHeader("To", address);
        msg.setSubject(MimeUtility.encodeText(subject, StandardCharsets.UTF_8.name(), null));
        msg.setContent(multipart);
        msg.saveChanges();
        msg.setSentDate(new Date());
        return msg;
    }

    private Multipart buildMultipartBody(String textBody, String htmlBody) throws MessagingException {
        Multipart multipart = new MimeMultipart("alternative");

        if (textBody != null) {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(textBody, "UTF-8");
            multipart.addBodyPart(textPart);
        }

        if (htmlBody != null) {
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);
        }
        return multipart;
    }

    private Properties buildSMTPProperties(Map<String, String> config) {
        Properties props = new Properties();

        if (config.containsKey("host")) {
            props.setProperty("mail.smtp.host", config.get("host"));
        }

        if (config.containsKey("port") && config.get("port") != null) {
            props.setProperty("mail.smtp.port", config.get("port"));
        }

        if (isAuthConfigured(config)) {
            props.setProperty("mail.smtp.auth", "true");
        }

        if(isAuthTypeTokenConfigured(config)) {
            props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        }

        if(isDebugEnabled(config)) {
            props.put("mail.debug", "true");
        }

        if (isSslConfigured(config)) {
            props.setProperty("mail.smtp.ssl.enable", "true");
        }

        if (isStarttlsConfigured(config)) {
            props.setProperty("mail.smtp.starttls.enable", "true");
        }

        if (isSslConfigured(config) || isStarttlsConfigured(config) || isAuthConfigured(config)){
            props.put("mail.smtp.ssl.protocols", SUPPORTED_SSL_PROTOCOLS);

            setupTruststore(props);
        }

        props.setProperty("mail.smtp.timeout", "10000");
        props.setProperty("mail.smtp.connectiontimeout", "10000");

        String envelopeFrom = config.get("envelopeFrom");
        if (isNotBlank(envelopeFrom)) {
            props.setProperty("mail.smtp.from", envelopeFrom);
        }
        return props;
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

    private void setupTruststore(Properties props) {
        JSSETruststoreConfigurator configurator = new JSSETruststoreConfigurator(session);

        SSLSocketFactory factory = configurator.getSSLSocketFactory();
        if (factory != null) {
            props.put("mail.smtp.ssl.socketFactory", factory);
            if (configurator.getProvider().getPolicy() == HostnameVerificationPolicy.ANY) {
                props.setProperty("mail.smtp.ssl.trust", "*");
                props.put("mail.smtp.ssl.checkserveridentity", Boolean.FALSE.toString()); // this should be the default but seems to be impl specific, so set it explicitly just to be sure
            }
            else {
                props.put("mail.smtp.ssl.checkserveridentity", Boolean.TRUE.toString());
            }
        }
    }

    @Override
    public void close() {

    }

    private static String getSupportedSslProtocols() {
        try {
            String[] protocols = SSLContext.getDefault().getSupportedSSLParameters().getProtocols();
            if (protocols != null) {
                return String.join(" ", protocols);
            }
        } catch (Exception e) {
            logger.warn("Failed to get list of supported SSL protocols", e);
        }
        return null;
    }

}

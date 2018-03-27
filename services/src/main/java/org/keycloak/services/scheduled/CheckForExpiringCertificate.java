/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.timer.ScheduledTask;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Checks for certificates that are getting close to expiring.  If a certificate is about to expire, then we send out a
 * notification.
 *
 * @author <a href="mailto:benjamin.berg@redhat.com">Benjamin Jacob Berg</a>
 * @version $Revision: 1 $
 * @since March 26th 2018
 */
public abstract class CheckForExpiringCertificate implements ScheduledTask {
    private static final Logger LOG = Logger.getLogger(CheckForExpiringCertificate.class);
    private static final String EMAIL_SUBJECT = "Certificate Renewal Notification";
    private static final String EMAIL_TEXT_BODY = "The following certificate is going to expire (or has expired) on %s \n" +
                                                  "\tSerial number = %s \n" +
                                                  "\tSubjectDN = %s";
    private static final String EMAIL_HTML_BODY = "<p>The following certificate is going to expire (or has expired) on %s </p>\n" +
                                                  "<table>\n" +
                                                  "\t<ul>Serial number = %s</ul>\n" +
                                                  "\t<ul>SubjectDN = %s</ul>\n" +
                                                  "</table>";

    /**
     * Decodes a Base64 encoded certificate and converts that to a X509 certificate.
     *
     * @param certificate Base64 encoded certificate.
     * @return X509 certificate
     */
    protected Certificate convertStringToCertificate(String certificate) throws CertificateException {
        Objects.requireNonNull(certificate, "Certificate String cannot be null.");

        byte[] decoded = Base64.getDecoder().decode(certificate);
        return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decoded));
    }

    /**
     * Is the certificates expiration about to expire in 90 days?
     *
     * @param expiration Timestamp of the certificate's expiration.
     * @return Expiring in 90 days or less? TRUE or FALSE
     */
    protected boolean isCertificateExpiring(LocalDateTime expiration) {
        return expiration.isBefore(LocalDateTime.now().plusDays(90));
    }

    /**
     * Send out a notification pertaining to the certificate expiration.
     *
     * @param session     For getting the {@link EmailSenderProvider}
     * @param realm       The SMTP configuration is in here.
     * @param certificate The certificate that is about to expire.
     */
    protected void sendNotification(KeycloakSession session, RealmModel realm, X509Certificate certificate) {
        Objects.requireNonNull(session, "KeycloakSession cannot be null.");
        Objects.requireNonNull(realm, "RealmModel cannot be null.");
        Objects.requireNonNull(certificate, "X509Certificate cannot be null.");

        EmailSenderProvider emailSenderProvider = session.getProvider(EmailSenderProvider.class);

        String email = realm.getSmtpConfig().get("notification");
        if (email == null || email.isEmpty()) return;

        try {
            emailSenderProvider.send(realm.getSmtpConfig(), email, EMAIL_SUBJECT, format(EMAIL_TEXT_BODY, certificate.getNotAfter(), certificate.getSerialNumber(), certificate.getSubjectDN()), format(EMAIL_HTML_BODY, certificate.getNotAfter(), certificate.getSerialNumber(), certificate.getSubjectDN()));
        } catch (EmailException e) {
            LOG.error("Exception occurred while attempting to send email.", e);
        }
    }
}

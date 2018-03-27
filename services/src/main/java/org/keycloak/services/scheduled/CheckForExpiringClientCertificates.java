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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Checks for certificates that are getting close to expiring.  If a certificate is about to expire, then we send out a
 * notification.
 *
 * @author <a href="mailto:benjamin.berg@redhat.com">Benjamin Jacob Berg</a>
 * @version $Revision: 1 $
 * @since March 29th 2018
 */
public class CheckForExpiringClientCertificates extends CheckForExpiringCertificate {
    private static final Logger LOG = Logger.getLogger(CheckForExpiringClientCertificates.class);

    /**
     * Checks that the client certificates from the {@link KeycloakSession} are checked to see if they are going to be
     * expiring soon. If they are, send out a notification.
     *
     * @param session Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     */
    @Override
    public void run(KeycloakSession session) {
        checkForExpiringClientCertificates(session);
    }

    /**
     * Validates the session and realms are not null and then passes the session to be iterated over.
     *
     * @param session Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     */
    private void checkForExpiringClientCertificates(KeycloakSession session) {
        Objects.requireNonNull(session, "KeycloakSession cannot be null.");

        iterateRealms(session);
    }

    /**
     * Iterates over realms to retrieve the client list and passes that object to be iterated over.
     *
     * @param session Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     */
    private void iterateRealms(KeycloakSession session) {
        Objects.requireNonNull(session.realms(), "RealmProvider cannot be null.");
        Objects.requireNonNull(session.realms().getRealms(), "RealmModel cannot be null.");

        for (RealmModel realm : session.realms().getRealms()) {
            List<ClientModel> clients = realm.getClients();

            iterateClients(session, realm, clients);
        }
    }

    /**
     * Iterates the client list passing the objects to have the certificate checked if the expiration date is
     * approaching.
     *
     * @param session Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     */
    private void iterateClients(KeycloakSession session, RealmModel realm, List<ClientModel> clients) {
        Objects.requireNonNull(clients, "clients cannot be null.");

        for (ClientModel client : clients) {
            getClientCertificate(session, realm, client);
        }
    }

    /**
     * Retrieves the certificate from the client and checks if the expiration date is approaching within 90 days.
     *
     * @param session Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     * @param realm   Containing client information.
     * @param client  Information for the certificates.
     */
    private void getClientCertificate(KeycloakSession session, RealmModel realm, ClientModel client) {
        Objects.requireNonNull(client, "client cannot be null.");

        Map<String, String> attributes = client.getAttributes();
        String certificate = attributes.get("saml.signing.certificate");

        if (certificate == null) return;

        X509Certificate x509Certificate;
        try {
            x509Certificate = (X509Certificate) convertStringToCertificate(certificate);
        } catch (CertificateException e) {
            LOG.error("Could not convert the string to a certificate.  Original Value: ({})", certificate, e);
            return;
        }
        LocalDateTime expiration = LocalDateTime.ofInstant(x509Certificate.getNotAfter().toInstant(), ZoneId.systemDefault());

        LOG.debugf("The certificate (%s) from the realm (%s) has an expiration date of (%s).", x509Certificate, realm, expiration);

        if (isCertificateExpiring(expiration)) {
            sendNotification(session, realm, x509Certificate);
        }
    }
}

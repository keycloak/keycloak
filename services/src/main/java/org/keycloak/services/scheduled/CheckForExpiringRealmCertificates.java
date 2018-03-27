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
import org.keycloak.keys.RsaKeyMetadata;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Checks for certificates that are getting close to expiring.  If a certificate is about to expire, then we send out a
 * notification.
 *
 * @author <a href="mailto:benjamin.berg@redhat.com">Benjamin Jacob Berg</a>
 * @version $Revision: 1 $
 * @since March 26th 2018
 */
public class CheckForExpiringRealmCertificates extends CheckForExpiringCertificate {
    private static final Logger LOG = Logger.getLogger(CheckForExpiringRealmCertificates.class);

    /**
     * Checks that the realm certificates from the {@link KeycloakSession} are checked to see if they are going to be
     * expiring soon. If they are, send out a notification.
     *
     * @param session Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     */
    @Override
    public void run(KeycloakSession session) {
        checkForExpiringRealmCertificates(session);
    }

    /**
     * Passes the session to have it's realms be iterated over.
     *
     * @param session Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     */
    private void checkForExpiringRealmCertificates(KeycloakSession session) {
        Objects.requireNonNull(session, "KeycloakSession cannot be null.");

        iterateRealms(session);
    }

    /**
     * Iterates over realms to retrieve the RSA key metadata list and passes that object to be iterated over.
     *
     * @param session Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     */
    private void iterateRealms(KeycloakSession session) {
        Objects.requireNonNull(session.realms(), "RealmProvider cannot be null.");
        Objects.requireNonNull(session.realms().getRealms(), "RealmModel cannot be null.");

        for (RealmModel realm : session.realms().getRealms()) {
            List<RsaKeyMetadata> rsaKeyMetadatas = session.keys().getRsaKeys(realm, false);

            iterateRSAKeyMetadata(session, realm, rsaKeyMetadatas);
        }
    }

    /**
     * Iterates the RSA key metadata list passing the objects to have the certificate checked if the expiration date is
     * approaching.
     *
     * @param session Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     */
    private void iterateRSAKeyMetadata(KeycloakSession session, RealmModel realm, List<RsaKeyMetadata> rsaKeyMetadatas) {
        Objects.requireNonNull(rsaKeyMetadatas, "rsaKeyMetadatas cannot be null.");

        for (RsaKeyMetadata rsaKeyMetadata : rsaKeyMetadatas) {
            getRealmCertificate(session, realm, rsaKeyMetadata);
        }
    }

    /**
     * Retrieves the certificate from the client and checks if the expiration date is approaching within 90 days.
     *
     * @param session        Realms, clients and certificates can be retrieved from {@link KeycloakSession}
     * @param realm          Containing RSA key metadata information.
     * @param rsaKeyMetadata Information for the certificates.
     */
    private void getRealmCertificate(KeycloakSession session, RealmModel realm, RsaKeyMetadata rsaKeyMetadata) {
        Objects.requireNonNull(rsaKeyMetadata, "rsaKeyMetadata cannot be null.");

        X509Certificate certificate = (X509Certificate) rsaKeyMetadata.getCertificate();
        LocalDateTime expiration = LocalDateTime.ofInstant(certificate.getNotAfter().toInstant(), ZoneId.systemDefault());

        LOG.debugf("The certificate (%s) from the realm (%s) has an expiration date of (%s).", certificate, realm, expiration);

        if (isCertificateExpiring(expiration)) {
            sendNotification(session, realm, certificate);
        }
    }
}

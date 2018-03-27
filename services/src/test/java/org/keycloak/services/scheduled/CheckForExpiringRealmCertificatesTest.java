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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.keys.RsaKeyMetadata;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.timer.ScheduledTask;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckForExpiringRealmCertificatesTest {
    private ScheduledTask scheduledTask = new CheckForExpiringRealmCertificates();

    @Mock
    private KeycloakSession keycloakSession;

    @Mock
    private RealmProvider realmProvider;

    @Mock
    private KeyManager keyManager;

    @Mock
    private RealmModel realmModel;

    @Mock
    private RsaKeyMetadata rsaKeyMetadata;

    @Mock
    private X509Certificate certificate;

    @Test
    public void expiringRealmCertificateTest() {
        FakeEmailSenderProvider fakeEmailSenderProvider = new FakeEmailSenderProvider();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("notification", "emailAddress");

        when(realmModel.getSmtpConfig()).thenReturn(attributes);
        when(keyManager.getRsaKeys(realmModel, false)).thenReturn(Collections.singletonList(rsaKeyMetadata));
        when(rsaKeyMetadata.getCertificate()).thenReturn(certificate);
        when(keycloakSession.keys()).thenReturn(keyManager);
        when(realmProvider.getRealms()).thenReturn(Collections.singletonList(realmModel));
        when(keycloakSession.realms()).thenReturn(realmProvider);
        when(certificate.getNotAfter()).thenReturn(daysInTheFuture(90));
        when(rsaKeyMetadata.getCertificate()).thenReturn(certificate);
        when(keycloakSession.realms()).thenReturn(realmProvider);
        when(keycloakSession.getProvider(any())).thenReturn(fakeEmailSenderProvider);

        scheduledTask.run(keycloakSession);

        assertEquals(attributes, fakeEmailSenderProvider.getConfig());
        assertEquals("emailAddress", fakeEmailSenderProvider.getAddress());
        assertEquals("Certificate Renewal Notification", fakeEmailSenderProvider.getSubject());
        assertTrue(fakeEmailSenderProvider.getTextBody().contains("The following certificate is going to expire (or has expired)"));
        assertTrue(fakeEmailSenderProvider.getHtmlBody().contains("The following certificate is going to expire (or has expired)"));
    }

    @Test
    public void nonExpiringRealmCertificateTest() {
        FakeEmailSenderProvider fakeEmailSenderProvider = new FakeEmailSenderProvider();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("notification", "emailAddress");

        when(realmModel.getSmtpConfig()).thenReturn(attributes);
        when(keyManager.getRsaKeys(realmModel, false)).thenReturn(Collections.singletonList(rsaKeyMetadata));
        when(rsaKeyMetadata.getCertificate()).thenReturn(certificate);
        when(keycloakSession.keys()).thenReturn(keyManager);
        when(realmProvider.getRealms()).thenReturn(Collections.singletonList(realmModel));
        when(keycloakSession.realms()).thenReturn(realmProvider);
        when(certificate.getNotAfter()).thenReturn(daysInTheFuture(91));
        when(rsaKeyMetadata.getCertificate()).thenReturn(certificate);
        when(keycloakSession.realms()).thenReturn(realmProvider);
        when(keycloakSession.getProvider(any())).thenReturn(fakeEmailSenderProvider);

        scheduledTask.run(keycloakSession);

        assertNull(fakeEmailSenderProvider.getConfig());
        assertNull(fakeEmailSenderProvider.getAddress());
        assertNull(fakeEmailSenderProvider.getSubject());
        assertNull(fakeEmailSenderProvider.getTextBody());
        assertNull(fakeEmailSenderProvider.getHtmlBody());
    }

    private static Date daysInTheFuture(int time) {
        Date futureDate = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(futureDate);
        calendar.add(Calendar.DATE, time);

        return calendar.getTime();
    }

    private class FakeEmailSenderProvider implements EmailSenderProvider {
        private Map<String, String> config;
        private String address;
        private String subject;
        private String textBody;
        private String htmlBody;

        @Override
        public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) {
            this.config = config;
            this.address = address;
            this.subject = subject;
            this.textBody = textBody;
            this.htmlBody = htmlBody;
        }

        @Override
        public void close() {

        }

        Map<String, String> getConfig() {
            return config;
        }

        String getAddress() {
            return address;
        }

        String getSubject() {
            return subject;
        }

        String getTextBody() {
            return textBody;
        }

        String getHtmlBody() {
            return htmlBody;
        }
    }

}
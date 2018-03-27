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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.timer.ScheduledTask;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckForExpiringClientCertificatesTest {
    private ScheduledTask scheduledTask = new CheckForExpiringClientCertificates();

    @Mock
    private KeycloakSession keycloakSession;

    @Mock
    private RealmProvider realmProvider;

    @Mock
    private KeyManager keyManager;

    @Mock
    private RealmModel realmModel;

    @Mock
    private ClientModel clientModel;

    @Mock
    private RsaKeyMetadata rsaKeyMetadata;

    @Mock
    private X509Certificate certificate;

    @Test
    public void expiringClientCertificateTest() {
        FakeEmailSenderProvider fakeEmailSenderProvider = new FakeEmailSenderProvider();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("saml.signing.certificate", "MIIDyDCCArACCQDTN09xVpJeYDANBgkqhkiG9w0BAQsFADCBpTELMAkGA" +
                                                   "1UEBhMCVVMxFzAVBgNVBAgMDk5vcnRoIENhcm9saW5hMRAwDgYDVQQHDA" +
                                                   "dSYWxlaWdoMRYwFAYDVQQKDA1SZWQgSGF0LCBJbmMuMRMwEQYDVQQLDAp" +
                                                   "SZWQgSGF0IElUMRswGQYDVQQDDBJSZWQgSGF0IElUIFJvb3QgQ0ExITAf" +
                                                   "BgkqhkiG9w0BCQEWEmluZm9zZWNAcmVkaGF0LmNvbTAeFw0xODAzMjAxN" +
                                                   "DU1NDFaFw0xODAzMjExNDU1NDFaMIGlMQswCQYDVQQGEwJVUzEXMBUGA1" +
                                                   "UECAwOTm9ydGggQ2Fyb2xpbmExEDAOBgNVBAcMB1JhbGVpZ2gxFjAUBgN" +
                                                   "VBAoMDVJlZCBIYXQsIEluYy4xEzARBgNVBAsMClJlZCBIYXQgSVQxGzAZ" +
                                                   "BgNVBAMMElJlZCBIYXQgSVQgUm9vdCBDQTEhMB8GCSqGSIb3DQEJARYSa" +
                                                   "W5mb3NlY0ByZWRoYXQuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMI" +
                                                   "IBCgKCAQEA2xNjht4K9BmYkh/6gx6/K+DDXjBEyzukeVP3dOGf7c5Q++p" +
                                                   "W6KS8cpCBmegkoJlKhMRCkBrGFls3agFU5OoYJCb9MsM6UBnHjecBH1fW" +
                                                   "WB43Nn+D9e656bjQzWnP0dXTgLsIMCkxrbhqkY5K+jmCe1Kb+KcpWWfsv" +
                                                   "D1arDABYVWQEITFieYB1rxNAWEVa8qwkG0G5fNe4zIaNNaPxIMFtY7IN8" +
                                                   "XWsl0kJEr68v7hjoBIwWdenNBaNzC2fNNgk53CRARkwu3ggEBq9pMN4Xm" +
                                                   "x+rFYZG7akNn4B0o/NZ7xxusuV7oZNtZutOq5KECgPTYdfVCuOr+2Ejt/" +
                                                   "4jbrJ+Rv/QIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQA00bzrQdasU9Kok" +
                                                   "qEDS/3Vlibh7jRPG+oVI9ofjSJq+qS02Ady5bK+vv8SbIpNWPna7q5Z16" +
                                                   "VvbTx6nZW6cMAFAVLrwtpvI9zP0JjBYiXEF/oHLuKNqwjHAC/RcmQr80x" +
                                                   "n/ArJy17Z5QUS30rkZ0LDlCX03OLRCEq5rEHmPwz4VolR6qQVUVaYzD19" +
                                                   "ccuZBf5OYrf+EVO+voZnL8sHQY6lLtaDMgCdFNdgm/9mm+262Srg4pAV3" +
                                                   "7Le7Z+y15+e+tZS+Nyal8Ci4HY6lT/Tiw0BEH67HynDXHI2F2y5iqjo2N" +
                                                   "znnB0+K9/BAJfZlN11rgl7onwD5tt7hd+1D7M972In");
        attributes.put("notification", "emailAddress");

        when(realmModel.getSmtpConfig()).thenReturn(attributes);
        when(keyManager.getRsaKeys(realmModel, false)).thenReturn(Collections.singletonList(rsaKeyMetadata));
        when(rsaKeyMetadata.getCertificate()).thenReturn(certificate);
        when(keycloakSession.keys()).thenReturn(keyManager);
        when(realmProvider.getRealms()).thenReturn(Collections.singletonList(realmModel));
        when(clientModel.getAttributes()).thenReturn(attributes);
        when(realmModel.getClients()).thenReturn(Collections.singletonList(clientModel));
        when(realmModel.getSmtpConfig()).thenReturn(attributes);
        when(keycloakSession.realms()).thenReturn(realmProvider);
        when(keycloakSession.getProvider(EmailSenderProvider.class)).thenReturn(fakeEmailSenderProvider);

        scheduledTask.run(keycloakSession);

        assertEquals(attributes, fakeEmailSenderProvider.getConfig());
        assertEquals("emailAddress", fakeEmailSenderProvider.getAddress());
        assertEquals("Certificate Renewal Notification", fakeEmailSenderProvider.getSubject());
        assertTrue(fakeEmailSenderProvider.getTextBody().contains("The following certificate is going to expire (or has expired)"));
        assertTrue(fakeEmailSenderProvider.getHtmlBody().contains("The following certificate is going to expire (or has expired)"));
    }

    @Test
    public void badClientCertificateTest() {
        FakeEmailSenderProvider fakeEmailSenderProvider = new FakeEmailSenderProvider();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("saml.signing.certificate", "badCertificate");
        attributes.put("notification", "emailAddress");

        when(realmModel.getSmtpConfig()).thenReturn(attributes);
        when(keyManager.getRsaKeys(realmModel, false)).thenReturn(Collections.singletonList(rsaKeyMetadata));
        when(rsaKeyMetadata.getCertificate()).thenReturn(certificate);
        when(keycloakSession.keys()).thenReturn(keyManager);
        when(realmProvider.getRealms()).thenReturn(Collections.singletonList(realmModel));
        when(clientModel.getAttributes()).thenReturn(attributes);
        when(realmModel.getClients()).thenReturn(Collections.singletonList(clientModel));
        when(realmModel.getSmtpConfig()).thenReturn(attributes);
        when(keycloakSession.realms()).thenReturn(realmProvider);
        when(keycloakSession.getProvider(EmailSenderProvider.class)).thenReturn(fakeEmailSenderProvider);

        scheduledTask.run(keycloakSession);

        assertNull(fakeEmailSenderProvider.getConfig());
        assertNull(fakeEmailSenderProvider.getAddress());
        assertNull(fakeEmailSenderProvider.getSubject());
        assertNull(fakeEmailSenderProvider.getTextBody());
        assertNull(fakeEmailSenderProvider.getHtmlBody());
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
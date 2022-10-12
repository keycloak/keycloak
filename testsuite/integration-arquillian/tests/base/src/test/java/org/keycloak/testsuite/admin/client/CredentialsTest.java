/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.admin.client;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.PemUtils;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.testsuite.util.AdminEventPaths;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class CredentialsTest extends AbstractClientTest {

    private ClientResource accountClient;
    private String accountClientDbId;

    @Before
    public void init() {
        accountClient = findClientResourceById("account");
        accountClientDbId = accountClient.toRepresentation().getId();
    }

    @Test
    public void testGetAndRegenerateSecret() {
        CredentialRepresentation oldCredential = accountClient.getSecret();
        CredentialRepresentation newCredential = accountClient.generateNewSecret();

        CredentialRepresentation secretRep = new CredentialRepresentation();
        secretRep.setType(CredentialRepresentation.SECRET);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.ACTION, AdminEventPaths.clientGenerateSecretPath(accountClientDbId), secretRep, ResourceType.CLIENT);

        assertNotNull(oldCredential);
        assertNotNull(newCredential);
        assertNotEquals(newCredential.getValue(), oldCredential.getValue());
        assertEquals(newCredential.getValue(), accountClient.getSecret().getValue());
    }

    @Test
    public void testGetAndRegenerateRegistrationAccessToken() {
        ClientRepresentation rep = accountClient.toRepresentation();
        String oldToken = rep.getRegistrationAccessToken();
        String newToken = accountClient.regenerateRegistrationAccessToken().getRegistrationAccessToken();
        assertNull(oldToken); // registration access token not saved in ClientRep
        assertNotNull(newToken); // it's only available via regenerateRegistrationAccessToken()
        assertNull(accountClient.toRepresentation().getRegistrationAccessToken());

        // Test event
        ClientRepresentation testedRep = new ClientRepresentation();
        testedRep.setClientId(rep.getClientId());
        testedRep.setRegistrationAccessToken(newToken);
        assertAdminEvents.assertEvent(getRealmId(), OperationType.ACTION, AdminEventPaths.clientRegenerateRegistrationAccessTokenPath(accountClientDbId), testedRep, ResourceType.CLIENT);
    }

    @Test
    public void testGetCertificateResource() {
        ClientAttributeCertificateResource certRsc = accountClient.getCertficateResource("jwt.credential");
        CertificateRepresentation cert = certRsc.generate();
        CertificateRepresentation certFromGet = certRsc.getKeyInfo();
        assertEquals(cert.getCertificate(), certFromGet.getCertificate());
        assertEquals(cert.getPrivateKey(), certFromGet.getPrivateKey());

        assertAdminEvents.assertEvent(getRealmId(), OperationType.ACTION, AdminEventPaths.clientCertificateGenerateSecretPath(accountClientDbId, "jwt.credential"), cert, ResourceType.CLIENT);
    }

    @Test
    public void testUploadKeyAndCertificate() throws Exception {
        String privateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCFI44L/ld7Pejv" +
                "mWvdNNi6f7a7RrhwXmnlS/XRSjCsztjD6nUZhweD6jZQ3XdvEcH+V5+olnRaclCj" +
                "pyP8lwy4XXLhaUdLDDSvwvS7foUqTxS7sw6oLT4A6LZTMYhyFgzme4Pf4D2DV2zb" +
                "47/J0osqqILqAkIomn1vdGzx7IlS3T+2SFMAnH4takJuxYcUSdTYluXb1JLgp51h" +
                "H9F0fls3OXgvRIAdLWixYj7EhCJVjJucZWoWMl5D3TTqmY4kZ601puWOLpPGXLA/" +
                "ymz03/+7e3EF4qVWrMII3WItZCtv80cDoYNUMFhV/6pr3Xfny8VdEelakQ4Z+0Hq" +
                "dnMcDMS7AgMBAAECggEAdvkiwkzyV8snEJUE8pN66I2f+RJlVuTHtIOAkxm9DW95" +
                "/PjS05D6gkMVboW5aKNxu5sJrVlleD2XGecOHXXNhCLdudieQM4Tdsy8bZ/N19T7" +
                "LhAAFQD4208ZPpaQ24Ig3QWa/sAft/Kz8HtAs1VVzzTuZ7bE8Au9848Sm81HmaeO" +
                "q4JM58PNuHlTqEYVy4udl1BsIraEblHwNcTaku+hrSLVwwyBVpZDmZVKDQ3cbRDl" +
                "kXxgkRqwB4lQIaU8WAnjJJ8awpJRww4NDMeaq8vMCsmYWBbpRqHQKCp50FO6ObMn" +
                "oXB9vMZHTCmTTxx/kE/Oz0WXCRrXQhMDiHhDOUGe4QKBgQDxMVlarjZuIdpmajQ1" +
                "VUSiW0gOXVyLmbbGvvJTpMx+uwYoUWKzMI9a4pwrknxK+bQ/0f2+VWkIdn4lV8Qa" +
                "e97X/BUH/oTSVjpzPi4lNXi3gXVAk3mP2fJGtLSL3MMQhGA7ia8P1mWb868cR1Oa" +
                "1WD3txtLida9EA90HYNpnTDRXwKBgQCNUABvlDompvldPlkzUJzpLc/MEADsYZzd" +
                "GrK6DG2zlYRF5Fd00hOhyP/vVzRVn6oL34S9XyKb/tA+Xk1tGIjaneg+qpXkGuT8" +
                "xV8MuG7+aRrBbSaU+24GRkhETT95dMY30OrrK/Gj13qTCRrtFbUIFnGLX8zvyEoY" +
                "Idczu6a+JQKBgQDhwu+AkdEhBU8IziBQS6AS2J5506nsgdUz80egQ4GmnikFVCiu" +
                "0aVdzP+hSVxOUa0Gj+iTYB8QbNlm2uAUah4MzUhiUzrAJzZO6MUIzyRPoD3AVEpU" +
                "/AxrYnoc8Gcl7sKk+BuFF4nEkDQ4pAr9wv0g4aQK8Nqv3XiZl4aKNTicwQKBgCpk" +
                "vsy6jjt8SkET7FS9sWUu7jEM+AnyOsoGMWopQlvm1hjVqPsN7v/ROh7GbZfmX3BS" +
                "bRMW0F42MqkTR20cOCpFxx/Ns0wMWXgPijVZu0qto2413aZCS76sJRAAkLNSOgkS" +
                "rvdAZqy9fzNPPvmzUm0LtqKWaffhgYD93f7SI/LVAoGAded5aklVxD0uTfgOdItk" +
                "sr5bWxMrzEtMlLLLdTQXziv6Kvxu+w/0RClkjQ3HcOq8Xj9keYrZx9LEZIqbhWFl" +
                "8bQtgKq1KZocStkloskDzEkQuSQQqP9lSqRn7jOFjtEzDJm2ZdzG9pH+nNFlnBtN" +
                "Di55kopQiwrYBodOxIIYMyo=";
        String certificate = "MIICnTCCAYUCBgFPPLDaTzANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdjbGllbnQxMB4XDTE1MDgxNzE3MjI0N1oXDTI1MDgxNzE3MjQyN1owEjEQMA4GA1UEAwwHY2xpZW50MTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIUjjgv+V3s96O+Za9002Lp/trtGuHBeaeVL9dFKMKzO2MPqdRmHB4PqNlDdd28Rwf5Xn6iWdFpyUKOnI/yXDLhdcuFpR0sMNK/C9Lt+hSpPFLuzDqgtPgDotlMxiHIWDOZ7g9/gPYNXbNvjv8nSiyqoguoCQiiafW90bPHsiVLdP7ZIUwCcfi1qQm7FhxRJ1NiW5dvUkuCnnWEf0XR+Wzc5eC9EgB0taLFiPsSEIlWMm5xlahYyXkPdNOqZjiRnrTWm5Y4uk8ZcsD/KbPTf/7t7cQXipVaswgjdYi1kK2/zRwOhg1QwWFX/qmvdd+fLxV0R6VqRDhn7Qep2cxwMxLsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAKE6OA46sf20bz8LZPoiNsqRwBUDkaMGXfnob7s/hJZIIwDEx0IAQ3uKsG7q9wb+aA6s+v7S340zb2k3IxuhFaHaZpAd4CyR5cn1FHylbzoZ7rI/3ASqHDqpljdJaFqPH+m7nZWtyDvtZf+gkZ8OjsndwsSBK1d/jMZPp29qYbl1+XfO7RCp/jDqro/R3saYFaIFiEZPeKn1hUJn6BO48vxH1xspSu9FmlvDOEAOz4AuM58z4zRMP49GcFdCWr1wkonJUHaSptJaQwmBwLFUkCbE5I1ixGMb7mjEud6Y5jhfzJiZMo2U8RfcjNbrN0diZl3jB6LQIwESnhYSghaTjNQ==";
        String certificate2 = "MIICnTCCAYUCBgFPPQDGxTANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdjbGllbnQxMB4XDTE1MDgxNzE4NTAwNVoXDTI1MDgxNzE4NTE0NVowEjEQMA4GA1UEAwwHY2xpZW50MTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMMw3PaBffWxgS2PYSDDBp6As+cNvv9kt2C4f/RDAGmvSIHPFev9kuQiKs3Oaws3ZsV4JG3qHEuYgnh9W4vfe3DwNwtD1bjL5FYBhPBFTw0lAQECYxaBHnkjHwUKp957FqdSPPICm3LjmTcEdlH+9dpp9xHCMbbiNiWDzWI1xSxC8Fs2d0hwz1sd+Q4QeTBPIBWcPM+ICZtNG5MN+ORfayu4X+Me5d0tXG2fQO//rAevk1i5IFjKZuOjTwyKB5SJIY4b8QTeg0g/50IU7Ht00Pxw6CK02dHS+FvXHasZlD3ckomqCDjStTBWdhJo5dST0CbOqalkkpLlCCbGA1yEQRsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAUIMeJ+EAo8eNpCG/nXImacjrKakbFnZYBGD/gqeTGaZynkX+jgBSructTHR83zSH+yELEhsAy+3BfK4EEihp+PEcRnK2fASVkHste8AQ7rlzC+HGGirlwrVhWCdizNUCGK80DE537IZ7nmZw6LFG9P5/Q2MvCsOCYjRUvMkukq6TdXBXR9tETwZ+0gpSfsOxjj0ZF7ftTRUSzx4rFfcbM9fRNdVizdOuKGc8HJPA5lLOxV6CyaYIvi3y5RlQI1OHeS34lE4w9CNPRFa/vdxXvN7ClyzA0HMFNWxBN7pC/Ht/FbhSvaAagJBHg+vCrcY5C26Oli7lAglf/zZrwUPs0w==";

        ClientAttributeCertificateResource certRsc = accountClient.getCertficateResource("jwt.credential");

        // Upload privateKey and certificate as JKS store
        MultipartFormDataOutput keyCertForm = new MultipartFormDataOutput();
        keyCertForm.addFormData("keystoreFormat", "JKS", MediaType.TEXT_PLAIN_TYPE);
        keyCertForm.addFormData("keyAlias", "clientkey", MediaType.TEXT_PLAIN_TYPE);
        keyCertForm.addFormData("keyPassword", "keypass", MediaType.TEXT_PLAIN_TYPE);
        keyCertForm.addFormData("storePassword", "storepass", MediaType.TEXT_PLAIN_TYPE);

        URL idpMeta = getClass().getClassLoader().getResource("client-auth-test/keystore-client1.jks");
        byte [] content = Files.readAllBytes(Paths.get(idpMeta.toURI()));
        keyCertForm.addFormData("file", content, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        CertificateRepresentation cert = certRsc.uploadJks(keyCertForm);

        // Returned cert is not the new state but rather what was extracted from inputs
        assertNotNull("cert not null", cert);
        assertEquals("cert properly extracted", certificate, cert.getCertificate());
        assertEquals("privateKey properly extracted", privateKey, cert.getPrivateKey());

        // Get the certificate - to make sure cert was properly updated
        cert = certRsc.getKeyInfo();
        assertEquals("cert properly set", certificate, cert.getCertificate());
        assertEquals("privateKey properly set", privateKey, cert.getPrivateKey());

        // Upload a different certificate via /upload-certificate, privateKey should be nullified
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("keystoreFormat", "Certificate PEM", MediaType.TEXT_PLAIN_TYPE);
        form.addFormData("file", certificate2.getBytes(Charset.forName("ASCII")), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        cert = certRsc.uploadJksCertificate(form);
        assertNotNull("cert not null", cert);
        assertEquals("cert properly extracted", certificate2, cert.getCertificate());
        assertNull("privateKey not included", cert.getPrivateKey());

        // Get the certificate - to make sure cert was properly updated, and privateKey is null
        cert = certRsc.getKeyInfo();
        assertEquals("cert properly set", certificate2, cert.getCertificate());
        assertNull("privateKey nullified", cert.getPrivateKey());

        // Re-upload the private key
        certRsc.uploadJks(keyCertForm);

        // Upload certificate as PEM via /upload - nullifies the private key
        form = new MultipartFormDataOutput();
        form.addFormData("keystoreFormat", "Certificate PEM", MediaType.TEXT_PLAIN_TYPE);
        form.addFormData("file", certificate2.getBytes(Charset.forName("ASCII")), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        cert = certRsc.uploadJks(form);
        assertNotNull("cert not null", cert);
        assertEquals("cert properly extracted", certificate2, cert.getCertificate());
        assertNull("privateKey not included", cert.getPrivateKey());

        // Get the certificate again - to make sure cert is set, and privateKey is null
        cert = certRsc.getKeyInfo();
        assertEquals("cert properly set", certificate2, cert.getCertificate());
        assertNull("privateKey nullified", cert.getPrivateKey());

        // Upload certificate with header - should be stored without header
        form = new MultipartFormDataOutput();
        form.addFormData("keystoreFormat", "Certificate PEM", MediaType.TEXT_PLAIN_TYPE);

        String certificate2WithHeaders = PemUtils.BEGIN_CERT + "\n" + certificate2 + "\n" + PemUtils.END_CERT;

        form.addFormData("file", certificate2WithHeaders.getBytes(Charset.forName("ASCII")), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        cert = certRsc.uploadJks(form);
        assertNotNull("cert not null", cert);
        assertEquals("cert properly extracted", certificate2, cert.getCertificate());
        assertNull("privateKey not included", cert.getPrivateKey());

        // Get the certificate again - to make sure cert is set, and privateKey is null
        cert = certRsc.getKeyInfo();
        assertEquals("cert properly set", certificate2, cert.getCertificate());
        assertNull("privateKey nullified", cert.getPrivateKey());
    }

    @Test
    public void testDownloadKeystore() throws Exception {
        ClientAttributeCertificateResource certRsc = accountClient.getCertficateResource("jwt.credential");

        // generate a key pair first
        CertificateRepresentation certrep = certRsc.generate();
        log.warn("Cert Rep Impl: " + certRsc.getClass().getName());

        // download the key and certificate
        KeyStoreConfig config = new KeyStoreConfig();
        config.setFormat("JKS");
        config.setKeyAlias("alias");
        config.setKeyPassword("keyPass");
        config.setStorePassword("storePass");
        byte[] result = certRsc.getKeystore(config);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new ByteArrayInputStream(result), "storePass".toCharArray());
        Key key = keyStore.getKey("alias", "keyPass".toCharArray());
        Certificate cert = keyStore.getCertificate("alias");

        assertTrue("Certificat is X509", cert instanceof X509Certificate);
        String keyPem = KeycloakModelUtils.getPemFromKey(key);
        String certPem = KeycloakModelUtils.getPemFromCertificate((X509Certificate) cert);

        assertEquals("cert match", certrep.getCertificate(), certPem);

        // This test fails if key is not RAW as certrep's key is RAW
        if(key.getFormat().equals("RAW")){
            assertEquals("key match", certrep.getPrivateKey(), keyPem);
        }
    }

    @Test
    public void testGenerateAndDownloadKeystore() throws Exception {
        ClientAttributeCertificateResource certRsc = accountClient.getCertficateResource("jwt.credential");

        // generate a key pair first
        CertificateRepresentation firstcert = certRsc.generate();

        KeyStoreConfig config = new KeyStoreConfig();
        config.setFormat("JKS");
        config.setKeyAlias("alias");
        config.setKeyPassword("keyPass");
        config.setStorePassword("storePass");
        byte[] result = certRsc.generateAndGetKeystore(config);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new ByteArrayInputStream(result), "storePass".toCharArray());
        Key key = keyStore.getKey("alias", "keyPass".toCharArray());
        Certificate cert = keyStore.getCertificate("alias");

        assertTrue("Certificat is X509", cert instanceof X509Certificate);
        String keyPem = KeycloakModelUtils.getPemFromKey(key);
        String certPem = KeycloakModelUtils.getPemFromCertificate((X509Certificate) cert);

        assertNotEquals("new key generated", firstcert.getPrivateKey(), keyPem);
        assertNotEquals("new cert generated", firstcert.getCertificate(), certPem);
    }
}

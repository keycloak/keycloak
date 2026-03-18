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

package org.keycloak.tests.admin.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectCryptoHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.crypto.CryptoHelper;
import org.keycloak.testframework.crypto.KeystoreInfo;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
public class CredentialsTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectClient(attachTo = "account")
    ManagedClient managedClient;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectCryptoHelper
    CryptoHelper cryptoHelper;

    @TempDir
    public static File folder;

    @BeforeAll
    public static void init() {
        if(!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(new DefaultCryptoProvider());
        }
    }

    @Test
    public void testGetAndRegenerateSecret() {
        CredentialRepresentation oldCredential = managedClient.admin().getSecret();
        CredentialRepresentation newCredential = managedClient.admin().generateNewSecret();

        CredentialRepresentation secretRep = new CredentialRepresentation();
        secretRep.setType(CredentialRepresentation.SECRET);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.clientGenerateSecretPath(managedClient.getId()), secretRep, ResourceType.CLIENT);

        assertNotNull(oldCredential);
        assertNotNull(newCredential);
        assertNotEquals(newCredential.getValue(), oldCredential.getValue());
        assertEquals(newCredential.getValue(), managedClient.admin().getSecret().getValue());
    }

    @Test
    public void testGetAndRegenerateRegistrationAccessToken() {
        ClientRepresentation rep = managedClient.admin().toRepresentation();
        String oldToken = rep.getRegistrationAccessToken();
        String newToken = managedClient.admin().regenerateRegistrationAccessToken().getRegistrationAccessToken();
        assertNull(oldToken); // registration access token not saved in ClientRep
        assertNotNull(newToken); // it's only available via regenerateRegistrationAccessToken()
        assertNull(managedClient.admin().toRepresentation().getRegistrationAccessToken());

        // Test event
        ClientRepresentation testedRep = new ClientRepresentation();
        testedRep.setClientId(rep.getClientId());
        testedRep.setRegistrationAccessToken(newToken);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.clientRegenerateRegistrationAccessTokenPath(managedClient.getId()), testedRep, ResourceType.CLIENT);
    }

    @Test
    public void testGetCertificateResource() {
        ClientAttributeCertificateResource certRsc = managedClient.admin().getCertficateResource("jwt.credential");
        CertificateRepresentation cert = certRsc.generate();
        CertificateRepresentation certFromGet = certRsc.getKeyInfo();
        assertEquals(cert.getCertificate(), certFromGet.getCertificate());
        assertEquals(cert.getPrivateKey(), certFromGet.getPrivateKey());

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.clientCertificateGenerateSecretPath(managedClient.getId(), "jwt.credential"), cert, ResourceType.CLIENT);
    }

    @Test
    public void testUploadKeyAndCertificate() throws Exception {
        String certificate2 = "MIICnTCCAYUCBgFPPQDGxTANBgkqhkiG9w0BAQsFADASMRAwDgYDVQQDDAdjbGllbnQxMB4XDTE1MDgxNzE4NTAwNVoXDTI1MDgxNzE4NTE0NVowEjEQMA4GA1UEAwwHY2xpZW50MTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMMw3PaBffWxgS2PYSDDBp6As+cNvv9kt2C4f/RDAGmvSIHPFev9kuQiKs3Oaws3ZsV4JG3qHEuYgnh9W4vfe3DwNwtD1bjL5FYBhPBFTw0lAQECYxaBHnkjHwUKp957FqdSPPICm3LjmTcEdlH+9dpp9xHCMbbiNiWDzWI1xSxC8Fs2d0hwz1sd+Q4QeTBPIBWcPM+ICZtNG5MN+ORfayu4X+Me5d0tXG2fQO//rAevk1i5IFjKZuOjTwyKB5SJIY4b8QTeg0g/50IU7Ht00Pxw6CK02dHS+FvXHasZlD3ckomqCDjStTBWdhJo5dST0CbOqalkkpLlCCbGA1yEQRsCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAUIMeJ+EAo8eNpCG/nXImacjrKakbFnZYBGD/gqeTGaZynkX+jgBSructTHR83zSH+yELEhsAy+3BfK4EEihp+PEcRnK2fASVkHste8AQ7rlzC+HGGirlwrVhWCdizNUCGK80DE537IZ7nmZw6LFG9P5/Q2MvCsOCYjRUvMkukq6TdXBXR9tETwZ+0gpSfsOxjj0ZF7ftTRUSzx4rFfcbM9fRNdVizdOuKGc8HJPA5lLOxV6CyaYIvi3y5RlQI1OHeS34lE4w9CNPRFa/vdxXvN7ClyzA0HMFNWxBN7pC/Ht/FbhSvaAagJBHg+vCrcY5C26Oli7lAglf/zZrwUPs0w==";

        ClientAttributeCertificateResource certRsc = managedClient.admin().getCertficateResource("jwt.credential");

        KeystoreUtil.KeystoreFormat preferredKeystoreType = KeystoreUtil.KeystoreFormat.valueOf(adminClient.serverInfo().getInfo().getCryptoInfo().getSupportedKeystoreTypes().get(0));

        // Generate keystore file and upload privateKey and certificate from it as JKS store (or eventually PKCS12 or BCFKS store according to which one is preferred type)
        KeystoreInfo generatedKeystore = cryptoHelper.keystore().generateKeystore(folder, preferredKeystoreType, "clientkey", "storepass", "keypass");
        MultipartFormDataOutput keyCertForm = new MultipartFormDataOutput();

        keyCertForm.addFormData("keystoreFormat", preferredKeystoreType.toString(), MediaType.TEXT_PLAIN_TYPE);
        keyCertForm.addFormData("keyAlias", "clientkey", MediaType.TEXT_PLAIN_TYPE);
        keyCertForm.addFormData("keyPassword", "keypass", MediaType.TEXT_PLAIN_TYPE);
        keyCertForm.addFormData("storePassword", "storepass", MediaType.TEXT_PLAIN_TYPE);

        FileInputStream fs = new FileInputStream(generatedKeystore.getKeystoreFile());
        byte [] content = fs.readAllBytes();
        fs.close();
        keyCertForm.addFormData("file", content, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        CertificateRepresentation cert = certRsc.uploadJks(keyCertForm);

        // Returned cert is not the new state but rather what was extracted from inputs
        assertNotNull(cert, "cert not null");
        assertEquals(generatedKeystore.getCertificateInfo().getCertificate(), cert.getCertificate(), "cert properly extracted");
        assertEquals(generatedKeystore.getCertificateInfo().getPrivateKey(), cert.getPrivateKey(), "privateKey properly extracted");

        // Get the certificate - to make sure cert was properly updated
        cert = certRsc.getKeyInfo();
        assertEquals(generatedKeystore.getCertificateInfo().getCertificate(), cert.getCertificate(), "cert properly set");
        assertEquals(generatedKeystore.getCertificateInfo().getPrivateKey(), cert.getPrivateKey(), "privateKey properly set");

        // Upload a different certificate via /upload-certificate, privateKey should be nullified
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("keystoreFormat", "Certificate PEM", MediaType.TEXT_PLAIN_TYPE);
        form.addFormData("file", certificate2.getBytes(StandardCharsets.US_ASCII), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        cert = certRsc.uploadJksCertificate(form);
        assertNotNull(cert, "cert not null");
        assertEquals(certificate2, cert.getCertificate(), "cert properly extracted");
        assertNull(cert.getPrivateKey(), "privateKey not included");

        // Get the certificate - to make sure cert was properly updated, and privateKey is null
        cert = certRsc.getKeyInfo();
        assertEquals(certificate2, cert.getCertificate(), "cert properly set");
        assertNull(cert.getPrivateKey(), "privateKey nullified");

        // Re-upload the private key
        certRsc.uploadJks(keyCertForm);

        // Upload certificate as PEM via /upload - nullifies the private key
        form = new MultipartFormDataOutput();
        form.addFormData("keystoreFormat", "Certificate PEM", MediaType.TEXT_PLAIN_TYPE);
        form.addFormData("file", certificate2.getBytes(StandardCharsets.US_ASCII), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        cert = certRsc.uploadJks(form);
        assertNotNull(cert, "cert not null");
        assertEquals(certificate2, cert.getCertificate(), "cert properly extracted");
        assertNull(cert.getPrivateKey(), "privateKey not included");

        // Get the certificate again - to make sure cert is set, and privateKey is null
        cert = certRsc.getKeyInfo();
        assertEquals(certificate2, cert.getCertificate(), "cert properly set");
        assertNull(cert.getPrivateKey(), "privateKey nullified");

        // Upload certificate with header - should be stored without header
        form = new MultipartFormDataOutput();
        form.addFormData("keystoreFormat", "Certificate PEM", MediaType.TEXT_PLAIN_TYPE);

        String certificate2WithHeaders = PemUtils.BEGIN_CERT + "\n" + certificate2 + "\n" + PemUtils.END_CERT;

        form.addFormData("file", certificate2WithHeaders.getBytes(StandardCharsets.US_ASCII), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        cert = certRsc.uploadJks(form);
        assertNotNull(cert, "cert not null");
        assertEquals(certificate2, cert.getCertificate(),"cert properly extracted");
        assertNull(cert.getPrivateKey(), "privateKey not included");

        // Get the certificate again - to make sure cert is set, and privateKey is null
        cert = certRsc.getKeyInfo();
        assertEquals(certificate2, cert.getCertificate(), "cert properly set");
        assertNull(cert.getPrivateKey(), "privateKey nullified");
    }

    @Test
    public void testDownloadKeystore() throws Exception {
        ClientAttributeCertificateResource certRsc = managedClient.admin().getCertficateResource("jwt.credential");

        // generate a key pair first
        CertificateRepresentation certrep = certRsc.generate();

        KeystoreUtil.KeystoreFormat preferredKeystoreType = KeystoreUtil.KeystoreFormat.valueOf(adminClient.serverInfo().getInfo().getCryptoInfo().getSupportedKeystoreTypes().get(0));

        // download the key and certificate
        KeyStoreConfig config = new KeyStoreConfig();
        config.setFormat(preferredKeystoreType.toString());
        config.setKeyAlias("alias");
        config.setKeyPassword("keyPass");
        config.setStorePassword("storePass");
        byte[] result = certRsc.getKeystore(config);

        KeyStore keyStore = CryptoIntegration.getProvider().getKeyStore(preferredKeystoreType);
        keyStore.load(new ByteArrayInputStream(result), "storePass".toCharArray());
        Key key = keyStore.getKey("alias", "keyPass".toCharArray());
        Certificate cert = keyStore.getCertificate("alias");

        assertInstanceOf(X509Certificate.class, cert, "Certificat is X509");
        String keyPem = KeycloakModelUtils.getPemFromKey(key);
        String certPem = KeycloakModelUtils.getPemFromCertificate((X509Certificate) cert);

        assertEquals(certrep.getPrivateKey(), keyPem, "key match");
        assertEquals(certrep.getCertificate(), certPem, "cert match");
    }

    @Test
    public void testGenerateAndDownloadKeystore() throws Exception {
        ClientAttributeCertificateResource certRsc = managedClient.admin().getCertficateResource("jwt.credential");

        // generate a key pair first
        CertificateRepresentation firstcert = certRsc.generate();

        KeystoreUtil.KeystoreFormat preferredKeystoreType = KeystoreUtil.KeystoreFormat.valueOf(adminClient.serverInfo().getInfo().getCryptoInfo().getSupportedKeystoreTypes().get(0));

        KeyStoreConfig config = new KeyStoreConfig();
        config.setFormat(preferredKeystoreType.toString());
        config.setKeyAlias("alias");
        config.setKeyPassword("keyPass");
        config.setStorePassword("storePass");
        config.setKeySize(4096);
        config.setValidity(3);
        byte[] result = certRsc.generateAndGetKeystore(config);
        KeyStore keyStore = CryptoIntegration.getProvider().getKeyStore(preferredKeystoreType);
        keyStore.load(new ByteArrayInputStream(result), "storePass".toCharArray());
        Key key = keyStore.getKey("alias", "keyPass".toCharArray());
        Certificate cert = keyStore.getCertificate("alias");

        assertInstanceOf(X509Certificate.class, cert, "Certificat is X509");
        String keyPem = KeycloakModelUtils.getPemFromKey(key);
        String certPem = KeycloakModelUtils.getPemFromCertificate((X509Certificate) cert);

        assertNotEquals(firstcert.getPrivateKey(), keyPem, "new key generated");
        assertNotEquals(firstcert.getCertificate(), certPem, "new cert generated");
    }
}

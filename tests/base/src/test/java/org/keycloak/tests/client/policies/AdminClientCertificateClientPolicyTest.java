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
package org.keycloak.tests.client.policies;

import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectRequestExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AdminClientCertificateClientPolicyTest extends AbstractClientPoliciesTest {

    @InjectRealm
    ManagedRealm realm;

    @BeforeEach
    public void initCrypto() {
        if (!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(new DefaultCryptoProvider());
        }
    }

    @Test
    public void rejectCertificateGenerate() throws Exception {
        ClientAttributeCertificateResource certificates = createClient("certificate-generate-client")
                .getCertficateResource("jwt.credential");
        CertificateRepresentation original = certificates.generate();

        setupRejectingPolicy();

        Assertions.assertThrows(BadRequestException.class, certificates::generate);
        assertCertificateUnchanged(certificates, original);
    }

    @Test
    public void rejectCertificateUpload() throws Exception {
        ClientAttributeCertificateResource certificates = createClient("certificate-upload-client")
                .getCertficateResource("jwt.credential");
        CertificateRepresentation original = certificates.generate();
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("keystoreFormat", "Certificate PEM", MediaType.TEXT_PLAIN_TYPE);
        form.addFormData("file", original.getCertificate().getBytes(StandardCharsets.US_ASCII), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        setupRejectingPolicy();

        Assertions.assertThrows(BadRequestException.class, () -> certificates.uploadJksCertificate(form));
        assertCertificateUnchanged(certificates, original);
    }

    @Test
    public void rejectCertificateUploadJks() throws Exception {
        ClientAttributeCertificateResource certificates = createClient("certificate-upload-jks-client")
                .getCertficateResource("jwt.credential");
        CertificateRepresentation original = certificates.generate();
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("keystoreFormat", "Certificate PEM", MediaType.TEXT_PLAIN_TYPE);
        form.addFormData("file", original.getCertificate().getBytes(StandardCharsets.US_ASCII), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        setupRejectingPolicy();

        Assertions.assertThrows(BadRequestException.class, () -> certificates.uploadJks(form));
        assertCertificateUnchanged(certificates, original);
    }

    @Test
    public void rejectCertificateGenerateAndDownload() throws Exception {
        ClientAttributeCertificateResource certificates = createClient("certificate-gen-download-client")
                .getCertficateResource("jwt.credential");
        CertificateRepresentation original = certificates.generate();

        KeyStoreConfig config = new KeyStoreConfig();
        config.setFormat("PKCS12");
        config.setKeyAlias("alias");
        config.setKeyPassword("keyPass");
        config.setStorePassword("storePass");

        setupRejectingPolicy();

        Assertions.assertThrows(BadRequestException.class, () -> certificates.generateAndGetKeystore(config));
        assertCertificateUnchanged(certificates, original);
    }

    private ClientResource createClient(String clientId) {
        ClientRepresentation client = ClientBuilder.create(generateSuffixedName(clientId))
                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .publicClient(false)
                .build();

        try (Response response = realm.admin().clients().create(client)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.clients().delete(id));
            return realm.admin().clients().get(id);
        }
    }

    private void assertCertificateUnchanged(ClientAttributeCertificateResource certificates, CertificateRepresentation expected) {
        CertificateRepresentation actual = certificates.getKeyInfo();
        Assertions.assertEquals(expected.getCertificate(), actual.getCertificate());
        Assertions.assertEquals(expected.getPrivateKey(), actual.getPrivateKey());
    }

    private void setupRejectingPolicy() throws Exception {
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }
}

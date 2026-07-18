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
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.KeyStoreConfig;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientProtocolCondition;
import org.keycloak.services.clientpolicy.condition.ClientProtocolConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.context.admin.ClientCertificateUpdateContext;
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
    public void applyTargetClientProtocolCondition() throws Exception {
        ClientAttributeCertificateResource oidcCertificates = createClient(
                "certificate-target-oidc-client", OIDCLoginProtocol.LOGIN_PROTOCOL)
                .getCertficateResource("jwt.credential");
        ClientAttributeCertificateResource samlCertificates = createClient(
                "certificate-target-saml-client", SamlProtocol.LOGIN_PROTOCOL)
                .getCertficateResource("jwt.credential");
        CertificateRepresentation original = oidcCertificates.generate();

        setupRejectingProtocolPolicy(OIDCLoginProtocol.LOGIN_PROTOCOL);

        Assertions.assertThrows(BadRequestException.class, oidcCertificates::generate);
        assertCertificateUnchanged(oidcCertificates, original);
        Assertions.assertNotNull(samlCertificates.generate().getCertificate());
    }

    @Test
    public void applyAuthenticatedUpdaterCondition() throws Exception {
        ClientAttributeCertificateResource certificates = createClient("certificate-updater-client")
                .getCertficateResource("jwt.credential");
        CertificateRepresentation original = certificates.generate();
        setupRejectingUpdaterPolicy();

        Assertions.assertThrows(BadRequestException.class, certificates::generate);
        assertCertificateUnchanged(certificates, original);
    }

    @Test
    public void omitRawJwksFromPolicyContext() {
        CertificateRepresentation proposed = new CertificateRepresentation();
        proposed.setJwks("{\"keys\":[{\"kty\":\"RSA\",\"d\":\"private-value\"}]}");
        proposed.setPrivateKey("private-key");
        proposed.setPublicKey("public-key");
        proposed.setKid("kid");

        CertificateRepresentation sanitized = new ClientCertificateUpdateContext(null, "jwt.credential", proposed, null)
                .getProposedCertificate();
        Assertions.assertNull(sanitized.getJwks());
        Assertions.assertNull(sanitized.getPrivateKey());
        Assertions.assertEquals("public-key", sanitized.getPublicKey());
        Assertions.assertEquals("kid", sanitized.getKid());
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
        return createClient(clientId, OIDCLoginProtocol.LOGIN_PROTOCOL);
    }

    private ClientResource createClient(String clientId, String protocol) {
        ClientRepresentation client = ClientBuilder.create(generateSuffixedName(clientId))
                .protocol(protocol)
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

    private void setupRejectingProtocolPolicy(String protocol) throws Exception {
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                ClientProtocolConditionFactory.PROVIDER_ID, new ClientProtocolCondition.Configuration(protocol));
    }

    private void setupRejectingUpdaterPolicy() throws Exception {
        ClientUpdaterContextCondition.Configuration configuration = new ClientUpdaterContextCondition.Configuration();
        configuration.setUpdateClientSource(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER));
        setupPolicy(realm, RejectRequestExecutorFactory.PROVIDER_ID, null,
                ClientUpdaterContextConditionFactory.PROVIDER_ID, configuration);
    }
}

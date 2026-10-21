package org.keycloak.tests.broker.saml;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.tests.saml.SamlBrowserlessBrokerClient;
import org.keycloak.tests.saml.SamlResponseDocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.PROXY_RESTRICTION_VALIDATION;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.VALIDATE_SIGNATURE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcSamlBrokerTest extends AbstractKcSamlBrokerTest {

    private static final String SIGNING_KEY_NAME = "kc-saml-idp-signing";

    private KeyPair signingKeyPair;
    private X509Certificate signingCertificate;

    @BeforeEach
    void initSigningKeys() {
        CryptoIntegration.init(getClass().getClassLoader());
        signingKeyPair = KeyUtils.generateRsaKeyPair(2048);
        signingCertificate = CertificateUtils.generateV1SelfSignedCertificate(signingKeyPair, "kc-saml-idp");
    }

    @Test
    public void testHolderOfKeyOnlySubjectConfirmationIsRejected() throws Exception {
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> {
                                assertBearerConfirmationIssuedBy(response);
                                response.removeSubjectConfirmations()
                                        .addSubjectConfirmation(SamlResponseDocument.HOLDER_OF_KEY_METHOD);
                            })
                            .submit());
        }

        assertTrue(getConsumerRealm().admin().users().search(getUserLogin(), true).isEmpty());
    }

    @Test
    public void testUnknownSubjectConfirmationMethodIsRejected() throws Exception {
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> {
                                assertBearerConfirmationIssuedBy(response);
                                response.removeSubjectConfirmations()
                                        .addSubjectConfirmation("urn:example:unsupported");
                            })
                            .submit());
        }

        assertTrue(getConsumerRealm().admin().users().search(getUserLogin(), true).isEmpty());
    }

    @Test
    public void testMissingSubjectConfirmationIsRejected() throws Exception {
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> {
                                assertBearerConfirmationIssuedBy(response);
                                response.removeSubjectConfirmations();
                            })
                            .submit());
        }

        assertTrue(getConsumerRealm().admin().users().search(getUserLogin(), true).isEmpty());
    }

    @Test
    public void testBearerSubjectConfirmationWithoutDataIsRejected() throws Exception {
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> {
                                assertBearerConfirmationIssuedBy(response);
                                response.removeSubjectConfirmations()
                                        .addSubjectConfirmation(SamlResponseDocument.BEARER_METHOD);
                            })
                            .submit());
        }

        assertTrue(getConsumerRealm().admin().users().search(getUserLogin(), true).isEmpty());
    }

    @Test
    public void testBearerAndHolderOfKeySubjectConfirmationIsAccepted() throws Exception {
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.OK.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> {
                                assertBearerConfirmationIssuedBy(response);
                                response.addSubjectConfirmation(SamlResponseDocument.HOLDER_OF_KEY_METHOD);
                            })
                            .submit());
        }
    }

    @Test
    public void testInvalidBearerAndHolderOfKeySubjectConfirmationIsRejected() throws Exception {
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> {
                                assertBearerConfirmationIssuedBy(response);
                                response.expireSubjectConfirmationData(SamlResponseDocument.BEARER_METHOD)
                                        .addSubjectConfirmation(SamlResponseDocument.HOLDER_OF_KEY_METHOD);
                            })
                            .submit());
        }

        assertTrue(getConsumerRealm().admin().users().search(getUserLogin(), true).isEmpty());
    }

    @Test
    public void testProxyRestrictionRejectedByDefault() throws Exception {
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> response.addProxyRestriction(0))
                            .submit());
        }

        assertTrue(getConsumerRealm().admin().users().search(getUserLogin(), true).isEmpty());
    }

    @Test
    public void testProxyRestrictionWithPositiveCountRejectedByDefault() throws Exception {
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> response.addProxyRestriction(5))
                            .submit());
        }

        assertTrue(getConsumerRealm().admin().users().search(getUserLogin(), true).isEmpty());
    }

    @Test
    public void testProxyRestrictionAllowedWhenValidationDisabled() throws Exception {
        setProxyRestrictionValidation(false);
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.OK.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> response.addProxyRestriction(0))
                            .submit());
        } finally {
            setProxyRestrictionValidation(true);
        }
    }

    @Test
    public void testSignedProxyRestrictionRejected() throws Exception {
        enableSignatureValidation();
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> response.addProxyRestriction(0)
                                    .sign(signingKeyPair, SIGNING_KEY_NAME, signingCertificate))
                            .submit());
        }

        assertTrue(getConsumerRealm().admin().users().search(getUserLogin(), true).isEmpty());
    }

    @Test
    public void testSignedResponseAcceptedWithoutProxyRestriction() throws Exception {
        enableSignatureValidation();
        try (SamlBrowserlessBrokerClient broker = new SamlBrowserlessBrokerClient(getConsumerRealm(), SAML_CLIENT_ID_SALES_POST, IDP_SAML_ALIAS)) {
            assertEquals(Response.Status.OK.getStatusCode(),
                    broker.login(getUserLogin(), getUserPassword())
                            .onResponse(response -> response.sign(signingKeyPair, SIGNING_KEY_NAME, signingCertificate))
                            .submit());
        }
    }

    private void enableSignatureValidation() {
        IdentityProviderResource idpResource = getConsumerRealm().admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation idp = idpResource.toRepresentation();
        idp.getConfig().put(VALIDATE_SIGNATURE, Boolean.TRUE.toString());
        idp.getConfig().put(SIGNING_CERTIFICATE_KEY, PemUtils.encodeCertificate(signingCertificate));
        idpResource.update(idp);
    }

    private void setProxyRestrictionValidation(boolean enabled) {
        IdentityProviderResource idpResource = getConsumerRealm().admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation idp = idpResource.toRepresentation();
        idp.getConfig().put(PROXY_RESTRICTION_VALIDATION, String.valueOf(enabled));
        idpResource.update(idp);
    }

    private static void assertBearerConfirmationIssuedBy(SamlResponseDocument response) {
        assertEquals(1, response.getSubjectConfirmationCount(),
                "Expected the identity provider to issue a single SubjectConfirmation");
        assertTrue(response.hasSubjectConfirmationData(SamlResponseDocument.BEARER_METHOD),
                "Expected the identity provider to issue a bearer SubjectConfirmation carrying SubjectConfirmationData");
    }
}

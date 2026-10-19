package org.keycloak.tests.broker.saml;

import jakarta.ws.rs.core.Response;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.tests.saml.SamlBrowserlessBrokerClient;
import org.keycloak.tests.saml.SamlResponseDocument;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcSamlBrokerTest extends AbstractKcSamlBrokerTest {

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

    private static void assertBearerConfirmationIssuedBy(SamlResponseDocument response) {
        assertEquals(1, response.getSubjectConfirmationCount(),
                "Expected the identity provider to issue a single SubjectConfirmation");
        assertTrue(response.hasSubjectConfirmationData(SamlResponseDocument.BEARER_METHOD),
                "Expected the identity provider to issue a bearer SubjectConfirmation carrying SubjectConfirmationData");
    }
}

package org.keycloak.tests.broker.saml;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcSamlBrokerTest extends AbstractKcSamlBrokerTest {

    @Test
    public void testRejectsSamlResponseWithoutSubject() throws Exception {
        assertInvalidSamlResponse(SamlBrokerResponseTampering.Mutation.REMOVE_SUBJECT);
    }

    @Test
    public void testRejectsSamlResponseWithoutBearerSubjectConfirmation() throws Exception {
        assertInvalidSamlResponse(SamlBrokerResponseTampering.Mutation.REMOVE_BEARER_CONFIRMATION);
    }

    @Test
    public void testRejectsBearerSubjectConfirmationWithoutData() throws Exception {
        assertInvalidSamlResponse(SamlBrokerResponseTampering.Mutation.REMOVE_SUBJECT_CONFIRMATION_DATA);
    }

    @Test
    public void testRejectsBearerSubjectConfirmationWithoutNotOnOrAfter() throws Exception {
        assertInvalidSamlResponse(SamlBrokerResponseTampering.Mutation.REMOVE_NOT_ON_OR_AFTER);
    }

    @Test
    public void testRejectsBearerSubjectConfirmationWithoutRecipient() throws Exception {
        assertInvalidSamlResponse(SamlBrokerResponseTampering.Mutation.REMOVE_RECIPIENT);
    }

    private void assertInvalidSamlResponse(SamlBrokerResponseTampering.Mutation mutation) throws Exception {
        SamlBrokerResponseTampering.Result result = SamlBrokerResponseTampering.loginWithMutation(
                oauth, IDP_SAML_ALIAS, getUserLogin(), getUserPassword(), mutation);

        assertEquals(400, result.statusCode(), "Malformed broker SAML response should be rejected");
        assertTrue(result.body().contains("Invalid requester"),
                "Malformed broker SAML response should produce an invalid requester error");
    }
}

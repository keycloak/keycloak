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
package org.keycloak.tests.broker.saml;

import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.tests.saml.SamlClient;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for keycloak#54342: a SAML assertion whose Bearer SubjectConfirmationData Recipient names one
 * IdP-initiated client must not be accepted by the broker for a login targeting a different IdP-initiated client
 * on the same broker, even when the outer Response Destination is absent or forged.
 */
@KeycloakIntegrationTest
public class KcSamlBrokerIdpInitiatedRecipientConfusionTest extends AbstractKcSamlBrokerTest {

    private static final String CLIENT_A_SSO_URL_NAME = "sales-post";
    private static final String CLIENT_B_SSO_URL_NAME = "sales-post-2";
    private static final int ASSERTION_VALIDITY_SECONDS = 3600;

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectEvents(realmRef = "consumer")
    Events events;

    @BeforeEach
    void setUp() {
        // error events of the broker endpoint are not saved by default, so they are invisible to the test events otherwise
        consumerRealm.updateWithCleanup(realm -> realm.enabledEventTypes(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR.name()));
        updateIdp(idp -> idp.getConfig().put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "false"));

        String consumerBaseUrl = consumerRealm.getBaseUrl();
        String consumerServerRoot = consumerBaseUrl.substring(0, consumerBaseUrl.indexOf("/realms/"));
        String clientBBaseUrl = consumerServerRoot + "/sales-post-2";
        ClientRepresentation clientB = ClientBuilder.create(SAML_CLIENT_ID_SALES_POST + "-2")
                .enabled(true)
                .fullScopeEnabled(true)
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .baseUrl(clientBBaseUrl)
                .redirectUris(clientBBaseUrl + "/*")
                .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
                .attribute(SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME, CLIENT_B_SSO_URL_NAME)
                .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, clientBBaseUrl + "/saml")
                .build();
        try (Response response = consumerRealm.admin().clients().create(clientB)) {
            assertEquals(201, response.getStatus(), "client B should be created");
        }
    }

    @Test
    void testIdpInitiatedLoginToClientASucceeds() throws Exception {
        assertAccepted(buildResponse(CLIENT_A_SSO_URL_NAME), CLIENT_A_SSO_URL_NAME, "Login to client A should be accepted");
    }

    @Test
    void testIdpInitiatedLoginToClientBSucceeds() throws Exception {
        assertAccepted(buildResponse(CLIENT_B_SSO_URL_NAME), CLIENT_B_SSO_URL_NAME, "Login to client B should be accepted");
    }

    /**
     * Reproduces keycloak#54342: an assertion whose Bearer SubjectConfirmationData Recipient names client A is
     * replayed, unmodified except for stripping the (optional, unsigned) outer Response Destination, against
     * IdP-initiated client B on the same broker. Before the fix, the empty allowed-recipient set caused the
     * Recipient check to be skipped entirely, and the login succeeded for B.
     */
    @Test
    void testAssertionForClientACannotBeReplayedToClientB() throws Exception {
        ResponseType responseType = buildResponseModel(CLIENT_A_SSO_URL_NAME);
        responseType.setDestination(null);

        assertRejected(SAML2Response.convert(responseType), CLIENT_B_SSO_URL_NAME,
                "Assertion for client A replayed to client B with no Destination should be rejected");

        EventAssertion.assertError(events.poll())
                .type(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR)
                .error(Errors.INVALID_SAML_RESPONSE);
    }

    /**
     * Same as above, but the outer Destination is forged to point at client B instead of being omitted. The
     * forged Destination alone must not be enough: the signed Recipient (here, the one the test built the
     * assertion with) still names client A and must still be enforced.
     */
    @Test
    void testAssertionForClientACannotBeReplayedToClientBWithForgedDestination() throws Exception {
        ResponseType responseType = buildResponseModel(CLIENT_A_SSO_URL_NAME);
        responseType.setDestination(getIdpInitiatedBrokerEndpoint(CLIENT_B_SSO_URL_NAME));

        assertRejected(SAML2Response.convert(responseType), CLIENT_B_SSO_URL_NAME,
                "Assertion for client A replayed to client B with a forged Destination should still be rejected");
    }

    /**
     * Hardening check: a Bearer SubjectConfirmationData without a Recipient attribute at all must be rejected,
     * even when it is sent back to the very client it was issued for. The outer Destination is also absent here:
     * with only a present Destination, the pre-existing #53475 validator logic already makes Recipient mandatory
     * (allowedRecipients is non-empty), so this specifically covers the gap #54342 closes, where Destination is
     * also missing and the Recipient check must still not be skipped.
     */
    @Test
    void testAssertionWithoutRecipientIsRejectedEvenWhenSentToItsOwnClient() throws Exception {
        ResponseType responseType = buildResponseModel(CLIENT_A_SSO_URL_NAME);
        responseType.setDestination(null);
        responseType.getAssertions().get(0).getAssertion().getSubject().getConfirmation().get(0)
                .getSubjectConfirmationData().setRecipient(null);

        assertRejected(SAML2Response.convert(responseType), CLIENT_A_SSO_URL_NAME,
                "Assertion without a Recipient should be rejected even for its own client");
    }

    private void assertAccepted(Document samlResponse, String ssoUrlName, String message) throws Exception {
        try (CloseableHttpResponse response = post(samlResponse, ssoUrlName)) {
            assertEquals(302, response.getStatusLine().getStatusCode(), message);
        }
    }

    private void assertRejected(Document samlResponse, String ssoUrlName, String message) throws Exception {
        try (CloseableHttpResponse response = post(samlResponse, ssoUrlName)) {
            assertEquals(400, response.getStatusLine().getStatusCode(), message);
        }
    }

    private CloseableHttpResponse post(Document samlResponse, String ssoUrlName) throws Exception {
        URI brokerEndpoint = URI.create(getIdpInitiatedBrokerEndpoint(ssoUrlName));
        return httpClient.execute(SamlClient.Binding.POST.createSamlUnsignedResponse(brokerEndpoint, null, samlResponse));
    }

    private String getIdpInitiatedBrokerEndpoint(String ssoUrlName) {
        return consumerRealm.getBaseUrl() + "/broker/" + getIdpAlias() + "/endpoint/clients/" + ssoUrlName;
    }

    private Document buildResponse(String ssoUrlName) throws Exception {
        return SAML2Response.convert(buildResponseModel(ssoUrlName));
    }

    private ResponseType buildResponseModel(String ssoUrlName) throws Exception {
        return new SAML2LoginResponseBuilder()
                .issuer(providerRealm.getBaseUrl())
                .destination(getIdpInitiatedBrokerEndpoint(ssoUrlName))
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), USER_EMAIL)
                .sessionIndex("session-" + UUID.randomUUID())
                .requestIssuer(consumerRealm.getBaseUrl())
                .assertionExpiration(ASSERTION_VALIDITY_SECONDS)
                .subjectExpiration(ASSERTION_VALIDITY_SECONDS)
                .buildModel();
    }

    private void updateIdp(Consumer<IdentityProviderRepresentation> update) {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        update.accept(idp);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }
}

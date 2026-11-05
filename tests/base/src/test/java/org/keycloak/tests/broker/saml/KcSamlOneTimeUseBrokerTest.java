/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.function.Consumer;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.tests.saml.SamlClient;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcSamlOneTimeUseBrokerTest extends AbstractKcSamlBrokerTest {

    private static final String IDP_INITIATED_SSO_URL_NAME = "sales-post";
    private static final int ASSERTION_VALIDITY_SECONDS = 3600;

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectEvents(realmRef = "consumer")
    Events events;

    @InjectTimeOffSet(enableForCaches = true)
    TimeOffSet timeOffSet;

    @BeforeEach
    void configureConsumerRealm() {
        // error events of the broker endpoint are not saved by default, so they are invisible to the test events otherwise
        consumerRealm.updateWithCleanup(realm -> realm.enabledEventTypes(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR.name()));
        updateIdp(idp -> idp.getConfig().put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "false"));
    }

    @Test
    void testOneTimeUseReplayDetected() throws Exception {
        Document samlResponse = buildResponse(true);

        assertAccepted(samlResponse, "First use of OneTimeUse assertion should be accepted");
        events.skipAll();
        assertRejected(samlResponse, "Replayed OneTimeUse assertion should be rejected");

        EventAssertion event = EventAssertion.assertError(events.poll())
                .type(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR)
                .error(Errors.INVALID_SAML_RESPONSE)
                .clientId(SAML_CLIENT_ID_SALES_POST + ".dot/ted")
                .details(Details.IDENTITY_PROVIDER, getIdpAlias());
        assertTrue(event.getEvent().getDetails().get(Details.REASON).startsWith("OneTimeUse assertion has already been used"));
    }

    @Test
    void testSignedOneTimeUseReplayDetected() throws Exception {
        KeyPair signingKeyPair = KeyUtils.generateRsaKeyPair(2048);
        X509Certificate signingCertificate = CertificateUtils.generateV1SelfSignedCertificate(signingKeyPair, "test-idp");
        updateIdp(idp -> {
            idp.getConfig().put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
            idp.getConfig().put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY,
                    PemUtils.encodeCertificate(signingCertificate));
        });

        Document samlResponse = buildResponse(true);
        BaseSAML2BindingBuilder<?> bindingBuilder = new BaseSAML2BindingBuilder<>()
                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                .signWith("test-idp", signingKeyPair, signingCertificate);
        bindingBuilder.signDocument(samlResponse);

        assertAccepted(samlResponse, "First use of signed OneTimeUse assertion should be accepted");
        assertRejected(samlResponse, "Replayed signed OneTimeUse assertion should be rejected");
    }

    @Test
    void testDistinctOneTimeUseAssertionsAreAccepted() throws Exception {
        assertAccepted(buildResponse(true), "First OneTimeUse assertion should be accepted");
        assertAccepted(buildResponse(true), "A different OneTimeUse assertion should also be accepted");
    }

    @Test
    void testAssertionWithoutOneTimeUseIsNotTracked() throws Exception {
        Document samlResponse = buildResponse(false);

        assertAccepted(samlResponse, "First use of assertion without OneTimeUse should be accepted");
        assertAccepted(samlResponse, "Assertion without OneTimeUse is not subject to replay detection");
    }

    @Test
    void testOneTimeUseWithExpiredAssertionIsRejected() throws Exception {
        ResponseType responseType = buildResponseModel(true);
        ConditionsType conditions = responseType.getAssertions().get(0).getAssertion().getConditions();
        conditions.setNotOnOrAfter(conditions.getNotBefore());

        assertRejected(SAML2Response.convert(responseType), "Expired OneTimeUse assertion should be rejected");
    }

    @Test
    void testAssertionOlderThanMaxAgeIsRejectedWhileStillValid() throws Exception {
        int maxAgeSeconds = 60;
        updateIdp(idp -> idp.getConfig().put(SAMLIdentityProviderConfig.ONE_TIME_USE_MAX_AGE_SECONDS, String.valueOf(maxAgeSeconds)));

        Document samlResponse = buildResponse(true);

        assertAccepted(samlResponse, "First use of OneTimeUse assertion should be accepted");

        timeOffSet.set(maxAgeSeconds / 2);
        assertRejected(samlResponse, "Replay within the maximum age should be rejected");

        // the assertion is still within its validity period, but beyond the maximum age
        timeOffSet.set(maxAgeSeconds * 2);
        assertRejected(samlResponse, "Replay beyond the maximum age should be rejected even after the replay marker is gone");
    }

    private void assertAccepted(Document samlResponse, String message) throws Exception {
        try (CloseableHttpResponse response = post(samlResponse)) {
            assertEquals(302, response.getStatusLine().getStatusCode(), message);
        }
    }

    private void assertRejected(Document samlResponse, String message) throws Exception {
        try (CloseableHttpResponse response = post(samlResponse)) {
            assertEquals(400, response.getStatusLine().getStatusCode(), message);
        }
    }

    private CloseableHttpResponse post(Document samlResponse) throws Exception {
        URI brokerEndpoint = URI.create(getIdpInitiatedBrokerEndpoint());
        return httpClient.execute(SamlClient.Binding.POST.createSamlUnsignedResponse(brokerEndpoint, null, samlResponse));
    }

    private String getIdpInitiatedBrokerEndpoint() {
        return consumerRealm.getBaseUrl() + "/broker/" + IDP_SAML_ALIAS
                + "/endpoint/clients/" + IDP_INITIATED_SSO_URL_NAME;
    }

    private Document buildResponse(boolean includeOneTimeUse) throws Exception {
        return SAML2Response.convert(buildResponseModel(includeOneTimeUse));
    }

    private ResponseType buildResponseModel(boolean includeOneTimeUse) throws Exception {
        return new SAML2LoginResponseBuilder()
                .issuer(providerRealm.getBaseUrl())
                .destination(getIdpInitiatedBrokerEndpoint())
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), USER_EMAIL)
                .sessionIndex("session-" + UUID.randomUUID())
                .requestIssuer(consumerRealm.getBaseUrl())
                .assertionExpiration(ASSERTION_VALIDITY_SECONDS)
                .subjectExpiration(ASSERTION_VALIDITY_SECONDS)
                .includeOneTimeUseCondition(includeOneTimeUse)
                .buildModel();
    }

    private void updateIdp(Consumer<IdentityProviderRepresentation> update) {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        update.accept(idp);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }
}

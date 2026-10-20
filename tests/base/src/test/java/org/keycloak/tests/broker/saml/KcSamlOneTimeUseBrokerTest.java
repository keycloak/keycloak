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
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation.KeyMetadataRepresentation;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.broker.AbstractKcSamlBrokerTest;
import org.keycloak.tests.saml.SamlClient;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcSamlOneTimeUseBrokerTest extends AbstractKcSamlBrokerTest {

    private static final String IDP_INITIATED_SSO_URL_NAME = "sales-post";

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @BeforeEach
    void configureOneTimeUseAndSigning() {
        String samlClientId = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(samlClientId).get(0);
        client.getAttributes().put(SamlConfigAttributes.SAML_ONETIMEUSE_CONDITION, "true");
        client.getAttributes().put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "true");
        providerRealm.admin().clients().get(client.getId()).update(client);

        String providerSigningCert = activeRs256SigCert(providerRealm);
        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
        idp.getConfig().put(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY, providerSigningCert);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }

    @Test
    void testOneTimeUseReplayDetected() throws Exception {
        disableSignatureValidation();

        URI brokerEndpoint = URI.create(getIdpInitiatedBrokerEndpoint());
        Document samlResponse = buildOneTimeUseResponse(brokerEndpoint.toString(), 300);

        try (CloseableHttpResponse firstResponse = httpClient.execute(
                SamlClient.Binding.POST.createSamlUnsignedResponse(brokerEndpoint, null, samlResponse))) {
            assertNotEquals(400, firstResponse.getStatusLine().getStatusCode(),
                    "First use of OneTimeUse assertion should not be rejected as replay");
        }

        try (CloseableHttpResponse secondResponse = httpClient.execute(
                SamlClient.Binding.POST.createSamlUnsignedResponse(brokerEndpoint, null, samlResponse))) {
            assertEquals(400, secondResponse.getStatusLine().getStatusCode(),
                    "Replayed OneTimeUse assertion should be rejected");
            String body = IOUtils.toString(secondResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue(body.contains("Invalid") || body.contains("expired") || body.contains("error"),
                    "Error page should indicate the assertion was rejected");
        }
    }

    @Test
    void testOneTimeUseWithoutNotOnOrAfterIsRejected() throws Exception {
        disableSignatureValidation();

        URI brokerEndpoint = URI.create(getIdpInitiatedBrokerEndpoint());
        ResponseType responseType = buildResponseModel(brokerEndpoint.toString(), 300);

        AssertionType assertion = responseType.getAssertions().get(0).getAssertion();
        assertion.getConditions().setNotOnOrAfter(null);

        Document samlResponse = SAML2Response.convert(responseType);

        try (CloseableHttpResponse response = httpClient.execute(
                SamlClient.Binding.POST.createSamlUnsignedResponse(brokerEndpoint, null, samlResponse))) {
            assertEquals(400, response.getStatusLine().getStatusCode(),
                    "OneTimeUse assertion without NotOnOrAfter should be rejected");
        }
    }

    @Test
    void testOneTimeUseWithExpiredAssertionIsRejected() throws Exception {
        disableSignatureValidation();

        URI brokerEndpoint = URI.create(getIdpInitiatedBrokerEndpoint());
        ResponseType responseType = buildResponseModel(brokerEndpoint.toString(), 0);

        ConditionsType conditions = responseType.getAssertions().get(0).getAssertion().getConditions();
        conditions.setNotOnOrAfter(conditions.getNotBefore());

        Document samlResponse = SAML2Response.convert(responseType);

        try (CloseableHttpResponse response = httpClient.execute(
                SamlClient.Binding.POST.createSamlUnsignedResponse(brokerEndpoint, null, samlResponse))) {
            assertEquals(400, response.getStatusLine().getStatusCode(),
                    "Expired OneTimeUse assertion should be rejected");
        }
    }

    @Test
    void testOneTimeUseWithNotOnOrAfterTooFarInFutureIsRejected() throws Exception {
        disableSignatureValidation();

        URI brokerEndpoint = URI.create(getIdpInitiatedBrokerEndpoint());
        // MAX_ONE_TIME_USE_TTL_SECONDS is 12 hours; set expiration to 24 hours
        ResponseType responseType = buildResponseModel(brokerEndpoint.toString(), 24 * 60 * 60);

        Document samlResponse = SAML2Response.convert(responseType);

        try (CloseableHttpResponse response = httpClient.execute(
                SamlClient.Binding.POST.createSamlUnsignedResponse(brokerEndpoint, null, samlResponse))) {
            assertEquals(400, response.getStatusLine().getStatusCode(),
                    "OneTimeUse assertion with NotOnOrAfter too far in the future should be rejected");
        }
    }

    @Test
    void testOneTimeUseWithExcessivelyLongKeyIsRejected() throws Exception {
        disableSignatureValidation();

        URI brokerEndpoint = URI.create(getIdpInitiatedBrokerEndpoint());
        ResponseType responseType = buildResponseModel(brokerEndpoint.toString(), 300);

        // Create an issuer long enough that "saml.oneTimeUse." + issuer + "::" + assertionId > 255
        String longIssuer = "https://idp.example.com/" + "a".repeat(250);
        responseType.getAssertions().get(0).getAssertion().getIssuer().setValue(longIssuer);

        Document samlResponse = SAML2Response.convert(responseType);

        try (CloseableHttpResponse response = httpClient.execute(
                SamlClient.Binding.POST.createSamlUnsignedResponse(brokerEndpoint, null, samlResponse))) {
            assertEquals(400, response.getStatusLine().getStatusCode(),
                    "OneTimeUse assertion with cache key exceeding 255 characters should be rejected");
        }
    }

    private String getIdpInitiatedBrokerEndpoint() {
        return consumerRealm.getBaseUrl() + "/broker/" + IDP_SAML_ALIAS
                + "/endpoint/clients/" + IDP_INITIATED_SSO_URL_NAME;
    }

    private ResponseType buildResponseModel(String destination, int expirationSeconds) throws Exception {
        ResponseType responseType = new SAML2LoginResponseBuilder()
                .issuer(providerRealm.getBaseUrl())
                .destination(destination)
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), USER_EMAIL)
                .sessionIndex("session-" + UUID.randomUUID())
                .requestIssuer(consumerRealm.getBaseUrl())
                .assertionExpiration(expirationSeconds)
                .subjectExpiration(expirationSeconds)
                .includeOneTimeUseCondition(true)
                .buildModel();
        return responseType;
    }

    private Document buildOneTimeUseResponse(String destination, int expirationSeconds) throws Exception {
        return SAML2Response.convert(buildResponseModel(destination, expirationSeconds));
    }

    private void disableSignatureValidation() {
        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "false");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }

    private static String activeRs256SigCert(ManagedRealm realm) {
        return realm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(k -> k.getCertificate() != null && KeyUse.SIG.equals(k.getUse())
                        && Algorithm.RS256.equals(k.getAlgorithm()))
                .map(KeyMetadataRepresentation::getCertificate)
                .findFirst().orElseThrow();
    }
}

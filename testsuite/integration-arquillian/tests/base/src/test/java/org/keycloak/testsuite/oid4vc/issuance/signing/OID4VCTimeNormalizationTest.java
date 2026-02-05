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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailResponse;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration tests validating time-claim normalization configuration and effects on JWT-VC.
 *
 * @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 *
 */
public class OID4VCTimeNormalizationTest extends OID4VCIssuerEndpointTest {

    @Test
    public void testJwtVcNbfRoundedToStartOfDayUtc() {
        // Configure realm to round time claims to DAY
        testingClient.server(TEST_REALM_NAME).run(session -> {
            session.getContext().getRealm().setAttribute("oid4vci.time.claims.strategy", "round");
            session.getContext().getRealm().setAttribute("oid4vci.time.round.unit", "DAY");
        });

        final String scopeName = jwtTypeCredentialClientScope.getName();
        String credConfigId = jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                var authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier);

                String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                Response response = issuerEndpoint.requestCredential(requestPayload);
                assertEquals(HttpStatus.SC_OK, response.getStatus());

                CredentialResponse credentialResponse = JsonSerialization.mapper
                        .convertValue(response.getEntity(), CredentialResponse.class);
                assertNotNull(credentialResponse);
                String jwtString = (String) credentialResponse.getCredentials().get(0).getCredential();

                JsonWebToken jwt = TokenVerifier.create(jwtString, JsonWebToken.class).getToken();
                assertNotNull(jwt);

                Long nbf = jwt.getNbf();
                assertNotNull("nbf should be present", nbf);

                // Assert nbf is truncated to start of day UTC (multiple of 86400)
                assertEquals(0, nbf % 86400);

                // Additionally, ensure issuance date inside vc claim (if present) aligns with the day boundary
                Object vcObj = jwt.getOtherClaims().get("vc");
                var vc = JsonSerialization.mapper.convertValue(vcObj, VerifiableCredential.class);
                Instant issuance = vc.getIssuanceDate();
                assertNotNull("issuanceDate should be present", issuance);
                assertEquals(0, issuance.getEpochSecond() % 86400);
            } catch (IOException | VerificationException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

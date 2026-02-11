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
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailResponse;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCIssuedAtTimeClaimMapper;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * SD-JWT variant: ensure realm-level rounding of issuanceDate propagates to iat when mapper sources from VC.
 *
 * @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 *
 */
public class OID4VCTimeNormalizationSdJwtTest extends OID4VCIssuerEndpointTest {

    @Test
    public void testSdJwtIatRoundedViaRealmNormalizedIssuanceDate() {
        // Configure realm to round time claims to DAY
        testingClient.server(TEST_REALM_NAME).run(session -> {
            session.getContext().getRealm().setAttribute("oid4vci.time.claims.strategy", "round");
            session.getContext().getRealm().setAttribute("oid4vci.time.round.unit", "DAY");
        });

        String scopeName = sdJwtTypeCredentialClientScope.getName();
        String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
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

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                // Add a protocol mapper that maps iat from VC issuanceDate (VALUE_SOURCE=VC)
                ClientScopeRepresentation clientScope = fromJsonString(clientScopeString, ClientScopeRepresentation.class);
                ProtocolMapperRepresentation pr = new ProtocolMapperRepresentation();
                pr.setName("iat-from-vc");
                pr.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
                pr.setProtocolMapper(OID4VCIssuedAtTimeClaimMapper.MAPPER_ID);
                pr.setConfig(Map.of(
                        OID4VCIssuedAtTimeClaimMapper.CLAIM_NAME, "iat",
                        OID4VCIssuedAtTimeClaimMapper.VALUE_SOURCE, "VC"
                ));
                clientScope.getProtocolMappers().add(pr);

                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier);

                String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                Response response = issuerEndpoint.requestCredential(requestPayload);
                assertEquals(HttpStatus.SC_OK, response.getStatus());

                CredentialResponse credentialResponse = JsonSerialization.mapper.convertValue(response.getEntity(), CredentialResponse.class);
                assertNotNull(credentialResponse);

                // Parse SD-JWT and check iat rounding (multiple of 86400)
                SdJwtVP sdJwtVP = SdJwtVP.of(credentialResponse.getCredentials().get(0).getCredential().toString());
                JsonWebToken jwt = TokenVerifier.create(sdJwtVP.getIssuerSignedJWT().getJws(), JsonWebToken.class).getToken();
                Long iat = jwt.getIat();
                assertNotNull(iat);
                assertEquals(0, iat % 86400);
            } catch (IOException | VerificationException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

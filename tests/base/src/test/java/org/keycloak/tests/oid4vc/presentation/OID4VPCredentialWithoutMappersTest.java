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

package org.keycloak.tests.oid4vc.presentation;

import java.util.List;
import java.util.Map;

import org.keycloak.OID4VCConstants;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.broker.oid4vp.RequestedCredentials;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SIGNING_ALG;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The verifier requests a PID without mappers next to a membership credential.
 * The membership credential holds the principal claim, which links the user to the identity provider.
 * The PID query holds only its vct and no claims.
 */
@KeycloakIntegrationTest(config = PresentationServerConfig.class)
public class OID4VPCredentialWithoutMappersTest extends OID4VPVerifierTestBase {

    private static final String MEMBERSHIP = "membership";
    private static final String MEMBERSHIP_TRUST_IDP_ALIAS = "oid4vp-membership-trust";

    private static String membershipVct;

    @Override
    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        CredentialScopeRepresentation membershipScope = requireExistingCredentialScope(sdJwtTypeNaturalPersonScopeName);
        membershipVct = membershipScope.getVct();
        setCredentialScopeAttributes(membershipScope, Map.of(VC_SIGNING_ALG, Algorithm.ES256));
        createTrustIdp(ISSUER_TRUST_IDP_ALIAS, realmSigningJwks());
        createTrustIdp(MEMBERSHIP_TRUST_IDP_ALIAS, realmSigningJwks());
        createVerifierIdp(Map.of(OID4VPIdentityProviderConfig.REQUESTED_CREDENTIALS,
                requestedCredentials(new RequestedCredentials(
                        List.of(credential(CREDENTIAL_ID, ISSUER_TRUST_IDP_ALIAS),
                                new RequestedCredentials.Credential(MEMBERSHIP, OID4VCConstants.FORMAT_SD_JWT_VC,
                                        List.of(membershipVct), List.of(MEMBERSHIP_TRUST_IDP_ALIAS))),
                        null, new RequestedCredentials.Principal(MEMBERSHIP, "familyName")))));
    }

    @Override
    protected void createVerifierMappers() {
    }

    @Test
    public void credentialWithoutMappersIsRequestedWithoutClaims() throws Exception {
        JsonNode credentials = requestObject().path("dcql_query").path("credentials");

        assertEquals(JsonSerialization.readValue("""
                [
                  {
                    "id": "pid",
                    "format": "dc+sd-jwt",
                    "meta": { "vct_values": ["%s"] }
                  },
                  {
                    "id": "membership",
                    "format": "dc+sd-jwt",
                    "meta": { "vct_values": ["%s"] },
                    "claims": [{ "path": ["familyName"] }]
                  }
                ]""".formatted(sdJwtTypeCredentialVct, membershipVct), JsonNode.class), credentials);
    }

    @Test
    public void loginWithCredentialWithoutMappersContinues() throws Exception {
        OID4VCTestContext pid = issueCredential();
        OID4VCTestContext membership = issueCredential(null, sdJwtNaturalPersonCredentialScope);
        JsonNode request = requestObject();
        ObjectNode vpToken = JsonSerialization.mapper.createObjectNode();
        vpToken.putArray(CREDENTIAL_ID).add(wallet.presentation(pid, request));
        vpToken.putArray(MEMBERSHIP).add(wallet.presentation(membership, request));

        driver.open(wallet.directPost(request, vpToken.toString()).getRedirectUri());
        assertLoginContinued();
    }
}

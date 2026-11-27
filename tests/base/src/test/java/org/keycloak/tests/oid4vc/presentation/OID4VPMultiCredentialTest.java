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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OID4VCConstants;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.broker.oid4vp.RequestedCredentials;
import org.keycloak.broker.oid4vp.mappers.AbstractOID4VPClaimMapper;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vpDirectPostResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SIGNING_ALG;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Drives the OID4VP wallet login with two requested credentials of different types: the required
 * credential holding the principal and an optional natural person credential in a credential set of
 * its own, trusted through a trust material identity provider of its own and read by a mapper of its
 * own.
 */
@KeycloakIntegrationTest(config = PresentationServerConfig.class)
public class OID4VPMultiCredentialTest extends OID4VPVerifierTestBase {

    private static final String EITHER_TYPE = "eitherType";
    private static final String MEMBERSHIP = "membership";
    private static final String MEMBERSHIP_TRUST_IDP_ALIAS = "oid4vp-membership-trust";
    private static final String MEMBERSHIP_MAPPER = "memberFamilyName-mapper";

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
                withCredentialSets(List.of(List.of(CREDENTIAL_ID)), List.of(List.of(MEMBERSHIP)))));
        addIdpMapper(MEMBERSHIP_MAPPER, USER_ATTRIBUTE_MAPPER_ID, Map.of(
                AbstractOID4VPClaimMapper.CREDENTIAL, MEMBERSHIP,
                "claim", "familyName",
                "user.attribute", "memberFamilyName"));
        addIdpMapper("importedMemberFamilyName-mapper", USER_ATTRIBUTE_MAPPER_ID, Map.of(
                AbstractOID4VPClaimMapper.CREDENTIAL, MEMBERSHIP,
                "claim", "familyName",
                "user.attribute", "importedMemberFamilyName",
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.IMPORT.name()));
        for (String holder : List.of("erin", "frank", "grace", "heidi", "ivan", "judy")) {
            createCompletionHolder(holder);
            grantMembership(holder);
        }
    }

    @Test
    public void dcqlQueryRequestsEveryCredentialWithItsClaimsAndTheCredentialSets() throws Exception {
        JsonNode dcqlQuery = requestObject().path("dcql_query");

        JsonNode membership = dcqlQuery.path("credentials").path(1);
        assertEquals(JsonSerialization.readValue("""
                {
                  "id": "membership",
                  "format": "dc+sd-jwt",
                  "meta": { "vct_values": ["%s"] },
                  "claims": [{ "path": ["familyName"] }]
                }""".formatted(membershipVct), JsonNode.class), membership);
        assertEquals(JsonSerialization.readValue("""
                [
                  { "options": [["pid"]] },
                  { "options": [["membership"]], "required": false }
                ]""", JsonNode.class), dcqlQuery.path("credential_sets"));
    }

    @Test
    public void claimsOfBothCredentialsAreMapped() throws Exception {
        OID4VCTestContext pid = issueCredential("erin");
        OID4VCTestContext membership = issueCredential("erin", sdJwtNaturalPersonCredentialScope);
        UserRepresentation holder = deleteHolder("erin");
        JsonNode request = requestObject();

        driver.open(wallet.directPost(request, vpToken(request, Map.of(CREDENTIAL_ID, pid, MEMBERSHIP, membership)))
                .getRedirectUri());
        assertLoginCompleted();

        UserRepresentation user = federatedUser(holder.getEmail());
        assertEquals(List.of("Baumeister"), user.getAttributes().get("memberFamilyName"));
        assertEquals(List.of("London"), user.getAttributes().get("locality"));
        endBrowserSession(user.getId());
    }

    @Test
    public void absentOptionalCredentialIsNotMapped() throws Exception {
        OID4VCTestContext credential = issueCredential("frank");
        UserRepresentation holder = deleteHolder("frank");
        JsonNode request = requestObject();

        driver.open(wallet.directPost(request, vpToken(request, Map.of(CREDENTIAL_ID, credential))).getRedirectUri());
        assertLoginCompleted();

        UserRepresentation user = federatedUser(holder.getEmail());
        assertNull(user.getAttributes().get("memberFamilyName"));
        assertEquals(List.of("London"), user.getAttributes().get("locality"));
        endBrowserSession(user.getId());
    }

    @Test
    public void claimOfACredentialNoLongerPresentedIsRemoved() throws Exception {
        OID4VCTestContext pid = issueCredential("heidi");
        OID4VCTestContext membership = issueCredential("heidi", sdJwtNaturalPersonCredentialScope);
        UserRepresentation holder = deleteHolder("heidi");
        JsonNode request = requestObject();
        driver.open(wallet.directPost(request, vpToken(request, Map.of(CREDENTIAL_ID, pid, MEMBERSHIP, membership)))
                .getRedirectUri());
        assertLoginCompleted();
        UserRepresentation user = federatedUser(holder.getEmail());
        assertEquals(List.of("Baumeister"), user.getAttributes().get("memberFamilyName"));
        endBrowserSession(user.getId());

        request = requestObject();
        driver.open(wallet.directPost(request, vpToken(request, Map.of(CREDENTIAL_ID, pid))).getRedirectUri());
        assertLoginCompleted();

        user = federatedUser(holder.getEmail());
        assertNull(user.getAttributes().get("memberFamilyName"));
        assertEquals(List.of("London"), user.getAttributes().get("locality"));
        endBrowserSession(user.getId());
    }

    @Test
    public void importedClaimOfACredentialNoLongerPresentedIsKept() throws Exception {
        OID4VCTestContext pid = issueCredential("ivan");
        OID4VCTestContext membership = issueCredential("ivan", sdJwtNaturalPersonCredentialScope);
        UserRepresentation holder = deleteHolder("ivan");
        JsonNode request = requestObject();
        driver.open(wallet.directPost(request, vpToken(request, Map.of(CREDENTIAL_ID, pid, MEMBERSHIP, membership)))
                .getRedirectUri());
        assertLoginCompleted();
        UserRepresentation user = federatedUser(holder.getEmail());
        endBrowserSession(user.getId());

        request = requestObject();
        driver.open(wallet.directPost(request, vpToken(request, Map.of(CREDENTIAL_ID, pid))).getRedirectUri());
        assertLoginCompleted();

        user = federatedUser(holder.getEmail());
        assertEquals(List.of("Baumeister"), user.getAttributes().get("importedMemberFamilyName"));
        assertNull(user.getAttributes().get("memberFamilyName"));
        endBrowserSession(user.getId());
    }

    @Test
    public void mapperOfACredentialThatIsNotRequestedIsSkipped() throws Exception {
        addIdpMapper("stale-mapper", USER_ATTRIBUTE_MAPPER_ID, Map.of(
                AbstractOID4VPClaimMapper.CREDENTIAL, "removed",
                "claim", "staleClaim",
                "user.attribute", "stale"));
        try {
            JsonNode credentials = requestObject().path("dcql_query").path("credentials");
            assertEquals(2, credentials.size());
            assertFalse(credentials.toString().contains("staleClaim"));
        } finally {
            testRealm.admin().identityProviders().get(IDP_ALIAS).getMappers().stream()
                    .filter(stored -> stored.getName().equals("stale-mapper"))
                    .forEach(stored -> testRealm.admin().identityProviders().get(IDP_ALIAS).delete(stored.getId()));
        }
    }

    @Test
    public void missingRequiredCredentialIsRejected() throws Exception {
        OID4VCTestContext membership = issueCredential("grace", sdJwtNaturalPersonCredentialScope);
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, vpToken(request, Map.of(MEMBERSHIP, membership)));

        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
    }

    @Test
    public void credentialOfAnotherTypeIsRejected() throws Exception {
        OID4VCTestContext pid = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request,
                vpToken(request, Map.of(CREDENTIAL_ID, pid, MEMBERSHIP, pid)));

        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
    }

    @Test
    public void optionalCredentialWithholdingItsRequestedClaimIsRejected() throws Exception {
        OID4VCTestContext pid = issueCredential();
        OID4VCTestContext membership = issueCredential(null, sdJwtNaturalPersonCredentialScope);
        JsonNode request = requestObject();
        ObjectNode vpToken = JsonSerialization.mapper.createObjectNode();
        vpToken.putArray(CREDENTIAL_ID).add(wallet.presentation(pid, request));
        vpToken.putArray(MEMBERSHIP).add(wallet.presentationOfRequestedClaims(membership, request, MEMBERSHIP, "familyName"));

        Oid4vpDirectPostResponse response = wallet.directPost(request, vpToken.toString());

        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
        assertEquals("The credential 'membership' does not disclose the requested claim 'familyName'",
                response.getErrorDescription());
    }

    @Test
    public void presentationOfAnUnrequestedCredentialIsRejected() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request,
                vpToken(request, Map.of(CREDENTIAL_ID, credential, "other", credential)));

        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
    }

    @Test
    public void alternativeOptionAcceptsThePrincipalCredentialAlone() throws Exception {
        updateRequestedCredentials(withCredentialSets(
                List.of(List.of(CREDENTIAL_ID, MEMBERSHIP), List.of(CREDENTIAL_ID)), null));
        OID4VCTestContext credential = issueCredential();

        JsonNode request = requestObject();
        assertEquals(JsonSerialization.readValue("""
                [{ "options": [["pid", "membership"], ["pid"]] }]""", JsonNode.class),
                request.path("dcql_query").path("credential_sets"));
        driver.open(wallet.directPost(request, vpToken(request, Map.of(CREDENTIAL_ID, credential))).getRedirectUri());
        assertLoginContinued();
    }

    @Test
    public void principalCredentialMissingInAnOptionIsRejected() {
        BadRequestException e = assertThrows(BadRequestException.class, () -> updateRequestedCredentials(
                withCredentialSets(List.of(List.of(CREDENTIAL_ID), List.of(MEMBERSHIP)), null)));

        assertEquals("The principal credential 'pid' must be part of every option of at least one required credential set",
                errorMessage(e.getResponse()));
    }

    @Test
    public void principalOfAnotherTypeDoesNotSignInTheLinkedUser() throws Exception {
        OID4VCTestContext pid = issueCredential("judy");
        OID4VCTestContext membership = issueCredential("judy", sdJwtNaturalPersonCredentialScope);
        UserRepresentation holder = deleteHolder("judy");
        JsonNode request = requestObject();
        driver.open(wallet.directPost(request, vpToken(request, Map.of(CREDENTIAL_ID, pid))).getRedirectUri());
        assertLoginCompleted();
        UserRepresentation user = federatedUser(holder.getEmail());
        List<String> links = List.of(sdJwtTypeCredentialVct + "#" + holder.getEmail());
        assertEquals(links, federatedUserIds(user));
        endBrowserSession(user.getId());

        // No mapper refers to this credential id, so only the email claim is requested, which both types carry.
        updateRequestedCredentials(requestedCredentials(new RequestedCredentials(
                List.of(new RequestedCredentials.Credential(EITHER_TYPE, OID4VCConstants.FORMAT_SD_JWT_VC,
                        List.of(sdJwtTypeCredentialVct, membershipVct),
                        List.of(ISSUER_TRUST_IDP_ALIAS, MEMBERSHIP_TRUST_IDP_ALIAS))),
                null, new RequestedCredentials.Principal(EITHER_TYPE, "email"))));

        request = requestObject();
        driver.open(wallet.directPost(request, vpToken(request, Map.of(EITHER_TYPE, membership))).getRedirectUri());

        assertLoginContinued();
        assertEquals(links, federatedUserIds(user));
        assertEquals(1, testRealm.admin().users().search(holder.getEmail(), true).size());
    }

    // The required options and, unless null, an optional credential set with the given options.
    private String withCredentialSets(List<List<String>> requiredOptions, List<List<String>> optionalOptions) {
        List<RequestedCredentials.CredentialSet> sets = optionalOptions == null
                ? List.of(new RequestedCredentials.CredentialSet(requiredOptions, null))
                : List.of(new RequestedCredentials.CredentialSet(requiredOptions, null),
                        new RequestedCredentials.CredentialSet(optionalOptions, false));
        return requestedCredentials(new RequestedCredentials(
                List.of(credential(CREDENTIAL_ID, ISSUER_TRUST_IDP_ALIAS),
                        new RequestedCredentials.Credential(MEMBERSHIP, OID4VCConstants.FORMAT_SD_JWT_VC,
                                List.of(membershipVct), List.of(MEMBERSHIP_TRUST_IDP_ALIAS))),
                sets, new RequestedCredentials.Principal(CREDENTIAL_ID, "email")));
    }

    private void grantMembership(String username) {
        UserVerifiableCredentialRepresentation grant = new UserVerifiableCredentialRepresentation();
        grant.setCredentialScopeName(sdJwtTypeNaturalPersonScopeName);
        testRealm.admin().users().get(requireExistingUser(username).getId()).verifiableCredentials()
                .createCredential(grant);
    }

    // Presents each issued credential under its credential id.
    private String vpToken(JsonNode request, Map<String, OID4VCTestContext> credentials) {
        ObjectNode vpToken = JsonSerialization.mapper.createObjectNode();
        credentials.forEach((id, credential) -> vpToken.putArray(id).add(wallet.presentation(credential, request)));
        return vpToken.toString();
    }

    private List<String> federatedUserIds(UserRepresentation user) {
        return testRealm.admin().users().get(user.getId()).getFederatedIdentity().stream()
                .map(FederatedIdentityRepresentation::getUserId).toList();
    }

    private static String errorMessage(Response response) {
        return response.readEntity(ErrorRepresentation.class).getErrorMessage();
    }
}

package org.keycloak.tests.oid4vc;

import java.util.List;

import org.keycloak.admin.client.resource.UserVerifiableCredentialResource;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.jwtProofs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithRestCredentialOfferEnabled.class)
public class OID4VCIssuedCredentialsAdminAPITest extends OID4VCIssuerEndpointTest {

    @Test
    public void testIssuedCredentialsArePersistedAndRetrievableViaAdminAPI() {
        String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        String userId = testRealm.admin().users().search("john").get(0).getId();

        UserVerifiableCredentialResource userVerifiableCredentialResource = testRealm.admin().users().get(userId).verifiableCredentials();

        List<IssuedVerifiableCredentialRepresentation> initialIssuedCreds = userVerifiableCredentialResource.getIssuedCredentials();
        assertTrue(initialIssuedCreds.isEmpty(), "No issued credentials should exist initially");

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);

        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        String cNonce = oauth.oid4vc().nonceRequest().send().getNonce();
        Proofs proofs = jwtProofs(credentialIssuer.getCredentialIssuer(), cNonce);

        CredentialResponse credentialResponseVO = oauth.oid4vc()
                .credentialRequest()
                .bearerToken(token)
                .credentialIdentifier(credentialIdentifier)
                .proofs(proofs)
                .send()
                .getCredentialResponse();

        assertNotNull(credentialResponseVO, "Credential should have been issued");
        assertNotNull(credentialResponseVO.getCredentials(), "Credentials array should not be null");
        assertFalse(credentialResponseVO.getCredentials().isEmpty(), "At least one credential should be issued");

        List<IssuedVerifiableCredentialRepresentation> issuedCreds = testRealm.admin().users().get(userId).verifiableCredentials().getIssuedCredentials();

        assertEquals(1, issuedCreds.size(), "Exactly one issued credential should be stored");

        IssuedVerifiableCredentialRepresentation issuedCred = issuedCreds.get(0);

        assertAll(
                () -> assertNotNull(issuedCred.getId(), "Issued credential should have an ID"),
                () -> assertEquals(userId, issuedCred.getUserId(), "User ID should match"),
                () -> assertEquals(scopeName, issuedCred.getCredentialType(), "Credential type should match the scope name"),
                () -> assertNotNull(issuedCred.getRevision(), "Revision should be set"),
                () -> assertNotNull(issuedCred.getIssuedAt(), "IssuedAt timestamp should be set"),
                () -> assertNotNull(issuedCred.getExpiresAt(), "expiresAt timestamp should be set"),
                () -> assertNotNull(issuedCred.getClientId(), "ClientId should be set")
        );

        String cNonce2 = oauth.oid4vc().doNonceRequest().getNonce();
        Proofs proofs2 = jwtProofs(credentialIssuer.getCredentialIssuer(), cNonce2);

        CredentialResponse credentialResponseVO2 = oauth.oid4vc()
                .credentialRequest()
                .bearerToken(token)
                .credentialIdentifier(credentialIdentifier)
                .proofs(proofs2)
                .send()
                .getCredentialResponse();

        assertNotNull(credentialResponseVO2, "Second credential should have been issued");

        List<IssuedVerifiableCredentialRepresentation> multipleIssuedCreds = testRealm.admin().users().get(userId).verifiableCredentials().getIssuedCredentials();

        assertEquals(2, multipleIssuedCreds.size(), "Two issued credentials should be stored");

        // Verify sorting (newest first - DESC by issuedAt)
        assertTrue(multipleIssuedCreds.get(0).getIssuedAt() >= multipleIssuedCreds.get(1).getIssuedAt(),
                "Issued credentials should be sorted by issuedAt DESC (newest first)");

        // Verify expiration for both issued credentials
        for (IssuedVerifiableCredentialRepresentation issuedCredential : multipleIssuedCreds) {
            assertEquals(issuedCredential.getIssuedAt() + (CREDENTIALS_EXPIRATION_IN_SECONDS * 1000), issuedCredential.getExpiresAt());
        }
    }
}

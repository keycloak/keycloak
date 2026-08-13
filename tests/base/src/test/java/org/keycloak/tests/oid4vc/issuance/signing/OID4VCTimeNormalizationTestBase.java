package org.keycloak.tests.oid4vc.issuance.signing;

import java.util.HashMap;
import java.util.List;

import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.tests.oid4vc.OID4VCIssuerEndpointTest;
import org.keycloak.tests.oid4vc.OID4VCProofTestUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

/**
 * Shared base for time-normalization integration tests.
 * Provides helpers for configuring realm rounding/randomizing and issuing credentials.
 */
abstract class OID4VCTimeNormalizationTestBase extends OID4VCIssuerEndpointTest {

    /**
     * Configures the realm to use the {@code round} strategy for time claims with the given unit.
     *
     * @param unit e.g. "DAY", "HOUR", "MINUTE"
     */
    protected void configureRoundStrategy(String unit) {
        RealmRepresentation realmRep = testRealm.admin().toRepresentation();
        if (realmRep.getAttributes() == null) {
            realmRep.setAttributes(new HashMap<>());
        }
        realmRep.getAttributes().put(OID4VCIConstants.TIME_CLAIMS_STRATEGY, "round");
        realmRep.getAttributes().put(OID4VCIConstants.TIME_ROUND_UNIT, unit);
        testRealm.admin().update(realmRep);
    }

    /**
     * Issues a credential for the given scope and returns the credential response.
     */
    protected CredentialResponse issueCredentialForScope(ClientScopeRepresentation scope) {
        String scopeName = scope.getName();
        String credConfigId = scope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(getRealmPath(testRealm.getName())));

        String authCode = getAuthorizationCode(oauth, managedClient.admin().toRepresentation(), "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails()
                .get(0).getCredentialIdentifiers().get(0);
        String cNonce = oauth.oid4vc().nonceRequest().send().getNonce();

        return oauth.oid4vc()
                .credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(OID4VCProofTestUtils.jwtProofs(getRealmPath(testRealm.getName()), cNonce))
                .bearerToken(token)
                .send()
                .getCredentialResponse();
    }
}

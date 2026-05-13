package org.keycloak.protocol.oid4vc.issuance;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.clientpolicy.PredicateCredentialClientPolicy;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.IssuerState;
import org.keycloak.protocol.oid4vc.utils.CredentialScopeUtils;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpointCheckProvider;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpointChecker;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpointChecker.AuthorizationCheckException;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;

import static org.keycloak.OAuth2Constants.ISSUER_STATE;
import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_REQUIRED;

public class OID4VCAuthorizationCheckProvider implements AuthorizationEndpointCheckProvider {

    private final KeycloakSession session;

    public OID4VCAuthorizationCheckProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void check(AuthorizationEndpointChecker context) throws AuthorizationCheckException {
        ClientModel client = context.getClient();
        AuthorizationEndpointRequest request = context.getAuthorizationEndpointRequest();

        // Get the list of requested credential scopes that are associated with this client
        //
        List<CredentialScopeModel> credScopes = CredentialScopeUtils.getCredentialScopesForAuthorization(client, request);

        // Proceed when there are requested credential scopes
        //
        if (!credScopes.isEmpty()) {

            PredicateCredentialClientPolicy offerRequiredPolicy = VC_POLICY_CREDENTIAL_OFFER_REQUIRED;

            // Get the potential offer state derived from issuer_state
            //
            String issuerStateParam = request.getAdditionalReqParams().get(ISSUER_STATE);
            CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
            CredentialOfferState offerState = Optional.ofNullable(issuerStateParam)
                    .map(IssuerState::fromEncodedString)
                    .map(IssuerState::getCredentialsOfferId)
                    .map(offerStorage::getOfferStateById)
                    .orElse(null);

            List<String> offeredConfigurationIds = Optional.ofNullable(offerState)
                    .map(CredentialOfferState::getCredentialsOffer)
                    .map(CredentialsOffer::getCredentialConfigurationIds)
                    .orElse(List.of());

            // Check whether each requested credential_configuration_id has actually been offered
            //
            for (CredentialScopeModel credScope : credScopes) {
                String credConfigId = credScope.getCredentialConfigurationId();

                boolean requiredByScope = offerRequiredPolicy.validate(new CredentialScopeRepresentation(credScope));
                if (requiredByScope && !offeredConfigurationIds.contains(credConfigId)) {
                    String errorDetail = "Authorization request rejected by policy " + offerRequiredPolicy.getName() + " for scope: " + credScope.getName();
                    throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, errorDetail);
                }
            }
        }
    }

    @Override
    public void close() {
    }
}

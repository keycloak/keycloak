package org.keycloak.protocol.oid4vc.clientpolicy;

import java.util.List;
import java.util.Optional;

import org.keycloak.OAuthErrorException;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.IssuerState;
import org.keycloak.protocol.oid4vc.utils.CredentialScopeUtils;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

import static org.keycloak.OAuth2Constants.ISSUER_STATE;
import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_REQUIRED;
import static org.keycloak.services.clientpolicy.ClientPolicyEvent.AUTHORIZATION_REQUEST;

public class CredentialClientPolicyExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    protected final KeycloakSession session;

    public CredentialClientPolicyExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return CredentialClientPolicyExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (AUTHORIZATION_REQUEST.equals(context.getEvent())) {
            AuthorizationRequestContext authRequestContext = (AuthorizationRequestContext) context;
            checkCredentialPolicies(authRequestContext);
        }
    }

    private void checkCredentialPolicies(AuthorizationRequestContext context) throws ClientPolicyException {

        ClientModel client = context.getClient();
        if (client == null)
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, "No issuing client");

        // Get the list of requested credential scopes that are associated with this client
        //
        AuthorizationEndpointRequest request = context.getAuthorizationEndpointRequest();
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

            // Get the offered credential configuration ids
            //
            List<String> offeredConfigurationIds = Optional.ofNullable(offerState)
                    .map(CredentialOfferState::getCredentialsOffer)
                    .map(CredentialsOffer::getCredentialConfigurationIds)
                    .orElse(List.of());

            // Check whether each requested credential_configuration_id has actually been offered
            //
            for (CredentialScopeModel credScope : credScopes) {
                String credConfigId = credScope.getCredentialConfigurationId();
                if (!offeredConfigurationIds.contains(credConfigId)) {
                    String errorDetail = "Authorization request rejected by policy " + offerRequiredPolicy.getName() + " for client: " + client.getClientId();
                    throw new ClientPolicyException(Errors.NOT_ALLOWED, errorDetail);
                }
            }
        }
    }
}

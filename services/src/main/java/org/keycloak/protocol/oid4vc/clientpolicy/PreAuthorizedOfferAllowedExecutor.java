package org.keycloak.protocol.oid4vc.clientpolicy;


import org.keycloak.OAuthErrorException;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.CredentialOfferException;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED;
import static org.keycloak.services.clientpolicy.ClientPolicyEvent.CREDENTIAL_OFFER_CREATE;

/**
 * Governs whether pre-authorized offer grants are allowed
 */
public class PreAuthorizedOfferAllowedExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    protected final KeycloakSession session;

    public PreAuthorizedOfferAllowedExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return PreAuthorizedOfferAllowedExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (context.getEvent() == CREDENTIAL_OFFER_CREATE) {
            CredentialOfferPolicyContext credOfferContext = (CredentialOfferPolicyContext) context;
            checkCredentialPolicies(credOfferContext);
        }
    }

    private void checkCredentialPolicies(CredentialOfferPolicyContext context) throws ClientPolicyException {

        CredentialOfferState offerState = context.getCredentialOfferState();
        if (offerState == null)
            throw new ClientPolicyException(OAuthErrorException.UNSUPPORTED_GRANT_TYPE, "No credential offer state");

        CredentialScopeModel credScope = context.getCredentialScopeModel();
        if (credScope == null)
            throw new ClientPolicyException(OAuthErrorException.INVALID_SCOPE, "No client scope");

        if (offerState.getCredentialsOffer().getPreAuthorizedGrant() != null) {

            // Allowed by scope when property is defined and true
            // See default value on the policy definition
            PredicateCredentialClientPolicy preAuthPolicy = VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED;
            boolean allowedByScope = preAuthPolicy.validate(new CredentialScopeRepresentation(credScope));
            if (!allowedByScope) {
                throw new CredentialOfferException(Errors.NOT_ALLOWED,
                        "Pre-Authorized code grant rejected by policy " + preAuthPolicy.getName() + " for scope " + credScope.getName());
            }
            context.setEvaluatedOnEvent(true);
        }
    }
}

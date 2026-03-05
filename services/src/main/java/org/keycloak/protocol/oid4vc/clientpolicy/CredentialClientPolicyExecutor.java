package org.keycloak.protocol.oid4vc.clientpolicy;

import org.keycloak.OAuthErrorException;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

import org.jspecify.annotations.Nullable;

import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED;
import static org.keycloak.protocol.oid4vc.clientpolicy.CredentialClientPolicies.VC_POLICY_CREDENTIAL_OFFER_REQUIRED;

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
        if (context instanceof CredentialClientPolicyContext) {
            checkCredentialPolicies((CredentialClientPolicyContext) context);
        }
    }

    private void checkCredentialPolicies(CredentialClientPolicyContext context) throws ClientPolicyException {

        ClientModel client = session.getContext().getClient();
        if (client == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, "No issuing client");
        }
        CredentialScopeModel credScope = context.getCredentialScopeModel();
        if (credScope == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_SCOPE, "No client scope");
        }

        CredentialOfferState offerState = context.getCredentialOfferState();

        // Check policy 'oid4vci-offer-preauth-allowed'
        //
        if (context.getEvent() == ClientPolicyEvent.CREDENTIAL_OFFER_CREATE) {

            if (offerState == null)
                throw new ClientPolicyException(OAuthErrorException.UNSUPPORTED_GRANT_TYPE, "No credential offer state");

            PredicateCredentialClientPolicy preAuthPolicy = VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED;
            ClientPolicyRepresentation clientPolicy = findClientPolicyByName(preAuthPolicy.getName());

            boolean allowedByClient = clientPolicy != null && clientPolicy.isEnabled();

            for (String grantType : offerState.getCredentialsOffer().getGrants().keySet()) {
                if (grantType.equals(PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE)) {
                    boolean allowedByScope = preAuthPolicy.validate(new CredentialScopeRepresentation(credScope));
                    if (!allowedByClient || !allowedByScope) {
                        throw new ClientPolicyException(Errors.NOT_ALLOWED,
                                "Pre-Authorized code grant rejected by policy " + preAuthPolicy.getName() + " for client scope " + credScope.getName());
                    }
                }
            }

            context.setProcessed(true);
        }

        // Check policy 'oid4vci-offer-required'
        //
        if (context.getEvent() == ClientPolicyEvent.CREDENTIAL_REQUEST) {

            PredicateCredentialClientPolicy offerRequiredPolicy = VC_POLICY_CREDENTIAL_OFFER_REQUIRED;
            ClientPolicyRepresentation clientPolicy = findClientPolicyByName(offerRequiredPolicy.getName());

            boolean requiredByClient = clientPolicy != null && clientPolicy.isEnabled();
            boolean requiredByScope = offerRequiredPolicy.validate(new CredentialScopeRepresentation(credScope));

            if ((requiredByClient || requiredByScope) && offerState == null) {
                throw new ClientPolicyException(Errors.NOT_ALLOWED,
                        "Credential request rejected by policy " + offerRequiredPolicy.getName() + " for client scope " + credScope.getName());
            }

            context.setProcessed(true);
        }
    }

    private @Nullable ClientPolicyRepresentation findClientPolicyByName(String policyName) throws ClientPolicyException {
        RealmModel realm = session.getContext().getRealm();
        ClientPolicyRepresentation clientPolicy = session.clientPolicy().getClientPolicies(realm, false).getPolicies().stream()
                .filter(cp -> cp.getName().equals(policyName))
                .findFirst().orElse(null);
        return clientPolicy;
    }
}

package org.keycloak.protocol.oid4vc.clientpolicy;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;

public abstract class CredentialClientPolicies {

    /**
     * Governs whether the given `credential_configuration_id` requires a Credential Offer
     */
    public static PredicateCredentialClientPolicy VC_POLICY_CREDENTIAL_OFFER_REQUIRED = new PredicateCredentialClientPolicy(
            "oid4vci-offer-required", "vc.policy.offer.required", true, false);

    public static ClientPolicyRepresentation findClientPolicyByName(KeycloakSession session, String policyName)  {
        try {
            RealmModel realm = session.getContext().getRealm();
            return session.clientPolicy().getClientPolicies(realm, false).getPolicies().stream()
                    .filter(cp -> cp.getName().equals(policyName))
                    .findFirst().orElse(null);
        } catch (ClientPolicyException ex) {
            throw new RuntimeException("Cannot access client policies", ex);
        }
    }
}

package org.keycloak.protocol.oid4vc.clientpolicy;

public interface CredentialClientPolicies {

    /**
     * Governs whether the given `credential_configuration_id` requires a Credential Offer
     */
    PredicateCredentialClientPolicy VC_POLICY_CREDENTIAL_OFFER_REQUIRED = new PredicateCredentialClientPolicy(
            "oid4vci-offer-required", "vc.policy.offer.required", true, false);
}

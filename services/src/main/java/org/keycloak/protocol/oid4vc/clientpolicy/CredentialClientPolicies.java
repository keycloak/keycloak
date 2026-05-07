package org.keycloak.protocol.oid4vc.clientpolicy;

public interface CredentialClientPolicies {

    /**
     * Governs whether the given `credential_configuration_id` requires a Credential Offer
     */
    PredicateCredentialClientPolicy VC_POLICY_CREDENTIAL_OFFER_REQUIRED = new PredicateCredentialClientPolicy(
            "oid4vci-offer-required", "vc.policy.offer.required", true, false);

    /**
     * Governs whether Credential Offers with `pre-authorized_code` grant are allowed
     */
    PredicateCredentialClientPolicy VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED = new PredicateCredentialClientPolicy(
            "oid4vci-offer-preauth-allowed", "vc.policy.offer.preauth.allowed", true, true);
}

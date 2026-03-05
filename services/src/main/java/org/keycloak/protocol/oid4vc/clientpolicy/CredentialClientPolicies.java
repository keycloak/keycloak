package org.keycloak.protocol.oid4vc.clientpolicy;

public abstract class CredentialClientPolicies {

    /**
     * Governs whether the given Credential Configuration requires a Credential Offer
     */
    public static PredicateCredentialClientPolicy VC_POLICY_CREDENTIAL_OFFER_REQUIRED = new PredicateCredentialClientPolicy(
            "oid4vci-offer-required", "vc.policy.offer.required", true, false);

    /**
     * Governs whether Credential Offers with `pre-authorized_code` grant are allowed
     */
    public static PredicateCredentialClientPolicy VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED = new PredicateCredentialClientPolicy(
            "oid4vci-preauth-allowed", "vc.policy.preauth.allowed", true, false);
}

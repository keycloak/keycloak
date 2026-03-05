package org.keycloak.protocol.oid4vc.policy;

public interface CredentialPolicies {

    /**
     * Governs whether Credential Offers with `pre-authorized_code` grant are allowed
     */
    PredicateCredentialPolicy VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED = new PredicateCredentialPolicy(
            "oid4vci-offer-preauth-allowed", "vc.policy.offer.preauth.allowed", true, true);
}

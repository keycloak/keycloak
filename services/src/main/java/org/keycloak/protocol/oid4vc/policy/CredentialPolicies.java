package org.keycloak.protocol.oid4vc.policy;

public interface CredentialPolicies {

    /**
     * Governs whether the given `credential_configuration_id` requires a Credential Offer
     */
    PredicateCredentialPolicy VC_POLICY_CREDENTIAL_OFFER_REQUIRED = new PredicateCredentialPolicy(
            "oid4vci-offer-required", "vc.policy.offer.required", true, false);

    /**
     * Governs whether Credential Offers with `pre-authorized_code` grant are allowed
     */
    PredicateCredentialPolicy VC_POLICY_CREDENTIAL_OFFER_PREAUTH_ALLOWED = new PredicateCredentialPolicy(
            "oid4vci-offer-preauth-allowed", "vc.policy.offer.preauth.allowed", true, true);

    /**
     * Governs whether Credential Offers with `pre-authorized_code` grant require a `tx_code`
     */
    PredicateCredentialPolicy VC_POLICY_CREDENTIAL_OFFER_TXCODE_REQUIRED = new PredicateCredentialPolicy(
            "oid4vci-offer-txcode-required", "vc.policy.offer.txcode.required", true, false);

    /**
     * Governs whether Credential Offers with `pre-authorized_code` grant have the `tx_code` redacted in the response
     */
    PredicateCredentialPolicy VC_POLICY_CREDENTIAL_OFFER_TXCODE_REDACTED = new PredicateCredentialPolicy(
            "oid4vci-offer-txcode-redacted", "vc.policy.offer.txcode.redacted", true, false);
}

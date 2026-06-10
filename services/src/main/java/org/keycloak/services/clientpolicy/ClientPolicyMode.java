package org.keycloak.services.clientpolicy;

public enum ClientPolicyMode {

    /**
     * Default condition mode. At least one condition must evaluate to `yes`. None of the conditions must evaluate to `no`. Conditions, which evaluate to `abstain` are ignored
     */
    DEFAULT,

    /**
     * All conditions must evaluate to `yes`. None of the conditions must evaluate to `no` or `abstain`.
     */
    STRICT
}

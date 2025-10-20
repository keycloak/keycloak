package org.keycloak.protocol.ssf.event.types.risc;

/**
 * Account Credential Change Required signals that the account identified by the subject was required to change a credential. For example the user was required to go through a password change.
 */
public class AccountCredentialChangeRequired extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.1
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-credential-change-required";

    public AccountCredentialChangeRequired() {
        super(TYPE);
    }
}

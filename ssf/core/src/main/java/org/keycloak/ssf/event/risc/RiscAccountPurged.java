package org.keycloak.ssf.event.risc;

/**
 * Account Purged signals that the account identified by the subject has been permanently deleted.
 */
public class RiscAccountPurged extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.2
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-purged";

    public RiscAccountPurged() {
        super(TYPE);
    }
}

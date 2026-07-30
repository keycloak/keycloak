package org.keycloak.ssf.event.risc;

/**
 * The Account Enabled event signals that a previously disabled account has been
 * reactivated.
 *
 * See: https://openid.net/specs/openid-risc-1_0-final.html
 */
public class RiscAccountEnabled extends RiscEvent {

    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-enabled";

    public RiscAccountEnabled() {
        super(TYPE);
    }

    @Override
    public String toString() {
        return "AccountEnabled{}";
    }
}

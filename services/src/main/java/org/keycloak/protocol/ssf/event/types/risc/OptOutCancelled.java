package org.keycloak.protocol.ssf.event.types.risc;

/**
 * Opt Out Cancelled signals that the account identified by the subject cancelled the opt-out from RISC event exchanges. The account is in the opt-in state.
 */
public class OptOutCancelled extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.8.3
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/opt-out-cancelled";

    public OptOutCancelled() {
        super(TYPE);
    }
}

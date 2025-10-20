package org.keycloak.protocol.ssf.event.types.risc;

/**
 * Opt Out Initiated signals that the account identified by the subject initiated to opt out from RISC event exchanges. The account is in the opt-out-initiated state.
 */
public class OptOutInitiated extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.8.1
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/opt-out-initiated";

    public OptOutInitiated() {
        super(TYPE);
    }
}

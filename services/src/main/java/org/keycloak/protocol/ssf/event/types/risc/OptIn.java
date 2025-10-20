package org.keycloak.protocol.ssf.event.types.risc;

/**
 * Opt In signals that the account identified by the subject opted into RISC event exchanges. The account is in the opt-in state.
 */
public class OptIn extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.8.1
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/opt-in";

    public OptIn() {
        super(TYPE);
    }
}

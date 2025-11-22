package org.keycloak.protocol.ssf.event.types.risc;

/**
 * Opt Out Effective signals that the account identified by the subject was effectively opted out from RISC event exchanges. The account is in the opt-out state.
 */
public class OptOutEffective extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.8.4
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/opt-out-effective";

    public OptOutEffective() {
        super(TYPE);
    }
}

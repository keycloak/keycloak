package org.keycloak.protocol.ssf.event.types.risc;

/**
 * Recovery Activated signals that the account identified by the subject activated a recovery flow.
 */
public class RecoveryActivated extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.9
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/recovery-activated";

    public RecoveryActivated() {
        super(TYPE);
    }
}

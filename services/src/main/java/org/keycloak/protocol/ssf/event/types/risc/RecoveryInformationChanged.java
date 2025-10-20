package org.keycloak.protocol.ssf.event.types.risc;

/**
 * Recovery Information Changed signals that the account identified by the subject has changed some of its recovery information. For example a recovery email address was added or removed.
 */
public class RecoveryInformationChanged extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.10
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/recovery-information-changed";

    public RecoveryInformationChanged() {
        super(TYPE);
    }
}

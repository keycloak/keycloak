package org.keycloak.protocol.ssf.event.types.risc;

/**
 * Identifier Recycled signals that the identifier specified in the subject was recycled, and now it belongs to a new user. The subject type MUST be either email or phone.
 */
public class IdentifierRecycled extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.6
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/identifier-recycled";

    public IdentifierRecycled() {
        super(TYPE);
    }
}

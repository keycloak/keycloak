package org.keycloak.protocol.ssf.event.types.risc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Identifier Changed signals that the identifier specified in the subject has changed. The subject type MUST be either email or phone, and it MUST specify the old value.
 *
 * This event SHOULD be issued only by the provider that is authoritative over the identifier. For example, if the person that owns john.doe@example.com goes through a name change and wants the new john.row@example.com email then only the email provider example.com SHOULD issue an Identifier Changed event as shown in the example below.
 *
 * If an identifier used as a username or recovery option is changed, at a provider that is not authoritative over that identifier, then Recovery Information Changed SHOULD be used instead.
 */
public class IdentifierChanged extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.5
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/identifier-changed";

    /**
     * optional, the new value of the identifier.
     */
    @JsonProperty("new-value")
    private String newValue;

    public IdentifierChanged() {
        super(TYPE);
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}

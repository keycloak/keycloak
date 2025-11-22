package org.keycloak.protocol.ssf.event.types.risc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Account Disabled signals that the account identified by the subject has been disabled. The actual reason why the account was disabled might be specified with the nested reason attribute described below. The account may be enabled in the future.
 */
public class AccountDisabled extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.3
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-disabled";

    /**
     * optional, describes why was the account disabled.
     * Possible values:
     * - hijacking
     * - bulk-account
     */
    @JsonProperty("reason")
    private String reason;

    public AccountDisabled() {
        super(TYPE);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

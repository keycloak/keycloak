package org.keycloak.protocol.ssf.event.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSF Verification event.
 *
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#name-verification
 */
public class VerificationEvent extends SsfEvent {

    public static final String TYPE = "https://schemas.openid.net/secevent/ssf/event-type/verification";

    @JsonProperty("state")
    protected String state;

    public VerificationEvent() {
        super(TYPE);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "VerificationEvent{" +
               "state='" + state + '\'' +
               '}';
    }
}

package org.keycloak.ssf.event.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSF Verification event.
 *
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#name-verification
 */
public class SsfStreamVerificationEvent extends SsfStreamEvent {

    public static final String TYPE = "https://schemas.openid.net/secevent/ssf/event-type/verification";

    @JsonProperty("state")
    protected String state;

    public SsfStreamVerificationEvent() {
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
        // Render absent state as an empty object rather than "state='null'"
        // so the log representation matches what Jackson actually puts on
        // the wire (omitted thanks to @JsonInclude(NON_NULL)).
        return state == null
                ? "VerificationEvent{}"
                : "VerificationEvent{state='" + state + "'}";
    }
}

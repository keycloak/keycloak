package org.keycloak.ssf.event.stream;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSF Verification event.
 * <p>
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#name-verification
 */
public class SsfStreamVerificationEvent extends SsfStreamEvent {

    public static final String TYPE = SsfStreamEvent.EVENT_TYPE_BASE_URI + "verification";

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
    protected void appendFields(Map<String, Object> fields) {
        super.appendFields(fields);
        fields.put("state", state);
    }
}

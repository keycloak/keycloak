package org.keycloak.representations;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.TokenCategory;
import org.keycloak.util.TokenUtil;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogoutToken extends JsonWebToken {

    @JsonProperty("sid")
    protected String sid;

    @JsonProperty("events")
    protected Map<String, Object> events = new HashMap<>();

    public Map<String, Object> getEvents() {
        return events;
    }

    public void putEvents(String name, Object value) {
        events.put(name, value);
    }

    public String getSid() {
        return sid;
    }

    public LogoutToken setSid(String sid) {
        this.sid = sid;
        return this;
    }

    public LogoutToken() {
        type(TokenUtil.TOKEN_TYPE_LOGOUT);
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.LOGOUT;
    }
}

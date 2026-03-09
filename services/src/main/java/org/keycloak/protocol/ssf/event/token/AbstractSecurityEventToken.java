package org.keycloak.protocol.ssf.event.token;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.keycloak.TokenCategory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbstractSecurityEventToken implements SecurityEventToken {

    @JsonProperty("jti")
    protected String jti;

    @JsonProperty("iss")
    protected  String iss;

    @JsonProperty("iat")
    protected  Integer iat;

    @JsonProperty("aud")
    @JsonSerialize(using = StringOrArraySerializer.class)
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    protected  String[] aud;

    @JsonProperty("events")
    @JsonDeserialize(using = SsfEventMapJsonDeserializer.class)
    private Map<String, Object> events;

    @Override
    public TokenCategory getCategory() {
        // This MUST be ACCESS to select the proper Key Material for signing Security Event Tokens
        return TokenCategory.ACCESS;
    }

    @Override
    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    @Override
    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    @Override
    public Integer getIat() {
        return iat;
    }

    public void setIat(Integer iat) {
        this.iat = iat;
    }

    @Override
    public String[] getAud() {
        return aud;
    }

    public void setAud(String[] aud) {
        this.aud = aud;
    }

    @Override
    public Map<String, Object> getEvents() {
        if (events == null) {
            events = new LinkedHashMap<>();
        }
        return events;
    }

    public void setEvents(Map<String, Object> events) {
        this.events = events;
    }
}

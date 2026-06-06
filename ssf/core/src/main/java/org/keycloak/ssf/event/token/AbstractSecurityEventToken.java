package org.keycloak.ssf.event.token;

import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.TokenCategory;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
        // SSF SETs are server-produced and never issued to a client the way
        // OIDC access/id tokens are, so no OIDC-oriented TokenCategory
        // applies cleanly. The category returned here is NOT consulted by
        // the SSF signing path: SecurityEventTokenDispatcher resolves the
        // JWS signature algorithm via SsfSignatureAlgorithms#resolveForStream
        // (stream override → transmitter SPI default → hardcoded RS256) and
        // passes it straight to SecurityEventTokenEncoder. Returning
        // INTERNAL here is the neutral fallback that satisfies the
        // org.keycloak.Token contract; if a future caller accidentally
        // routes a SET through session.tokens() helpers, they'll pick up
        // the internal-token signer instead of silently inheriting a
        // receiver client's OIDC access-token alg.
        return TokenCategory.INTERNAL;
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

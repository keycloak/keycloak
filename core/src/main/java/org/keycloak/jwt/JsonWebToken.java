package org.keycloak.jwt;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JsonWebToken implements Serializable {
    @JsonProperty("jti")
    protected String id;
    @JsonProperty("exp")
    protected long expiration;
    @JsonProperty("nbf")
    protected long notBefore;
    @JsonProperty("iat")
    protected long issuedAt;
    @JsonProperty("iss")
    protected String issuer;
    @JsonProperty("aud")
    protected String audience;
    @JsonProperty("prn")
    protected String principal;
    @JsonProperty("typ")
    protected String type;

    public String getId() {
        return id;
    }

    public JsonWebToken id(String id) {
        this.id = id;
        return this;
    }


    public long getExpiration() {
        return expiration;
    }

    public JsonWebToken expiration(long expiration) {
        this.expiration = expiration;
        return this;
    }

    @JsonIgnore
    public boolean isExpired() {
        long time = System.currentTimeMillis() / 1000;
        return time > expiration;
    }

    public long getNotBefore() {
        return notBefore;
    }

    public JsonWebToken notBefore(long notBefore) {
        this.notBefore = notBefore;
        return this;
    }


    @JsonIgnore
    public boolean isNotBefore() {
        return (System.currentTimeMillis() / 1000) >= notBefore;

    }

    /**
     * Tests that the token is not expired and is not-before.
     *
     * @return
     */
    @JsonIgnore
    public boolean isActive() {
        return (!isExpired() || expiration == 0) && (isNotBefore() || notBefore == 0);
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    /**
     * Set issuedAt to the current time
     */
    @JsonIgnore
    public JsonWebToken issuedNow() {
        issuedAt = System.currentTimeMillis() / 1000;
        return this;
    }

    public JsonWebToken issuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }


    public String getIssuer() {
        return issuer;
    }

    public JsonWebToken issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }


    public String getAudience() {
        return audience;
    }

    public JsonWebToken audience(String audience) {
        this.audience = audience;
        return this;
    }

    public String getPrincipal() {
        return principal;
    }

    public JsonWebToken principal(String principal) {
        this.principal = principal;
        return this;
    }

    public String getType() {
        return type;
    }

    public JsonWebToken type(String type) {
        this.type = type;
        return this;
    }
}

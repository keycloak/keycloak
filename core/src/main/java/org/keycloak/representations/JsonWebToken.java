package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.keycloak.util.Time;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JsonWebToken implements Serializable {
    @JsonProperty("jti")
    protected String id;
    @JsonProperty("exp")
    protected int expiration;
    @JsonProperty("nbf")
    protected int notBefore;
    @JsonProperty("iat")
    protected int issuedAt;
    @JsonProperty("iss")
    protected String issuer;
    @JsonProperty("aud")
    protected String audience;
    @JsonProperty("sub")
    protected String subject;
    @JsonProperty("typ")
    protected String type;
    @JsonProperty("azp")
    public String issuedFor;

    public String getId() {
        return id;
    }

    public JsonWebToken id(String id) {
        this.id = id;
        return this;
    }


    public int getExpiration() {
        return expiration;
    }

    public JsonWebToken expiration(int expiration) {
        this.expiration = expiration;
        return this;
    }

    @JsonIgnore
    public boolean isExpired() {
        return Time.currentTime() > expiration;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public JsonWebToken notBefore(int notBefore) {
        this.notBefore = notBefore;
        return this;
    }


    @JsonIgnore
    public boolean isNotBefore() {
        return Time.currentTime() >= notBefore;

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

    public int getIssuedAt() {
        return issuedAt;
    }

    /**
     * Set issuedAt to the current time
     */
    @JsonIgnore
    public JsonWebToken issuedNow() {
        issuedAt = Time.currentTime();
        return this;
    }

    public JsonWebToken issuedAt(int issuedAt) {
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

    public String getSubject() {
        return subject;
    }

    public JsonWebToken subject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getType() {
        return type;
    }

    public JsonWebToken type(String type) {
        this.type = type;
        return this;
    }

    /**
     * OAuth client the token was issued for.
     *
     * @return
     */
    public String getIssuedFor() {
        return issuedFor;
    }

    public JsonWebToken issuedFor(String issuedFor) {
        this.issuedFor = issuedFor;
        return this;
    }
}

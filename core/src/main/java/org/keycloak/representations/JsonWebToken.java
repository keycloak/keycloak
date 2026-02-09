/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.representations;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.Token;
import org.keycloak.TokenCategory;
import org.keycloak.common.util.Time;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JsonWebToken implements Serializable, Token {
    public static final String AZP = "azp";
    public static final String AUD = "aud";
    public static final String SUBJECT = "sub";

    @JsonProperty("jti")
    protected String id;

    protected Long exp;
    protected Long nbf;
    protected Long iat;

    @JsonProperty("iss")
    protected String issuer;
    @JsonProperty(AUD)
    @JsonSerialize(using = StringOrArraySerializer.class)
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    protected String[] audience;
    @JsonProperty(SUBJECT)
    protected String subject;
    @JsonProperty("typ")
    protected String type;
    @JsonProperty(AZP)
    public String issuedFor;
    protected Map<String, Object> otherClaims = new HashMap<>();

    public String getId() {
        return id;
    }

    public JsonWebToken id(String id) {
        this.id = id;
        return this;
    }

    public Long getExp() {
        return exp;
    }

    public JsonWebToken exp(Long exp) {
        this.exp = exp;
        return this;
    }

    @JsonIgnore
    public boolean isExpired() {
        return exp != null && exp != 0 && Time.currentTime() > exp;
    }

    public Long getNbf() {
        return nbf;
    }

    public JsonWebToken nbf(Long nbf) {
        this.nbf = nbf;
        return this;
    }

    @JsonIgnore
    public boolean isNotBefore(long allowedTimeSkew) {
        return nbf == null || Time.currentTime() + allowedTimeSkew >= nbf;
    }

    /**
     * Tests that the token is not expired and is not-before.
     * This assumes a default clock-skew for the "is not before" of 10 seconds which is in line FAPI 2.0.
     * See <a href="https://openid.net/specs/fapi-security-profile-2_0-final.html#section-5.3.2.1-6">FAPI 2.0 Security Profile</a>:
     * <blockquote>
     * Clock skew is a cause of many interoperability issues. Even a few hundred milliseconds of clock skew can cause JWTs to be rejected
     * for being "issued in the future". The DPoP specification [RFC9449] suggests that JWTs are accepted in the reasonably near future
     * (on the order of seconds or minutes). This document goes further by requiring authorization servers to accept JWTs that have
     * timestamps up to 10 seconds in the future. 10 seconds was chosen as a value that does not affect security while greatly increasing
     * interoperability. Implementers are free to accept JWTs with a timestamp of up to 60 seconds in the future. Some ecosystems
     * have found that the value of 30 seconds is needed to fully eliminate clock skew issues. To prevent implementations switching
     * off iat and nbf checks completely this document imposes a maximum timestamp in the future of 60 seconds.
     * </blockquote>
     */
    @JsonIgnore
    public boolean isActive() {
        return isActive(10);
    }

    @JsonIgnore
    public boolean isActive(int allowedTimeSkew) {
        return !isExpired() && isNotBefore(allowedTimeSkew);
    }

    /**
     * @param sessionStarted Time in seconds
     * @return true if the particular token was issued before the given session start time. Which means that token cannot be issued by the particular session
     */
    @JsonIgnore
    public boolean isIssuedBeforeSessionStart(long sessionStarted) {
        return getIat() + 1 < sessionStarted;
    }

    public Long getIat() {
        return iat;
    }

    /**
     * Set issuedAt to the current time
     */
    @JsonIgnore
    public JsonWebToken issuedNow() {
        iat = (long) Time.currentTime();
        return this;
    }

    public JsonWebToken iat(Long iat) {
        this.iat = iat;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public JsonWebToken issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    @JsonIgnore
    public String[] getAudience() {
        return audience;
    }

    public boolean hasAudience(String audience) {
        if (this.audience == null) return false;
        for (String a : this.audience) {
            if (a.equals(audience)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyAudience(List<String> audiences) {
        String[] auds = getAudience();

        if (auds == null) {
            return false;
        }

        for (String aud : auds) {
            if (audiences.contains(aud)) {
                return true;
            }
        }

        return false;
    }

    public JsonWebToken audience(String... audience) {
        this.audience = audience;
        return this;
    }

    public JsonWebToken addAudience(String audience) {
        if (this.audience == null) {
            this.audience = new String[] { audience };
        } else {
            // Check if audience is already there
            for (String aud : this.audience) {
                if (audience.equals(aud)) {
                    return this;
                }
            }

            String[] newAudience = Arrays.copyOf(this.audience, this.audience.length + 1);
            newAudience[this.audience.length] = audience;
            this.audience = newAudience;
        }
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public JsonWebToken subject(String subject) {
        this.subject = subject;
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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

    /**
     * This is a map of any other claims and data that might be in the IDToken.  Could be custom claims set up by the auth server
     *
     * @return
     */
    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.INTERNAL;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof JsonWebToken)) {
            return false;
        }

        JsonWebToken that = (JsonWebToken) o;
        return Objects.equals(id, that.id) && //
                Objects.equals(exp, that.exp) && //
                Objects.equals(nbf, that.nbf) && //
                Objects.equals(iat, that.iat) && //
                Objects.equals(issuer, that.issuer) && //
                Arrays.equals(audience, that.audience) && //
                Objects.equals(subject, that.subject) && //
                Objects.equals(type, that.type) && //
                Objects.equals(issuedFor, that.issuedFor) && //
                Objects.equals(otherClaims, that.otherClaims);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(exp);
        result = 31 * result + Objects.hashCode(nbf);
        result = 31 * result + Objects.hashCode(iat);
        result = 31 * result + Objects.hashCode(issuer);
        result = 31 * result + Arrays.hashCode(audience);
        result = 31 * result + Objects.hashCode(subject);
        result = 31 * result + Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(issuedFor);
        result = 31 * result + Objects.hashCode(otherClaims);
        return result;
    }

    @Override
    public String toString() {
        try {
            return JsonSerialization.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

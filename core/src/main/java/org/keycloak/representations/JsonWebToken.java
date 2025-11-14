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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.Token;
import org.keycloak.TokenCategory;
import org.keycloak.common.util.Time;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;

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
     *
     * @return
     */
    @JsonIgnore
    public boolean isActive() {
        return isActive(0);
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
}

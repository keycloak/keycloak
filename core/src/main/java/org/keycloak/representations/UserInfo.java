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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pedroigor
 */
public class UserInfo {

    // Should be in signed UserInfo response
    @JsonProperty("iss")
    protected String issuer;
    @JsonProperty("aud")
    @JsonSerialize(using = StringOrArraySerializer.class)
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    protected String[] audience;

    @JsonProperty("sub")
    protected String sub;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("given_name")
    protected String givenName;

    @JsonProperty("family_name")
    protected String familyName;

    @JsonProperty("middle_name")
    protected String middleName;

    @JsonProperty("nickname")
    protected String nickName;

    @JsonProperty("preferred_username")
    protected String preferredUsername;

    @JsonProperty("profile")
    protected String profile;

    @JsonProperty("picture")
    protected String picture;

    @JsonProperty("website")
    protected String website;

    @JsonProperty("email")
    protected String email;

    @JsonProperty("email_verified")
    protected Boolean emailVerified;

    @JsonProperty("gender")
    protected String gender;

    @JsonProperty("birthdate")
    protected String birthdate;

    @JsonProperty("zoneinfo")
    protected String zoneinfo;

    @JsonProperty("locale")
    protected String locale;

    @JsonProperty("phone_number")
    protected String phoneNumber;

    @JsonProperty("phone_number_verified")
    protected Boolean phoneNumberVerified;

    @JsonProperty("address")
    protected AddressClaimSet address;

    @JsonProperty("updated_at")
    protected Long updatedAt;

    @JsonProperty("claims_locales")
    protected String claimsLocales;

    protected Map<String, Object> otherClaims = new HashMap<>();

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @JsonIgnore
    public String[] getAudience() {
        return audience;
    }

    public boolean hasAudience(String audience) {
        for (String a : this.audience) {
            if (a.equals(audience)) {
                return true;
            }
        }
        return false;
    }

    public void setAudience(String... audience) {
        this.audience = audience;
    }

    public String getSubject() {
        return this.sub;
    }

    public void setSubject(String subject) {
        this.sub = subject;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGivenName() {
        return this.givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return this.familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getMiddleName() {
        return this.middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPreferredUsername() {
        return this.preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public String getProfile() {
        return this.profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getPicture() {
        return this.picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getWebsite() {
        return this.website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerified() {
        return this.emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getGender() {
        return this.gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthdate() {
        return this.birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getZoneinfo() {
        return this.zoneinfo;
    }

    public void setZoneinfo(String zoneinfo) {
        this.zoneinfo = zoneinfo;
    }

    public String getLocale() {
        return this.locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getPhoneNumberVerified() {
        return this.phoneNumberVerified;
    }

    public void setPhoneNumberVerified(Boolean phoneNumberVerified) {
        this.phoneNumberVerified = phoneNumberVerified;
    }

    public AddressClaimSet getAddress() {
        return address;
    }

    public void setAddress(AddressClaimSet address) {
        this.address = address;
    }

    public Long getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSub() {
        return this.sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getClaimsLocales() {
        return this.claimsLocales;
    }

    public void setClaimsLocales(String claimsLocales) {
        this.claimsLocales = claimsLocales;
    }

    /**
     * This is a map of any other claims and data that might be in the UserInfo.  Could be custom claims set up by the auth server
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
}

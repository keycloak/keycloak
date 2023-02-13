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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.TokenCategory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IDToken extends JsonWebToken {
    public static final String NONCE = "nonce";
    public static final String AUTH_TIME = "auth_time";
    public static final String SESSION_STATE = "session_state";
    public static final String AT_HASH = "at_hash";
    public static final String C_HASH = "c_hash";
    public static final String NAME = "name";
    public static final String GIVEN_NAME = "given_name";
    public static final String FAMILY_NAME = "family_name";
    public static final String MIDDLE_NAME = "middle_name";
    public static final String NICKNAME = "nickname";
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String PROFILE = "profile";
    public static final String PICTURE = "picture";
    public static final String WEBSITE = "website";
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email_verified";
    public static final String GENDER = "gender";
    public static final String BIRTHDATE = "birthdate";
    public static final String ZONEINFO = "zoneinfo";
    public static final String LOCALE = "locale";
    public static final String PHONE_NUMBER = "phone_number";
    public static final String PHONE_NUMBER_VERIFIED = "phone_number_verified";
    public static final String ADDRESS = "address";
    public static final String UPDATED_AT = "updated_at";
    public static final String CLAIMS_LOCALES = "claims_locales";
    public static final String ACR = "acr";
    public static final String SESSION_ID = "sid";

    // Financial API - Part 2: Read and Write API Security Profile
    // http://openid.net/specs/openid-financial-api-part-2.html#authorization-server
    public static final String S_HASH = "s_hash";

    // NOTE!!!  WE used to use @JsonUnwrapped on a UserClaimSet object.  This screws up otherClaims and the won't work
    // anymore.  So don't have any @JsonUnwrapped!
    @JsonProperty(NONCE)
    protected String nonce;

    protected Long auth_time;

    // session_state is deprecated, sid should be used instead
    @JsonProperty(SESSION_STATE)
    @JsonAlias(SESSION_ID)
    protected String sessionState;

    @JsonProperty(AT_HASH)
    protected String accessTokenHash;

    @JsonProperty(C_HASH)
    protected String codeHash;

    @JsonProperty(NAME)
    protected String name;

    @JsonProperty(GIVEN_NAME)
    protected String givenName;

    @JsonProperty(FAMILY_NAME)
    protected String familyName;

    @JsonProperty(MIDDLE_NAME)
    protected String middleName;

    @JsonProperty(NICKNAME)
    protected String nickName;

    @JsonProperty(PREFERRED_USERNAME)
    protected String preferredUsername;

    @JsonProperty(PROFILE)
    protected String profile;

    @JsonProperty(PICTURE)
    protected String picture;

    @JsonProperty(WEBSITE)
    protected String website;

    @JsonProperty(EMAIL)
    protected String email;

    @JsonProperty(EMAIL_VERIFIED)
    protected Boolean emailVerified;

    @JsonProperty(GENDER)
    protected String gender;

    @JsonProperty(BIRTHDATE)
    protected String birthdate;

    @JsonProperty(ZONEINFO)
    protected String zoneinfo;

    @JsonProperty(LOCALE)
    protected String locale;

    @JsonProperty(PHONE_NUMBER)
    protected String phoneNumber;

    @JsonProperty(PHONE_NUMBER_VERIFIED)
    protected Boolean phoneNumberVerified;

    @JsonProperty(ADDRESS)
    protected AddressClaimSet address;

    @JsonProperty(UPDATED_AT)
    protected Long updatedAt;

    @JsonProperty(CLAIMS_LOCALES)
    protected String claimsLocales;

    @JsonProperty(ACR)
    protected String acr;

    // Financial API - Part 2: Read and Write API Security Profile
    // http://openid.net/specs/openid-financial-api-part-2.html#authorization-server
    @JsonProperty(S_HASH)
    protected String stateHash; 

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Long getAuth_time() {
        return auth_time;
    }

    /**
     * @deprecated int will overflow with values after 2038. Use {@link #getAuth_time()} instead.
     */
    @Deprecated
    @JsonIgnore
    public int getAuthTime() {
        return auth_time != null ? auth_time.intValue() : 0;
    }

    public void setAuth_time(Long auth_time) {
        this.auth_time = auth_time;
    }

    /**
     * @deprecated int will overflow with values after 2038. Use {@link #setAuth_time(Long)} ()} instead.
     */
    public void setAuthTime(int authTime) {
        this.auth_time = Long.valueOf(authTime);
    }

    @JsonProperty(SESSION_ID)
    public String getSessionId() {
        return sessionState;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public String getAccessTokenHash() {
        return accessTokenHash;
    }

    public void setAccessTokenHash(String accessTokenHash) {
        this.accessTokenHash = accessTokenHash;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
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

    public String getClaimsLocales() {
        return this.claimsLocales;
    }

    public void setClaimsLocales(String claimsLocales) {
        this.claimsLocales = claimsLocales;
    }

    public String getAcr() {
        return acr;
    }

    public void setAcr(String acr) {
        this.acr = acr;
    }

    // Financial API - Part 2: Read and Write API Security Profile
    // http://openid.net/specs/openid-financial-api-part-2.html#authorization-server
    public String getStateHash() {
        return stateHash;
    }

    public void setStateHash(String stateHash) {
        this.stateHash = stateHash;
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.ID;
    }

}

package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonUnwrapped;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IDToken extends JsonWebToken {
    public static final String NONCE = "nonce";
    public static final String SESSION_STATE = "session_state";
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
    // NOTE!!!  WE used to use @JsonUnwrapped on a UserClaimSet object.  This screws up otherClaims and the won't work
    // anymore.  So don't have any @JsonUnwrapped!
    @com.fasterxml.jackson.annotation.JsonProperty(NONCE)
    @JsonProperty(NONCE)
    protected String nonce;

    @com.fasterxml.jackson.annotation.JsonProperty(SESSION_STATE)
    @JsonProperty(SESSION_STATE)
    protected String sessionState;

    @com.fasterxml.jackson.annotation.JsonProperty(NAME)
    @JsonProperty(NAME)
    protected String name;

    @com.fasterxml.jackson.annotation.JsonProperty(GIVEN_NAME)
    @JsonProperty(GIVEN_NAME)
    protected String givenName
            ;
    @com.fasterxml.jackson.annotation.JsonProperty(FAMILY_NAME)
    @JsonProperty(FAMILY_NAME)
    protected String familyName;

    @com.fasterxml.jackson.annotation.JsonProperty(MIDDLE_NAME)
    @JsonProperty(MIDDLE_NAME)
    protected String middleName;

    @com.fasterxml.jackson.annotation.JsonProperty(NICKNAME)
    @JsonProperty(NICKNAME)
    protected String nickName;

    @com.fasterxml.jackson.annotation.JsonProperty(PREFERRED_USERNAME)
    @JsonProperty(PREFERRED_USERNAME)
    protected String preferredUsername;

    @com.fasterxml.jackson.annotation.JsonProperty(PROFILE)
    @JsonProperty(PROFILE)
    protected String profile;

    @com.fasterxml.jackson.annotation.JsonProperty(PICTURE)
    @JsonProperty(PICTURE)
    protected String picture;

    @com.fasterxml.jackson.annotation.JsonProperty(WEBSITE)
    @JsonProperty(WEBSITE)
    protected String website;

    @com.fasterxml.jackson.annotation.JsonProperty(EMAIL)
    @JsonProperty(EMAIL)
    protected String email;

    @com.fasterxml.jackson.annotation.JsonProperty(EMAIL_VERIFIED)
    @JsonProperty(EMAIL_VERIFIED)
    protected Boolean emailVerified;

    @com.fasterxml.jackson.annotation.JsonProperty(GENDER)
    @JsonProperty(GENDER)
    protected String gender;

    @com.fasterxml.jackson.annotation.JsonProperty(BIRTHDATE)
    @JsonProperty(BIRTHDATE)
    protected String birthdate;

    @com.fasterxml.jackson.annotation.JsonProperty(ZONEINFO)
    @JsonProperty(ZONEINFO)
    protected String zoneinfo;

    @com.fasterxml.jackson.annotation.JsonProperty(LOCALE)
    @JsonProperty(LOCALE)
    protected String locale;

    @com.fasterxml.jackson.annotation.JsonProperty(PHONE_NUMBER)
    @JsonProperty(PHONE_NUMBER)
    protected String phoneNumber;

    @com.fasterxml.jackson.annotation.JsonProperty(PHONE_NUMBER_VERIFIED)
    @JsonProperty(PHONE_NUMBER_VERIFIED)
    protected Boolean phoneNumberVerified;

    @com.fasterxml.jackson.annotation.JsonProperty(ADDRESS)
    @JsonProperty(ADDRESS)
    protected AddressClaimSet address;

    @com.fasterxml.jackson.annotation.JsonProperty(UPDATED_AT)
    @JsonProperty(UPDATED_AT)
    protected Long updatedAt;

    @com.fasterxml.jackson.annotation.JsonProperty(CLAIMS_LOCALES)
    @JsonProperty(CLAIMS_LOCALES)
    protected String claimsLocales;

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
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

}

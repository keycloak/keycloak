package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IDToken extends JsonWebToken {
    @JsonProperty("nonce")
    protected String nonce;

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
    protected String address;

    @JsonProperty("updated_at")
    protected Long updatedAt;

    @JsonProperty("formatted")
    protected String formattedAddress;

    @JsonProperty("street_address")
    protected String streetAddress;

    @JsonProperty("locality")
    protected String locality;

    @JsonProperty("region")
    protected String region;

    @JsonProperty("postal_code")
    protected String postalCode;

    @JsonProperty("country")
    protected String country;

    @JsonProperty("claims_locales")
    protected String claimsLocales;

    @JsonProperty("session_state")
    protected String sessionState;

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getZoneinfo() {
        return zoneinfo;
    }

    public void setZoneinfo(String zoneinfo) {
        this.zoneinfo = zoneinfo;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getPhoneNumberVerified() {
        return phoneNumberVerified;
    }

    public void setPhoneNumberVerified(Boolean phoneNumberVerified) {
        this.phoneNumberVerified = phoneNumberVerified;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getClaimsLocales() {
        return claimsLocales;
    }

    public void setClaimsLocales(String claimsLocales) {
        this.claimsLocales = claimsLocales;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }
}

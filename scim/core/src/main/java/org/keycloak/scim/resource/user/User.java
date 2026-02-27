package org.keycloak.scim.resource.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.common.Address;
import org.keycloak.scim.resource.common.Email;
import org.keycloak.scim.resource.common.InstantMessagingAddress;
import org.keycloak.scim.resource.common.Name;
import org.keycloak.scim.resource.common.PhoneNumber;
import org.keycloak.scim.resource.common.Photo;
import org.keycloak.scim.resource.common.X509Certificate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends ResourceTypeRepresentation {

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("name")
    private Name name;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("nickName")
    private String nickName;

    @JsonProperty("profileUrl")
    private String profileUrl;

    @JsonProperty("title")
    private String title;

    @JsonProperty("userType")
    private String userType;

    @JsonProperty("preferredLanguage")
    private String preferredLanguage;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("password")
    private String password;

    @JsonProperty("emails")
    private List<Email> emails;

    @JsonProperty("phoneNumbers")
    private List<PhoneNumber> phoneNumbers;

    @JsonProperty("ims")
    private List<InstantMessagingAddress> ims;

    @JsonProperty("photos")
    private List<Photo> photos;

    @JsonProperty("addresses")
    private List<Address> addresses;

    @JsonProperty("groups")
    private List<GroupMembership> groups;

    @JsonProperty("entitlements")
    private List<String> entitlements;

    @JsonProperty("roles")
    private List<String> roles;

    @JsonProperty("x509Certificates")
    private List<X509Certificate> x509Certificates;

    // Enterprise User Extension
    @JsonProperty("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User")
    private EnterpriseUser enterpriseUser;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(List<Email> emails) {
        this.emails = emails;
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public List<InstantMessagingAddress> getIms() {
        return ims;
    }

    public void setIms(List<InstantMessagingAddress> ims) {
        this.ims = ims;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public List<GroupMembership> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupMembership> groups) {
        this.groups = groups;
    }

    public List<String> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(List<String> entitlements) {
        this.entitlements = entitlements;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<X509Certificate> getX509Certificates() {
        return x509Certificates;
    }

    public void setX509Certificates(List<X509Certificate> x509Certificates) {
        this.x509Certificates = x509Certificates;
    }

    public EnterpriseUser getEnterpriseUser() {
        return enterpriseUser;
    }

    public void setEnterpriseUser(EnterpriseUser enterpriseUser) {
        this.enterpriseUser = enterpriseUser;
    }

    @JsonIgnore
    public String getFirstName() {
        return Optional.ofNullable(name).map(Name::getGivenName).orElse(null);
    }

    public void setFirstName(String firstName) {
        name = Optional.ofNullable(name).orElseGet(Name::new);
        name.setGivenName(firstName);
    }

    @JsonIgnore
    public String getLastName() {
        return Optional.ofNullable(name).map(Name::getFamilyName).orElse(null);
    }

    public void setLastName(String lastName) {
        name = Optional.ofNullable(name).orElseGet(Name::new);
        name.setFamilyName(lastName);
    }

    @JsonIgnore
    public String getEmail() {
        if (emails == null || emails.isEmpty()) {
            return null;
        }
        return emails.get(0).getValue();
    }

    public void setEmail(String email) {
        emails = List.of(new Email(email));
    }

    @Override
    public Set<String> getSchemas() {
        Set<String> schemas = super.getSchemas();
        if (enterpriseUser != null) {
            schemas.add(ENTERPRISE_USER_SCHEMA);
        }
        return schemas;
    }
}

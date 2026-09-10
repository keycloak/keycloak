package org.keycloak.scim.resource.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.keycloak.scim.resource.ResourceTypeRepresentation;

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

    @JsonProperty("emails")
    private List<Email> emails;

    @JsonProperty("groups")
    private List<GroupMembership> groups;

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

    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(List<Email> emails) {
        this.emails = emails;
    }

    public List<GroupMembership> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupMembership> groups) {
        this.groups = groups;
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

    public void addGroup(String id) {
        GroupMembership membership = new GroupMembership();

        membership.setValue(id);

        addGroup(membership);
    }

    public void addGroup(GroupMembership membership) {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        groups.add(membership);
    }
}

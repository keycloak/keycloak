package org.keycloak.models.entities;

import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserEntity extends AbstractIdentifiableEntity {

    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private boolean totp;
    private boolean enabled;

    private String realmId;

    private List<String> roleIds;

    private Map<String, String> attributes;
    private List<UserModel.RequiredAction> requiredActions;
    private List<CredentialEntity> credentials = new ArrayList<CredentialEntity>();
    private List<SocialLinkEntity> socialLinks;
    private String federationLink;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isTotp() {
        return totp;
    }

    public void setTotp(boolean totp) {
        this.totp = totp;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public List<String> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<UserModel.RequiredAction> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<UserModel.RequiredAction> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public List<CredentialEntity> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialEntity> credentials) {
        this.credentials = credentials;
    }

    public List<SocialLinkEntity> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(List<SocialLinkEntity> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public String getFederationLink() {
        return federationLink;
    }

    public void setFederationLink(String federationLink) {
        this.federationLink = federationLink;
    }
}


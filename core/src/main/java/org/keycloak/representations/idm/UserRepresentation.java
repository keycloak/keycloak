package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserRepresentation {

    protected String self; // link
    protected String id;
    protected String username;
    protected boolean enabled;
    protected boolean totp;
    protected boolean emailVerified;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String federationLink;
    protected Map<String, String> attributes;
    protected List<CredentialRepresentation> credentials;
    protected List<String> requiredActions;
    protected List<FederatedIdentityRepresentation> federatedIdentities;
    @Deprecated
    protected List<SocialLinkRepresentation> socialLinks;
    protected List<String> realmRoles;
    protected Map<String, List<String>> applicationRoles;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTotp() {
        return totp;
    }

    public void setTotp(boolean totp) {
        this.totp = totp;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public UserRepresentation attribute(String name, String value) {
        if (this.attributes == null) attributes = new HashMap<String, String>();
        attributes.put(name, value);
        return this;
    }

    public List<CredentialRepresentation> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialRepresentation> credentials) {
        this.credentials = credentials;
    }

    public UserRepresentation credential(String type, String value) {
        if (this.credentials == null) credentials = new ArrayList<CredentialRepresentation>();
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(type);
        cred.setValue(value);
        credentials.add(cred);
        return this;
    }

    public List<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<String> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public List<FederatedIdentityRepresentation> getFederatedIdentities() {
        return federatedIdentities;
    }

    public void setFederatedIdentities(List<FederatedIdentityRepresentation> federatedIdentities) {
        this.federatedIdentities = federatedIdentities;
    }

    public List<SocialLinkRepresentation> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(List<SocialLinkRepresentation> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(List<String> realmRoles) {
        this.realmRoles = realmRoles;
    }

    public Map<String, List<String>> getApplicationRoles() {
        return applicationRoles;
    }

    public void setApplicationRoles(Map<String, List<String>> applicationRoles) {
        this.applicationRoles = applicationRoles;
    }

    public String getFederationLink() {
        return federationLink;
    }

    public void setFederationLink(String federationLink) {
        this.federationLink = federationLink;
    }
}

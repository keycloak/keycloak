package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.keycloak.util.MultivaluedHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserRepresentation {

    protected String self; // link
    protected String id;
    protected Long createdTimestamp;
    protected String username;
    protected boolean enabled;
    protected boolean totp;
    protected boolean emailVerified;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String federationLink;
    protected String serviceAccountClientId; // For rep, it points to clientId (not DB ID)

    // Currently there is Map<String, List<String>> but for backwards compatibility, we also need to support Map<String, String>
    protected Map<String, Object> attributes;
    protected List<CredentialRepresentation> credentials;
    protected List<String> requiredActions;
    protected List<FederatedIdentityRepresentation> federatedIdentities;
    protected List<String> realmRoles;
    protected Map<String, List<String>> clientRoles;
    protected List<UserConsentRepresentation> clientConsents;

    @Deprecated
    protected Map<String, List<String>> applicationRoles;
    @Deprecated
    protected List<SocialLinkRepresentation> socialLinks;

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

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
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

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // This method can be removed once we can remove backwards compatibility with Keycloak 1.3 (then getAttributes() can be changed to return Map<String, List<String>> )
    @JsonIgnore
    public Map<String, List<String>> getAttributesAsListValues() {
        return (Map) attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public UserRepresentation singleAttribute(String name, String value) {
        if (this.attributes == null) attributes = new HashMap<>();
        attributes.put(name, Arrays.asList(value));
        return this;
    }

    public List<CredentialRepresentation> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialRepresentation> credentials) {
        this.credentials = credentials;
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

    public Map<String, List<String>> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(Map<String, List<String>> clientRoles) {
        this.clientRoles = clientRoles;
    }

    public List<UserConsentRepresentation> getClientConsents() {
        return clientConsents;
    }

    public void setClientConsents(List<UserConsentRepresentation> clientConsents) {
        this.clientConsents = clientConsents;
    }

    @Deprecated
    public Map<String, List<String>> getApplicationRoles() {
        return applicationRoles;
    }

    public String getFederationLink() {
        return federationLink;
    }

    public void setFederationLink(String federationLink) {
        this.federationLink = federationLink;
    }

    public String getServiceAccountClientId() {
        return serviceAccountClientId;
    }

    public void setServiceAccountClientId(String serviceAccountClientId) {
        this.serviceAccountClientId = serviceAccountClientId;
    }
}

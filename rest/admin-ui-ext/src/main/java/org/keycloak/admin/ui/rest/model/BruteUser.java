package org.keycloak.admin.ui.rest.model;

import java.util.Map;
import org.keycloak.representations.idm.UserRepresentation;

public class BruteUser extends UserRepresentation {

    Map<String, Object> bruteForceStatus;

    public BruteUser(UserRepresentation user) {
        this.id = user.getId();
        this.origin = user.getOrigin();
        this.createdTimestamp = user.getCreatedTimestamp();
        this.username = user.getUsername();
        this.enabled = user.isEnabled();
        this.totp = user.isTotp();
        this.emailVerified = user.isEmailVerified();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.federationLink = user.getFederationLink();
        this.serviceAccountClientId = user.getServiceAccountClientId();

        this.attributes = user.getAttributes();
        this.credentials = user.getCredentials();
        this.disableableCredentialTypes = user.getDisableableCredentialTypes();
        this.requiredActions = user.getRequiredActions();
        this.federatedIdentities = user.getFederatedIdentities();
        this.realmRoles = user.getRealmRoles();
        this.clientRoles = user.getClientRoles();
        this.clientConsents = user.getClientConsents();
        this.notBefore = user.getNotBefore();

        this.applicationRoles = user.getApplicationRoles();
        this.socialLinks = user.getSocialLinks();

        this.groups = user.getGroups();
        this.setAccess(user.getAccess());
    }

    public Map<String, Object> getBruteForceStatus() {
        return bruteForceStatus;
    }

    public void setBruteForceStatus(Map<String, Object> bruteForceStatus) {
        this.bruteForceStatus = bruteForceStatus;
    }
}

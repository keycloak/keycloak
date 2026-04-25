package org.keycloak.testframework.realm;

import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class UserBuilder extends Builder<UserRepresentation> {

    protected UserBuilder(UserRepresentation rep) {
        super(rep);
    }

    public static UserBuilder create() {
        return new UserBuilder(new UserRepresentation()).enabled(true);
    }

    public static UserBuilder update(UserRepresentation rep) {
        return new UserBuilder(rep);
    }

    public UserBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public UserBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public UserBuilder username(String username) {
        rep.setUsername(username);
        return this;
    }

    public UserBuilder name(String firstName, String lastName) {
        return firstName(firstName).lastName(lastName);
    }

    public UserBuilder email(String email) {
        rep.setEmail(email);
        return this;
    }

    public UserBuilder firstName(String firstName) {
        rep.setFirstName(firstName);
        return this;
    }

    public UserBuilder lastName(String lastName) {
        rep.setLastName(lastName);
        return this;
    }

    public UserBuilder emailVerified(boolean verified) {
        rep.setEmailVerified(verified);
        return this;
    }

    public UserBuilder credential(CredentialRepresentation credential) {
        rep.setCredentials(combine(rep.getCredentials(), credential));
        return this;
    }

    public UserBuilder credential(CredentialBuilder credential) {
        return credential(credential.build());
    }

    public UserBuilder password(String password) {
        return credential(CredentialBuilder.password(password));
    }

    public UserBuilder totpSecret(String totpSecret) {
        return credential(CredentialBuilder.totp(totpSecret));
    }

    public UserBuilder hotpSecret(String hotpSecret) {
        return credential(CredentialBuilder.hotp(hotpSecret));
    }

    public UserBuilder roles(String... roles) {
        rep.setRealmRoles(combine(rep.getRealmRoles(), roles));
        return this;
    }

    public UserBuilder clientRoles(String client, String... roles) {
        rep.setClientRoles(combine(rep.getClientRoles(), client, roles));
        return this;
    }

    public UserBuilder requiredActions(String... requiredActions) {
        rep.setRequiredActions(combine(rep.getRequiredActions(), requiredActions));
        return this;
    }

    public UserBuilder groups(String... groups) {
        rep.setGroups(combine(rep.getGroups(), groups));
        return this;
    }

    public UserBuilder attribute(String key, String... value) {
        rep.setAttributes(combine(rep.getAttributes(), key, value));
        return this;
    }

    public UserBuilder attributes(Map<String, List<String>> attributes) {
        rep.setAttributes(combine(rep.getAttributes(), attributes));
        return this;
    }

    public UserBuilder federatedLink(FederatedIdentityRepresentation federatedIdentity) {
        rep.setFederatedIdentities(combine(rep.getFederatedIdentities(), federatedIdentity));
        return this;
    }

    public UserBuilder federatedLink(FederatedIdentityBuilder federatedIdentity) {
        return federatedLink(federatedIdentity.build());
    }

    public UserBuilder federatedLink(String identityProvider, String federatedUserId, String federatedUsername) {
        return federatedLink(FederatedIdentityBuilder.create(identityProvider, federatedUserId, federatedUsername));
    }

    public UserBuilder serviceAccountId(String serviceAccountClientId) {
        rep.setServiceAccountClientId(serviceAccountClientId);
        return this;
    }

}

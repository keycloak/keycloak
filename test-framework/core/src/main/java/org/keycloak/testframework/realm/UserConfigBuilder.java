package org.keycloak.testframework.realm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class UserConfigBuilder {

    private final UserRepresentation rep;

    private UserConfigBuilder(UserRepresentation rep) {
        this.rep = rep;
    }

    public static UserConfigBuilder create() {
        UserRepresentation rep = new UserRepresentation();
        rep.setEnabled(true);
        return new UserConfigBuilder(rep);
    }

    public static UserConfigBuilder update(UserRepresentation rep) {
        return new UserConfigBuilder(rep);
    }

    public UserConfigBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public UserConfigBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public UserConfigBuilder username(String username) {
        rep.setUsername(username);
        return this;
    }

    public UserConfigBuilder name(String firstName, String lastName) {
        rep.setFirstName(firstName);
        rep.setLastName(lastName);
        return this;
    }

    public UserConfigBuilder email(String email) {
        rep.setEmail(email);
        return this;
    }

    public UserConfigBuilder firstName(String firstName) {
        rep.setFirstName(firstName);
        return this;
    }

    public UserConfigBuilder lastName(String lastName) {
        rep.setLastName(lastName);
        return this;
    }

    public UserConfigBuilder emailVerified(boolean verified) {
        rep.setEmailVerified(verified);
        return this;
    }

    public UserConfigBuilder password(String password) {
        rep.setCredentials(Collections.combine(rep.getCredentials(), Representations.toCredential(CredentialRepresentation.PASSWORD, password)));
        return this;
    }

    public UserConfigBuilder roles(String... roles) {
        rep.setRealmRoles(Collections.combine(rep.getRealmRoles(), roles));
        return this;
    }

    public UserConfigBuilder clientRoles(String client, String... roles) {
        rep.setClientRoles(Collections.combine(rep.getClientRoles(), client, roles));
        return this;
    }

    public UserConfigBuilder requiredActions(String... requiredActions) {
        rep.setRequiredActions(Collections.combine(rep.getRequiredActions(), requiredActions));
        return this;
    }

    public UserConfigBuilder groups(String... groups) {
        rep.setGroups(Collections.combine(rep.getGroups(), groups));
        return this;
    }

    public UserConfigBuilder attribute(String key, String... value) {
        rep.setAttributes(Collections.combine(rep.getAttributes(), key, value));
        return this;
    }

    public UserConfigBuilder attributes(Map<String, List<String>> attributes) {
        rep.setAttributes(Collections.combine(rep.getAttributes(), attributes));
        return this;
    }

    public UserConfigBuilder federatedLink(String identityProvider, String federatedUserId, String federatedUsername) {
        FederatedIdentityRepresentation federatedIdentity = new FederatedIdentityRepresentation();
        federatedIdentity.setUserId(federatedUserId);
        federatedIdentity.setUserName(federatedUsername);
        federatedIdentity.setIdentityProvider(identityProvider);

        rep.setFederatedIdentities(Collections.combine(rep.getFederatedIdentities(), federatedIdentity));
        return this;
    }

    public UserConfigBuilder totpSecret(String totpSecret) {
        rep.setCredentials(Collections.combine(rep.getCredentials(), ModelToRepresentation.toRepresentation(
                OTPCredentialModel.createTOTP(totpSecret, 6, 30, HmacOTP.HMAC_SHA1))));
        rep.setTotp(true);
        return this;
    }

    /**
     * Best practice is to use other convenience methods when configuring a user, but while the framework is under
     * active development there may not be a way to perform all updates required. In these cases this method allows
     * applying any changes to the underlying representation.
     *
     * @param update
     * @return this
     * @deprecated
     */
    public UserConfigBuilder update(UserUpdate... update) {
        Arrays.stream(update).forEach(u -> u.update(rep));
        return this;
    }

    public UserRepresentation build() {
        return rep;
    }

    public interface UserUpdate {

        void update(UserRepresentation client);

    }

}

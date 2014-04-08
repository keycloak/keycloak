package org.keycloak.authentication.model;

import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.authentication.AuthProviderStatus;
import org.keycloak.authentication.AuthUser;
import org.keycloak.authentication.AuthenticationProvider;
import org.keycloak.authentication.AuthenticationProviderException;

/**
 * Authentication provider, which delegates calling of all methods to specified realm
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractModelAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = Logger.getLogger(AbstractModelAuthenticationProvider.class);

    @Override
    public AuthUser getUser(RealmModel currentRealm, Map<String, String> config, String username) throws AuthenticationProviderException {
        RealmModel realm = getRealm(currentRealm, config);
        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(realm, username);
        return user == null ? null : createAuthenticatedUserInstance(user);
    }

    @Override
    public String registerUser(RealmModel currentRealm, Map<String, String> config, String username) throws AuthenticationProviderException {
        RealmModel realm = getRealm(currentRealm, config);
        UserModel user = currentRealm.addUser(username);
        user.setEnabled(true);
        return user.getId();
    }

    @Override
    public AuthProviderStatus validatePassword(RealmModel currentRealm, Map<String, String> config, String username, String password) throws AuthenticationProviderException {
        RealmModel realm = getRealm(currentRealm, config);
        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(realm, username);

        boolean result = realm.validatePassword(user, password);
        return result ? AuthProviderStatus.SUCCESS : AuthProviderStatus.INVALID_CREDENTIALS;
    }

    @Override
    public boolean updateCredential(RealmModel currentRealm, Map<String, String> config, String username, String password) throws AuthenticationProviderException {
        RealmModel realm = getRealm(currentRealm, config);

        // Validate password policy
        String error = realm.getPasswordPolicy().validate(password);
        if (error != null) {
            throw new AuthenticationProviderException(error);
        }

        UserModel user = realm.getUser(username);
        if (user == null) {
            logger.warnf("User '%s' doesn't exists. Skip password update", username);
            return false;
        }

        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);

        realm.updateCredential(user, cred);
        return true;
    }

    @Override
    public void close() {
    }

    protected abstract RealmModel getRealm(RealmModel currentRealm, Map<String, String> config) throws AuthenticationProviderException;

    protected AuthUser createAuthenticatedUserInstance(UserModel user) {
        return new AuthUser(user.getId(), user.getLoginName(), getName())
                .setName(user.getFirstName(), user.getLastName())
                .setEmail(user.getEmail());
    }
}

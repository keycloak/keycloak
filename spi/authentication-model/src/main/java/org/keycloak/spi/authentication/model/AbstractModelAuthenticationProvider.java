package org.keycloak.spi.authentication.model;

import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.spi.authentication.AuthProviderStatus;
import org.keycloak.spi.authentication.AuthResult;
import org.keycloak.spi.authentication.AuthenticatedUser;
import org.keycloak.spi.authentication.AuthenticationProvider;
import org.keycloak.spi.authentication.AuthenticationProviderException;

/**
 * Authentication provider, which delegates calling of all methods to specified realm
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractModelAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = Logger.getLogger(AbstractModelAuthenticationProvider.class);

    @Override
    public AuthResult validatePassword(RealmModel currentRealm, Map<String, String> config, String username, String password) throws AuthenticationProviderException {
        RealmModel realm = getRealm(currentRealm, config);

        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(realm, username);

        if (user == null) {
            return new AuthResult(AuthProviderStatus.USER_NOT_FOUND);
        }

        boolean result = realm.validatePassword(user, password);
        if (!result) {
            return  new AuthResult(AuthProviderStatus.INVALID_CREDENTIALS);
        }

        AuthenticatedUser authUser = createAuthenticatedUserInstance(user);
        return new AuthResult(AuthProviderStatus.SUCCESS).setProviderName(getName()).setUser(authUser);
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
            logger.debugf("User '%s' doesn't exists. Skip password update", username);
            return false;
        }

        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);

        realm.updateCredential(user, cred);
        return true;
    }

    protected abstract RealmModel getRealm(RealmModel currentRealm, Map<String, String> config) throws AuthenticationProviderException;

    protected abstract AuthenticatedUser createAuthenticatedUserInstance(UserModel user);
}

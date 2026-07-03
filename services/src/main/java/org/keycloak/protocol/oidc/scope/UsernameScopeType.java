package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

/**
 * Parameterized scope type that validates the parameter is an existing username in the realm.
 */
public class UsernameScopeType implements ParameterizedScopeTypeProvider {

    public static final String TYPE = "username";

    protected final KeycloakSession session;

    public UsernameScopeType() {
        this.session = null;
    }

    public UsernameScopeType(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getTypeName() {
        return TYPE;
    }

    @Override
    public ParameterizedScopeTypeProvider create(KeycloakSession session) {
        return new UsernameScopeType(session);
    }

    @Override
    public void validateParameter(@Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        if (StringUtil.isBlank(parameter)) {
            throw new InvalidScopeParameterException("Username parameter must not be blank");
        }
        resolveUser(scope, parameter);
    }

    @Override
    public void validateParameterWithUser(@Nonnull UserModel currentUser, @Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        UserModel targetUser = resolveUser(scope, parameter);
        if (targetUser.getId().equals(currentUser.getId())) {
            throw new InvalidScopeParameterException("User cannot target themselves");
        }
    }

    protected UserModel resolveUser(ClientScopeModel scope, String parameter) throws InvalidScopeParameterException {
        RealmModel realm = scope.getRealm();
        UserModel targetUser = session.users().getUserByUsername(realm, parameter);
        if (targetUser == null && realm.isLoginWithEmailAllowed() && parameter.contains("@")) {
            targetUser = session.users().getUserByEmail(realm, parameter);
        }
        if (targetUser == null) {
            throw new InvalidScopeParameterException(String.format("User '%s' not found in realm '%s'", parameter, realm.getName()));
        }
        if (!targetUser.isEnabled()) {
            throw new InvalidScopeParameterException(String.format("User '%s' is disabled in realm '%s'", parameter, scope.getRealm().getName()));
        }
        return targetUser;
    }
}

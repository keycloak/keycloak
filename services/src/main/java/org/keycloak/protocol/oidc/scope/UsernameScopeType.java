package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * Parameterized scope type that validates the parameter is an existing username in the realm.
 */
public class UsernameScopeType implements ParameterizedScopeTypeProvider {

    private static final Logger logger = Logger.getLogger(UsernameScopeType.class);

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

    /**
     * Checks whether the authenticated user is allowed to access the target user's data for this scope.
     */
    public boolean canAccessTargetUser(ClientScopeModel scope, UserModel currentUser, UserModel targetUser) {
        if (scope.isAllowUserDataAccess()) {
            return true;
        }
        return AdminPermissions.evaluator(session, scope.getRealm(), scope.getRealm(), currentUser).users().canView(targetUser);
    }

    @Override
    public void validateParameter(@Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        if (StringUtil.isBlank(parameter)) {
            throw new InvalidScopeParameterException("Username parameter must not be blank");
        }
    }

    @Override
    public void validateParameterWithUser(@Nonnull UserModel currentUser, @Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        validateParameterWithUser(currentUser, scope, null, parameter);
    }

    @Override
    public void validateParameterWithUser(@Nonnull UserModel currentUser,
                                          @Nonnull ClientScopeModel scope,
                                          @Nullable AuthenticatedClientSessionModel clientSession,
                                          @Nonnull String parameter) throws InvalidScopeParameterException {
        UserModel targetUser = resolveUser(scope, parameter);
        if (targetUser.getId().equals(currentUser.getId())) {
            throw new InvalidScopeParameterException("User cannot target themselves");
        }
        if (clientSession != null) {
            verifyPinnedIdentity(clientSession, parameter, targetUser.getId());
        }
    }

    protected void verifyPinnedIdentity(AuthenticatedClientSessionModel clientSession,
                                        String parameterValue,
                                        String resolvedId) throws InvalidScopeParameterException {
        String noteKey = PINNED_IDENTITY_NOTE_PREFIX + getTypeName() + "." + parameterValue;
        String pinnedId = clientSession.getNote(noteKey);
        if (pinnedId == null) {
            clientSession.setNote(noteKey, resolvedId);
            return;
        }
        if (!pinnedId.equals(resolvedId)) {
            logger.debugf("Rejecting scope parameter '%s': resolved identity changed since consent (pinned=%s, resolved=%s)", parameterValue, pinnedId, resolvedId);
            throw new InvalidScopeParameterException(String.format("Resolved identity for '%s' changed since consent was granted", parameterValue));
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

package org.keycloak.services.resources.admin;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Access;
import org.keycloak.util.TokenUtil;

import static org.keycloak.models.AdminRoles.isAdminClient;

/**
 * A {@link TokenPostProcessor} that removes from access tokens any admin role not explicitly granted to the
 * subject.
 */
public class AdminRoleTokenPostProcessor implements TokenPostProcessor {

    private final KeycloakSession session;

    public AdminRoleTokenPostProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void process(TokenPostProcessorContext context) {
        ClientSessionContext clientSessionCtx = context.clientSessionCtx();
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        UserModel user = userSession.getUser();
        AccessToken accessToken = context.accessToken();

        TokenUtil.convertTokenRolesFromOtherClaims(accessToken);
        removeTransientAdminRoles(user, accessToken);
    }

    private void removeTransientAdminRoles(UserModel user, AccessToken accessToken) {
        if (!containsAdminRoles(accessToken)) {
            return;
        }

        Set<String> expandedRoles = RoleUtils.getDeepUserRoleMappings(user).stream()
                .filter(AdminRoles::isAdminRole)
                .map(RoleModel::getName)
                .collect(Collectors.toSet());

        Access realmAccess = accessToken.getRealmAccess();
        Map<String, Access> resourceAccess = accessToken.getResourceAccess();
        RealmModel realm = session.getContext().getRealm();

        if (AdminRoles.isAdminRealm(realm.getName())) {
            removeMappedRoles(realmAccess, AdminRoles.REALM_LEVEL_ROLES, expandedRoles);
        }

        if (resourceAccess != null) {
            for (Entry<String, Access> clientRole : resourceAccess.entrySet()) {
                if (isAdminClient(realm, clientRole.getKey())) {
                    removeMappedRoles(clientRole.getValue(), AdminRoles.REALM_MANAGEMENT_ROLES, expandedRoles);
                }
            }
        }
    }

    private boolean containsAdminRoles(AccessToken accessToken) {
        Access realmAccess = accessToken.getRealmAccess();
        Map<String, Access> resourceAccess = accessToken.getResourceAccess();
        RealmModel realm = session.getContext().getRealm();

        if (realmAccess == null && (resourceAccess == null || resourceAccess.isEmpty())) {
            return false;
        }

        if (AdminRoles.isAdminRealm(realm.getName()) && containsAdminRoles(realmAccess, AdminRoles.REALM_LEVEL_ROLES)) {
            return true;
        }

        if (resourceAccess != null) {
            for (Entry<String, Access> entry : resourceAccess.entrySet()) {
                if (isAdminClient(realm, entry.getKey()) && containsAdminRoles(entry.getValue(), AdminRoles.REALM_MANAGEMENT_ROLES)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean containsAdminRoles(Access access, Set<String> adminRoles) {
        if (access == null || access.getRoles() == null) {
            return false;
        }
        return access.getRoles().stream().anyMatch(adminRoles::contains);
    }

    private void removeMappedRoles(Access access, Set<String> rolesToRemove, Set<String> expandedUserRoles) {
        if (access == null || access.getRoles() == null) {
            return;
        }

        access.getRoles().removeIf(role -> rolesToRemove.contains(role) && !expandedUserRoles.contains(role));
    }
}

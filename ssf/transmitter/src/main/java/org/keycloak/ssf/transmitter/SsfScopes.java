package org.keycloak.ssf.transmitter;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.ssf.Ssf;

import org.jboss.logging.Logger;

/**
 * Manages SSF client scopes for realms.
 */
public class SsfScopes {

    public static final String SCOPE_SSF_READ = "ssf.read";

    public static final String SCOPE_SSF_MANAGE = "ssf.manage";

    private static final Logger log = Logger.getLogger(SsfScopes.class);

    private SsfScopes() {
    }

    public static void createDefaultClientScopes(RealmModel realm) {
        addClientScopeIfAbsent(realm, Ssf.SCOPE_SSF_READ, "SSF read access to stream configurations", true);
        addClientScopeIfAbsent(realm, Ssf.SCOPE_SSF_MANAGE, "SSF manage access to create, update, and delete stream configurations", true);
    }

    protected static void addClientScopeIfAbsent(RealmModel realm, String scopeName, String description, boolean includeInTokenScope) {
        ClientScopeModel existingScope = KeycloakModelUtils.getClientScopeByName(realm, scopeName);
        if (existingScope != null) {
            log.debugf("Client scope '%s' already exists in realm '%s'. Skip creating it.", scopeName, realm.getName());
            repairLegacyScope(realm, existingScope);
            return;
        }

        ClientScopeModel scope = realm.addClientScope(scopeName);
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        scope.setDescription(description);
        scope.setDisplayOnConsentScreen(false);
        scope.setIncludeInTokenScope(includeInTokenScope);
        // SSF scopes are deliberately not registered as realm default scopes: a realm-level optional scope
        // would be assigned to every newly created OIDC client, which must not be able to request ssf.manage.
        // Admins assign the scopes to each receiver client explicitly.

        log.debugf("Client scope '%s' created in realm '%s'.", scopeName, realm.getName());
    }

    /**
     * Scopes created by earlier versions were missing the protocol, which hides them from the
     * admin console's "Add client scope" dialog. Backfill the protocol on such scopes.
     * <p>
     * Earlier versions also registered the scopes as realm default optional scopes. That registration had no
     * effect while the protocol was {@code null}, but once the protocol is set every new OIDC client would
     * inherit both scopes. The missing protocol is a reliable marker for a scope provisioned by the old
     * bootstrap, so the realm default registration is undone in the same step. Scopes that already carry a
     * protocol are left untouched because an admin may have configured them deliberately.
     */
    private static void repairLegacyScope(RealmModel realm, ClientScopeModel scope) {
        if (scope.getProtocol() != null) {
            return;
        }
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        realm.removeDefaultClientScope(scope);
        log.debugf("Set missing protocol on client scope '%s' in realm '%s' and removed it from the realm default scopes.", scope.getName(), realm.getName());
    }
}

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
            ensureProtocol(realm, existingScope);
            return;
        }

        ClientScopeModel scope = realm.addClientScope(scopeName);
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        scope.setDescription(description);
        scope.setDisplayOnConsentScreen(false);
        scope.setIncludeInTokenScope(includeInTokenScope);
        // SSF scopes are optional — receivers must explicitly request them
        realm.addDefaultClientScope(scope, false);

        log.debugf("Client scope '%s' created in realm '%s'.", scopeName, realm.getName());
    }

    /**
     * Scopes created by earlier versions were missing the protocol, which hides them from the
     * admin console's "Add client scope" dialog. Backfill the protocol on existing scopes.
     */
    private static void ensureProtocol(RealmModel realm, ClientScopeModel scope) {
        if (scope.getProtocol() != null) {
            return;
        }
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        log.debugf("Set missing protocol on client scope '%s' in realm '%s'.", scope.getName(), realm.getName());
    }
}

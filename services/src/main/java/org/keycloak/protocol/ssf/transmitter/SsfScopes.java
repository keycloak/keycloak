package org.keycloak.protocol.ssf.transmitter;

import org.jboss.logging.Logger;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ssf.Ssf;

/**
 * Manages SSF client scopes for realms.
 */
public class SsfScopes {

    public static final String SCOPE_SSF_READ = "ssf.read";

    public static final String SCOPE_SSF_MANAGE = "ssf.manage";

    /**
     * NON standard internal marker scope for Apple Business Manager compatibility.
     */
    public static final String SCOPE_APPLE_ABM = "apple-abm";

    private static final Logger log = Logger.getLogger(SsfScopes.class);

    private SsfScopes() {
    }

    public static void createDefaultClientScopes(RealmModel realm) {
        addClientScopeIfAbsent(realm, Ssf.SCOPE_SSF_READ, "SSF read access to stream configurations", true);
        addClientScopeIfAbsent(realm, Ssf.SCOPE_SSF_MANAGE, "SSF manage access to create, update, and delete stream configurations", true);

        // should we do this with a scope or a client attribute?
        addClientScopeIfAbsent(realm, Ssf.SCOPE_APPLE_ABM, "SSF scope to mark an Apple Business Manager Client", false);
    }

    protected static void addClientScopeIfAbsent(RealmModel realm, String scopeName, String description, boolean includeInTokenScope) {
        ClientScopeModel existingScope = KeycloakModelUtils.getClientScopeByName(realm, scopeName);
        if (existingScope != null) {
            log.debugf("Client scope '%s' already exists in realm '%s'. Skip creating it.", scopeName, realm.getName());
            return;
        }

        ClientScopeModel scope = realm.addClientScope(scopeName);
        scope.setDescription(description);
        scope.setDisplayOnConsentScreen(false);
        scope.setIncludeInTokenScope(includeInTokenScope);
        // SSF scopes are optional — receivers must explicitly request them
        realm.addDefaultClientScope(scope, false);

        log.debugf("Client scope '%s' created in realm '%s'.", scopeName, realm.getName());
    }
}

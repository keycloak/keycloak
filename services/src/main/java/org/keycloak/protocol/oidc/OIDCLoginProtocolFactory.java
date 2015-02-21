package org.keycloak.protocol.oidc;

import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCLoginProtocolFactory implements LoginProtocolFactory {
    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OIDCLoginProtocol().setSession(session);
    }

    @Override
    public void init(Config.Scope config) {
        System.out.println("here");

    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        return new OIDCLoginProtocolService(realm, event, authManager);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "openid-connect";
    }
}

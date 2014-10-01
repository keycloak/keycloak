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
public class OpenIDConnectFactory implements LoginProtocolFactory {
    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OpenIDConnect().setSession(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        return new OpenIDConnectService(realm, event, authManager);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "openid-connect";
    }
}

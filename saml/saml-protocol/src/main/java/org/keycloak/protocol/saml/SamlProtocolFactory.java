package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.services.managers.AuthenticationManager;
import org.picketlink.identity.federation.core.sts.PicketLinkCoreSTS;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocolFactory implements LoginProtocolFactory {

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        return new SamlService(realm, event, authManager);
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new SamlProtocol().setSession(session);
    }

    @Override
    public void init(Config.Scope config) {
        PicketLinkCoreSTS sts = PicketLinkCoreSTS.instance();
        sts.installDefaultConfiguration();
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "saml";
    }
}

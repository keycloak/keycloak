package org.keycloak.adapters.saml.profile.webbrowsersso;

import org.keycloak.adapters.saml.OnSessionCreated;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.profile.SamlInvocationContext;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.saml.common.constants.GeneralConstants;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BrowserHandler extends WebBrowserSsoAuthenticationHandler {
    public BrowserHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        super(facade, deployment, sessionStore);
    }

    @Override
    public AuthOutcome handle(OnSessionCreated onCreateSession) {
        return doHandle(new SamlInvocationContext(null, null, null), onCreateSession);
    }
}

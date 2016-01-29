package org.keycloak.adapters.saml;

import org.keycloak.adapters.saml.profile.SamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.webbrowsersso.BrowserHandler;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaSamlAuthenticator extends SamlAuthenticator {
    public CatalinaSamlAuthenticator(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        super(facade, deployment, sessionStore);
    }

    @Override
    protected void completeAuthentication(SamlSession account) {
        // complete
    }

    @Override
    protected SamlAuthenticationHandler createBrowserHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        return new BrowserHandler(facade, deployment, sessionStore);
    }

}

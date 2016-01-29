package org.keycloak.adapters.saml;

import org.jboss.logging.Logger;
import org.keycloak.adapters.saml.profile.SamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.ecp.EcpAuthenticationHandler;
import org.keycloak.adapters.saml.profile.webbrowsersso.WebBrowserSsoAuthenticationHandler;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class SamlAuthenticator {

    protected static Logger log = Logger.getLogger(SamlAuthenticator.class);

    private final SamlAuthenticationHandler handler;

    public SamlAuthenticator(final HttpFacade facade, final SamlDeployment deployment, final SamlSessionStore sessionStore) {
        this.handler = createAuthenticationHandler(facade, deployment, sessionStore);
    }

    public AuthChallenge getChallenge() {
        return this.handler.getChallenge();
    }

    public AuthOutcome authenticate() {
        log.debugf("SamlAuthenticator is using handler [%s]", this.handler);
        return this.handler.handle(new OnSessionCreated() {
            @Override
            public void onSessionCreated(SamlSession samlSession) {
                completeAuthentication(samlSession);
            }
        });
    }

    protected abstract void completeAuthentication(SamlSession samlSession);

    protected SamlAuthenticationHandler createAuthenticationHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        if (EcpAuthenticationHandler.canHandle(facade)) {
            return EcpAuthenticationHandler.create(facade, deployment, sessionStore);
        }

        // defaults to the web browser sso profile
        return createBrowserHandler(facade, deployment, sessionStore);
    }

    protected SamlAuthenticationHandler createBrowserHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        return WebBrowserSsoAuthenticationHandler.create(facade, deployment, sessionStore);
    }
}
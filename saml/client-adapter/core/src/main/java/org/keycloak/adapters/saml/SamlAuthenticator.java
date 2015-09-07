package org.keycloak.adapters.saml;

import org.jboss.logging.Logger;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.HttpFacade;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlAuthenticator {
    protected static Logger log = Logger.getLogger(SamlAuthenticator.class);

    protected HttpFacade facade;
    protected AuthChallenge challenge;

    public AuthChallenge getChallenge() {
        return challenge;
    }

    public AuthOutcome authenticate() {
        return null;
    }
}

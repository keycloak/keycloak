package org.keycloak.adapters.saml.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;

import java.security.Principal;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowSamlAuthenticator extends SamlAuthenticator {
    protected SecurityContext securityContext;

    public UndertowSamlAuthenticator(SecurityContext securityContext, HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        super(facade, deployment, sessionStore);
        this.securityContext = securityContext;
    }

    @Override
    protected void completeAuthentication(final SamlSession samlSession) {
        Account undertowAccount = new Account() {
            @Override
            public Principal getPrincipal() {
                return samlSession.getPrincipal();
            }

            @Override
            public Set<String> getRoles() {
                return samlSession.getRoles();
            }
        };
        securityContext.authenticationComplete(undertowAccount, "KEYCLOAK-SAML", false);

    }
}

package org.keycloak.adapters.jaas;

import org.jboss.logging.Logger;
import org.keycloak.VerificationException;

/**
 * Login module, which allows to authenticate Keycloak access token in environments, which rely on JAAS
 * <p/>
 * It expects login based on username and password where username must be equal to "Bearer" and password is keycloak access token.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BearerTokenLoginModule extends AbstractKeycloakLoginModule {

    private static final Logger log = Logger.getLogger(BearerTokenLoginModule.class);

    @Override
    protected Auth doAuth(String username, String password) throws VerificationException {
        // Should do some checking of authenticated username if it's equivalent to passed value?
        return bearerAuth(password);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}

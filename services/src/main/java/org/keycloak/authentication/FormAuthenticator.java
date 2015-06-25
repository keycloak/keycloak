package org.keycloak.authentication;

import org.keycloak.provider.Provider;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormAuthenticator extends Provider {
    void authenticate(AuthenticatorContext context);
    Response createChallenge(FormContext context, String... errorMessages);
}

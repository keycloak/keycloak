package org.keycloak.authentication;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

/**
 * This interface is for users that want to add custom client authenticators to an authentication flow.
 * You must implement this interface as well as a ClientAuthenticatorFactory.
 *
 * This interface is for verifying client credentials from request. On the adapter side, you must also implement org.keycloak.adapters.authentication.ClientCredentialsProvider , which is supposed
 * to add the client credentials to the request, which will ClientAuthenticator verify on server side
 *
 * @see org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator
 * @see org.keycloak.authentication.authenticators.client.JWTClientAuthenticator
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientAuthenticator extends Provider {

    /**
     * Initial call for the authenticator.  This method should check the current HTTP request to determine if the request
     * satisfies the ClientAuthenticator's requirements.  If it doesn't, it should send back a challenge response by calling
     * the ClientAuthenticationFlowContext.challenge(Response).
     *
     * @param context
     */
    void authenticateClient(ClientAuthenticationFlowContext context);

}

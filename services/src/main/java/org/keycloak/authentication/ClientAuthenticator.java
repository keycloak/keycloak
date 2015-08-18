package org.keycloak.authentication;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientAuthenticator extends Provider {

    /**
     * TODO: javadoc
     *
     * @param context
     */
    void authenticateClient(ClientAuthenticationFlowContext context);


    /**
     * Does this authenticator require that the client has already been identified?  That ClientAuthenticationFlowContext.getClient() is not null?
     *
     * @return
     */
    boolean requiresClient();

    /**
     * Is this authenticator configured for this client?
     *
     * @param session
     * @param realm
     * @param client
     * @return
     */
    boolean configuredFor(KeycloakSession session, RealmModel realm, ClientModel client);
}

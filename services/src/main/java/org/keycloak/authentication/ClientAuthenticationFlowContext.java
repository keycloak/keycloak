package org.keycloak.authentication;

import java.util.Map;

import org.keycloak.models.ClientModel;

/**
 * Encapsulates information about the execution in ClientAuthenticationFlow
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientAuthenticationFlowContext extends AbstractAuthenticationFlowContext {

    /**
     * Current client attached to this flow.  It can return null if no client has been identified yet
     *
     * @return
     */
    ClientModel getClient();

    /**
     * Attach a specific client to this flow.
     *
     * @param client
     */
    void setClient(ClientModel client);

    /**
     * Return the map where the authenticators can put some additional state related to authenticated client and the context how was
     * client authenticated (ie. attributes from client certificate etc). Map is writable, so you can add/remove items from it as needed.
     *
     * After successful authentication will be those state data put into UserSession notes. This allows you to configure
     * UserSessionNote protocol mapper for your client, which will allow to map those state data into the access token available in the application
     *
     * @return
     */
    Map<String, String> getClientAuthAttributes();

}

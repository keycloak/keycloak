package org.keycloak.authentication;

import org.keycloak.models.ClientModel;

/**
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

}

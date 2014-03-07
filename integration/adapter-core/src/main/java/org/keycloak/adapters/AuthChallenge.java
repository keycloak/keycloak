package org.keycloak.adapters;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AuthChallenge {
    /**
     *
     * @param exchange
     * @return challenge sent
     */
    boolean challenge(HttpFacade exchange);
}

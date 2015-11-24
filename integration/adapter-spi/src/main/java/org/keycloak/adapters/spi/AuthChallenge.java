package org.keycloak.adapters.spi;

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

    /**
     * Some platforms need the error code that will be sent (i.e. Undertow)
     *
     * @return
     */
    int getResponseCode();
}

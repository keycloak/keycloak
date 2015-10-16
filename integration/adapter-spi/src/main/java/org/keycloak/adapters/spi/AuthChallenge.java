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
     * Whether or not an error page should be displayed if possible along with the challenge
     *
     * @return
     */
    boolean errorPage();

    /**
     * If errorPage is true, this is the response code the challenge will send.  This is used by platforms
     * that call HttpServletResponse.sendError() to forward to error page.
     *
     * @return
     */
    int getResponseCode();
}

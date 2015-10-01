package org.keycloak.enums;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum RelativeUrlsUsed {

    /**
     * Always use relative URI and resolve them later based on browser HTTP request
     */
    ALL_REQUESTS,

    /**
     * Use relative Uris just for browser requests and resolve those based on browser HTTP requests.
     * Backend request (like refresh token request, codeToToken request etc) will use the URI based on current hostname
     */
    BROWSER_ONLY,

    /**
     * Relative Uri not used. Configuration contains absolute URI
     */
    NEVER;

    public boolean useRelative(boolean isBrowserReq) {
        switch (this) {
            case ALL_REQUESTS:
                return true;
            case NEVER:
                return false;
            case BROWSER_ONLY:
                return isBrowserReq;
            default:
                return true;
        }
    }
}

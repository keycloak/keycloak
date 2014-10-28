package org.keycloak.adapters;

/**
 * Abstraction for storing token info on adapter side. Intended to be per-request object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AdapterTokenStore {

    /**
     * Impl can validate if current token exists and perform refreshing if it exists and is expired
     */
    void checkCurrentToken();

    /**
     * Check if we are logged already (we have already valid and successfully refreshed accessToken). Establish security context if yes
     *
     * @param authenticator used for actual request authentication
     * @return true if we are logged-in already
     */
    boolean isCached(RequestAuthenticator authenticator);

    /**
     * Finish successful OAuth2 login and store validated account
     *
     * @param account
     */
    void saveAccountInfo(KeycloakAccount account);

    /**
     * Handle logout on store side and possibly propagate logout call to Keycloak
     */
    void logout();

    /**
     * Callback invoked after successful token refresh
     *
     * @param securityContext context where refresh was performed
     */
    void refreshCallback(RefreshableKeycloakSecurityContext securityContext);
}

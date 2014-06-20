package org.keycloak.models.cache;

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface CacheKeycloakSession extends KeycloakSession {
    KeycloakSession getDelegate();

    void registerRealmInvalidation(String id);

    void registerApplicationInvalidation(String id);

    void registerRoleInvalidation(String id);

    void registerOAuthClientInvalidation(String id);

    void registerUserInvalidation(String id);
}

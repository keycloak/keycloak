package org.keycloak.services.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface KeycloakSession {
    KeycloakTransaction getTransaction();

    void close();
}

package org.keycloak.services.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface KeycloakSessionFactory {
    KeycloakSession createSession();
    void close();
}

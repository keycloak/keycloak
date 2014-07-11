package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface KeycloakTransaction {
    void begin();
    void commit();
    void rollback();
    void setRollbackOnly();
    boolean getRollbackOnly();
    boolean isActive();
}

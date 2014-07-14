package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface KeycloakTransactionManager extends KeycloakTransaction {

    void enlist(KeycloakTransaction transaction);
    void enlistAfterCompletion(KeycloakTransaction transaction);

}

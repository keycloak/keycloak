package org.keycloak.provider;

import java.io.Serializable;

/**
 * Operational info about given Provider. 
 * Contains info about Provider that can be shown on "Server Info" page or accessed over Operational monitoring endpoint.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see MonitorableProviderFactory
 */
public interface ProviderOperationalInfo extends Serializable {

    /**
     * Return true if provider is OK from operation point of view. It means it is able to perform necessary work.
     * It can return false for example if remote DB of JPA provider is not available, or LDAP server of LDAP based user federation provider is not available.
     * 
     * @return true if provider is OK to perform his operation.
     */
    boolean isOk();

}

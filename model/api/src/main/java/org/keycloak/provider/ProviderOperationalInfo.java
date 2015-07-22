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

}

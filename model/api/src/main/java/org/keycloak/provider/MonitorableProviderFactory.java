package org.keycloak.provider;

/**
 * Provider factory for provider which is monitorable. It means some info about it can be shown on "Server Info" page or accessed over Operational monitoring endpoint.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface MonitorableProviderFactory<T extends Provider> extends ProviderFactory<T> {

    /**
     * Get operational info about given provider. This info contains informations about providers configuration and operational conditions (eg. errors in connection to remote systems etc).
     * Is used to be shown on "Server Info" page or in Operational monitoring endpoint.
     * 
     * @return extendion of {@link ProviderOperationalInfo}
     */
    public ProviderOperationalInfo getOperationalInfo();

}

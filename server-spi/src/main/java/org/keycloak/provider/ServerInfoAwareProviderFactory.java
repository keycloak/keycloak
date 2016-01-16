package org.keycloak.provider;

import java.util.Map;

/**
 * Marker interface for {@link ProviderFactory} of Provider which wants to show some info on "Server Info" page in Admin console.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface ServerInfoAwareProviderFactory {

    /**
     * Return actual info about the provider. This info contains informations about providers configuration and operational conditions (eg. errors in connection to remote systems etc) which is
     * shown on "Server Info" page then.
     * 
     * @return Map with keys describing value and relevant values itself
     */
    public Map<String, String> getOperationalInfo();

}

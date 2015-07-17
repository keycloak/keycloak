package org.keycloak.wellknown;

import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface WellKnownProvider extends Provider {

    Object getConfig();

}

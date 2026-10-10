package org.keycloak.theme.freemarker;

import org.keycloak.provider.ProviderFactory;

public interface FreeMarkerProviderFactory extends ProviderFactory<FreeMarkerProvider> {

    /**
     * Clears any cached compiled templates. Implementations that do not cache templates can leave this as a no-op.
     */
    default void clearCache() {
    }

}

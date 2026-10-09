package org.keycloak.json;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Factory for obtaining the {@link KeycloakJsonMapper} instance. The implementation is
 * discovered once via {@link ServiceLoader} and cached statically.
 */
public final class KeycloakJsonMapperFactory {

    private KeycloakJsonMapperFactory() {
    }

    public static KeycloakJsonMapper mapper() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final KeycloakJsonMapper INSTANCE = load();

        private static KeycloakJsonMapper load() {
            KeycloakJsonMapper best = null;
            Iterator<KeycloakJsonMapper> providers = ServiceLoader.load(KeycloakJsonMapper.class).iterator();
            while (providers.hasNext()) {
                try {
                    KeycloakJsonMapper candidate = providers.next();
                    if (best == null || candidate.getPriority() > best.getPriority()) {
                        best = candidate;
                    }
                } catch (ServiceConfigurationError e) {
                    // provider's Jackson dependency not on classpath, try next
                }
            }
            if (best == null) {
                throw new IllegalStateException("No KeycloakJsonMapper implementation found via ServiceLoader");
            }
            return best;
        }
    }
}

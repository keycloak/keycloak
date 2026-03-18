package org.keycloak.services.util;

import java.util.Comparator;
import java.util.Optional;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

/**
 * Utility class for resolving {@link WellKnownProviderFactory} instances based on their alias.
 * This class provides methods to interact with the available provider factories within the context
 * of a {@link KeycloakSessionFactory}.
 *
 * This is a utility class and is not intended to be instantiated.
 */
public class WellKnownProviderUtil {

    private WellKnownProviderUtil() {
        // Utility class
    }

    /**
     * Resolves a {@link WellKnownProviderFactory} from the specified alias.
     * If multiple factories share the same alias, the one with the lowest priority is selected.
     *
     * @param sessionFactory the {@link KeycloakSessionFactory} to retrieve provider factories from
     * @param alias the alias of the desired {@link WellKnownProviderFactory}; if null, an empty {@link Optional} is returned
     * @return an {@link Optional} containing the resolved {@link WellKnownProviderFactory}, or empty if no matching factory is found
     */
    public static Optional<WellKnownProviderFactory> resolveFromAlias(KeycloakSessionFactory sessionFactory, String alias) {

        if (alias == null) {
            return Optional.empty();
        }

        return sessionFactory.getProviderFactoriesStream(WellKnownProvider.class)
                .map(providerFactory -> (WellKnownProviderFactory) providerFactory)
                .filter(wellKnownProviderFactory -> alias.equals(wellKnownProviderFactory.getAlias()))
                .min(Comparator.comparingInt(WellKnownProviderFactory::getPriority));
    }
}

package org.keycloak.representations.admin.v2.validators;

import java.util.List;

/**
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public final class PersistedFieldResolvers {

    private static final List<PersistedFieldResolver> RESOLVERS = List.of(new ClientPersistedFieldResolver());

    private PersistedFieldResolvers() {
    }

    public static PersistedFieldResolver forType(Class<?> representationType) {
        return RESOLVERS.stream()
                .filter(resolver -> resolver.supports(representationType))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No PersistedFieldResolver defined for " + representationType));
    }
}

package org.keycloak.broker.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

/**
 * Utility for resolving identity provider mapper execution order.
 *
 * <p>When {@link IdentityProviderModel#MAPPER_ORDER_ENABLED} is enabled for the IdP, mappers are sorted by the per-mapper
 * {@link IdentityProviderMapperModel#MAPPER_ORDER} config value (ascending). Mappers without a valid order are executed
 * after ordered ones, preserving their legacy relative order.</p>
 *
 * <p>When disabled, the legacy mapper order is used.</p>
 */
public final class IdentityProviderMapperExecutionOrder {

    private IdentityProviderMapperExecutionOrder() {
    }

    public static Stream<IdentityProviderMapperModel> getMappersStream(KeycloakSession session, IdentityProviderModel idpConfig) {
        String alias = idpConfig.getAlias();

        if (!idpConfig.isMapperOrderEnabled()) {
            return session.identityProviders().getMappersByAliasStream(alias);
        }

        // Ensure any underlying JPA stream is closed eagerly (collect into a list).
        List<IdentityProviderMapperModel> mappers;
        try (Stream<IdentityProviderMapperModel> stream = session.identityProviders().getMappersByAliasStream(alias)) {
            mappers = stream.collect(Collectors.toCollection(ArrayList::new));
        }

        // Preserve legacy relative order for ties by sorting on original index.
        List<Integer> indices = IntStream.range(0, mappers.size()).boxed().collect(Collectors.toList());
        indices.sort((i1, i2) -> {
            int o1 = parseOrder(mappers.get(i1));
            int o2 = parseOrder(mappers.get(i2));
            if (o1 != o2) {
                return Integer.compare(o1, o2);
            }
            return Integer.compare(i1, i2);
        });

        return indices.stream().map(mappers::get);
    }

    private static int parseOrder(IdentityProviderMapperModel mapper) {
        if (mapper.getConfig() == null) {
            return Integer.MAX_VALUE;
        }
        String raw = mapper.getConfig().get(IdentityProviderMapperModel.MAPPER_ORDER);
        if (raw == null || raw.isBlank()) {
            return Integer.MAX_VALUE;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value < 0 ? Integer.MAX_VALUE : value;
        } catch (NumberFormatException nfe) {
            return Integer.MAX_VALUE;
        }
    }
}


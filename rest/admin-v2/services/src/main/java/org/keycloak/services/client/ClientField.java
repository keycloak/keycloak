package org.keycloak.services.client;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.admin.api.SortField;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;

/**
 * Sortable fields for Client Admin API v2 list queries ({@code sort}).
 * API names map to scalar {@code CLIENT} table columns.
 */
public enum ClientField implements SortField {
    CLIENT_ID("clientId", stringKey(BaseClientRepresentation::getClientId)),
    DISPLAY_NAME("displayName", stringKey(BaseClientRepresentation::getDisplayName)),
    DESCRIPTION("description", stringKey(BaseClientRepresentation::getDescription)),
    PROTOCOL("protocol", stringKey(BaseClientRepresentation::getProtocol)),
    ENABLED("enabled", booleanKey(BaseClientRepresentation::getEnabled)),
    APP_URL("appUrl", stringKey(BaseClientRepresentation::getAppUrl)),
    CREATED_TIMESTAMP("createdTimestamp", longKey(BaseClientRepresentation::getCreatedTimestamp)),
    UPDATED_TIMESTAMP("updatedTimestamp", longKey(BaseClientRepresentation::getUpdatedTimestamp));

    private final String apiName;
    private final ComparatorFactory comparatorFactory;

    ClientField(String apiName, ComparatorFactory comparatorFactory) {
        this.apiName = apiName;
        this.comparatorFactory = comparatorFactory;
    }

    @Override
    public String getApiName() {
        return apiName;
    }

    @Override
    public String toQueryValue() {
        return apiName;
    }

    public Comparator<BaseClientRepresentation> comparator(boolean ascending) {
        return comparatorFactory.comparator(ascending);
    }

    public static ClientField defaultField() {
        return CLIENT_ID;
    }

    public static Optional<ClientField> fromApiName(String apiName) {
        return Stream.of(values()).filter(field -> field.apiName.equals(apiName)).findFirst();
    }

    public static String allowedApiNames() {
        return Stream.of(values()).map(ClientField::getApiName).collect(Collectors.joining(", "));
    }

    private static ComparatorFactory longKey(Function<BaseClientRepresentation, Long> getter) {
        return ascending -> Comparator.comparing(getter, Comparator.nullsLast(
                ascending ? Long::compareTo : Comparator.<Long>reverseOrder()));
    }

    private static ComparatorFactory stringKey(Function<BaseClientRepresentation, String> getter) {
        return ascending -> Comparator.comparing(getter, Comparator.nullsLast(
                ascending ? String.CASE_INSENSITIVE_ORDER : String.CASE_INSENSITIVE_ORDER.reversed()));
    }

    private static ComparatorFactory booleanKey(Function<BaseClientRepresentation, Boolean> getter) {
        return ascending -> Comparator.comparing(getter, Comparator.nullsLast(
                ascending ? Boolean::compareTo : Comparator.<Boolean>reverseOrder()));
    }

    @FunctionalInterface
    private interface ComparatorFactory {
        Comparator<BaseClientRepresentation> comparator(boolean ascending);
    }
}

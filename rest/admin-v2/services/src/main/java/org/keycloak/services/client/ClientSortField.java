package org.keycloak.services.client;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sortable fields for Client Admin API v2 list queries ({@code sort}).
 * API names map to scalar {@code CLIENT} table columns.
 * 
 * TODO: should be Attribute metadata
 */
public enum ClientSortField {
    CLIENT_ID("clientId"),
    DISPLAY_NAME("displayName"),
    DESCRIPTION("description"),
    PROTOCOL("protocol"),
    ENABLED("enabled"),
    APP_URL("appUrl"),
    CREATED_TIMESTAMP("createdTimestamp"),
    UPDATED_TIMESTAMP("updatedTimestamp");

    private static final Map<String, ClientSortField> API_NAME_TO_CLIENT_FIELD = EnumSet.allOf(ClientSortField.class).stream()
            .collect(Collectors.toMap(f -> f.apiName, Function.identity()));

    private final String apiName;

    ClientSortField(String apiName) {
        this.apiName = apiName;
    }

    public String getApiName() {
        return apiName;
    }

    public static ClientSortField defaultField() {
        return CLIENT_ID;
    }

    public static Optional<ClientSortField> fromApiName(String apiName) {
        return Optional.ofNullable(API_NAME_TO_CLIENT_FIELD.get(apiName));
    }

    public static String allowedApiNames() {
        return Stream.of(values()).map(ClientSortField::getApiName).collect(Collectors.joining(", "));
    }

}

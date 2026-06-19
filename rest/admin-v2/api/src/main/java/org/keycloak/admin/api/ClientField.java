package org.keycloak.admin.api;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Sortable fields for Client Admin API v2 list queries ({@code sort}).
 * API names map to scalar {@code CLIENT} table columns.
 */
@Schema(enumeration = {"clientId", "displayName", "description", "protocol", "enabled", "appUrl"})
public enum ClientField {
    CLIENT_ID("clientId", stringKey(BaseClientRepresentation::getClientId)),
    DISPLAY_NAME("displayName", stringKey(BaseClientRepresentation::getDisplayName)),
    DESCRIPTION("description", stringKey(BaseClientRepresentation::getDescription)),
    PROTOCOL("protocol", stringKey(BaseClientRepresentation::getProtocol)),
    ENABLED("enabled", Comparator.comparing(BaseClientRepresentation::getEnabled, Comparator.nullsLast(Boolean::compareTo))),
    APP_URL("appUrl", stringKey(BaseClientRepresentation::getAppUrl));

    private final String apiName;
    private final Comparator<BaseClientRepresentation> comparator;

    ClientField(String apiName, Comparator<BaseClientRepresentation> comparator) {
        this.apiName = apiName;
        this.comparator = comparator;
    }

    public String getApiName() {
        return apiName;
    }

    public String toQueryValue() {
        return apiName;
    }

    public Comparator<BaseClientRepresentation> comparator(boolean ascending) {
        return ascending ? comparator : comparator.reversed();
    }

    public static ClientField defaultField() {
        return CLIENT_ID;
    }

    public static Optional<ClientField> fromApiName(String apiName) {
        return Stream.of(values()).filter(field -> field.apiName.equals(apiName)).findFirst();
    }

    private static Comparator<BaseClientRepresentation> stringKey(Function<BaseClientRepresentation, String> getter) {
        return Comparator.comparing(getter, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}

package org.keycloak.services.client.query;

import java.util.Map;
import java.util.function.Function;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

public class FieldResolver {

    private static final Map<String, Function<BaseClientRepresentation, Object>> FIELDS = Map.ofEntries(
            Map.entry("clientId", BaseClientRepresentation::getClientId),
            Map.entry("displayName", BaseClientRepresentation::getDisplayName),
            Map.entry("description", BaseClientRepresentation::getDescription),
            Map.entry("enabled", BaseClientRepresentation::getEnabled),
            Map.entry("protocol", BaseClientRepresentation::getProtocol),
            Map.entry("appUrl", BaseClientRepresentation::getAppUrl),
            Map.entry("redirectUris", BaseClientRepresentation::getRedirectUris),
            Map.entry("roles", BaseClientRepresentation::getRoles),
            Map.entry("loginFlows", client -> client instanceof OIDCClientRepresentation oidc ? oidc.getLoginFlows() : null),
            Map.entry("auth.method", client -> {
                if (client instanceof OIDCClientRepresentation oidc && oidc.getAuth() != null) {
                    return oidc.getAuth().getMethod();
                }
                return null;
            }),
            Map.entry("webOrigins", client -> client instanceof OIDCClientRepresentation oidc ? oidc.getWebOrigins() : null),
            Map.entry("serviceAccountRoles", client -> client instanceof OIDCClientRepresentation oidc ? oidc.getServiceAccountRoles() : null)
    );

    public static Object resolve(String fieldPath, BaseClientRepresentation client) {
        Function<BaseClientRepresentation, Object> accessor = FIELDS.get(fieldPath);
        if (accessor == null) {
            throw new ClientQueryException("Unknown query field: " + fieldPath);
        }
        return accessor.apply(client);
    }
}

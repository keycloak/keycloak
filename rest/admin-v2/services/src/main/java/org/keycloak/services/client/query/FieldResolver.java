package org.keycloak.services.client.query;

import java.util.Map;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.services.client.scim.BaseClientModelSchema;
import org.keycloak.services.client.scim.OIDCClientModelSchema;
import org.keycloak.services.client.scim.SAMLClientModelSchema;

public class FieldResolver {

    private static final Map<String, BaseClientModelSchema<?>> SCHEMAS = Map.of(
            OIDCClientRepresentation.PROTOCOL, OIDCClientModelSchema.INSTANCE,
            SAMLClientRepresentation.PROTOCOL, SAMLClientModelSchema.INSTANCE);

    public static boolean isKnownField(String fieldPath) {
        return SCHEMAS.values().stream().anyMatch(schema -> schema.getAttributeByPath(fieldPath) != null);
    }

    @SuppressWarnings("unchecked")
    public static Object resolve(String fieldPath, BaseClientRepresentation client) {
        String protocol = client.getProtocol();
        BaseClientModelSchema schema = protocol != null ? SCHEMAS.get(protocol) : null;
        if (schema != null) {
            return schema.getRepresentationValue(client, fieldPath);
        }
        return null;
    }
}

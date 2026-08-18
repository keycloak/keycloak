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
        if ("auth.method".equals(fieldPath)) {
            return true;
        }
        return SCHEMAS.values().stream().anyMatch(schema -> schema.getAttributes().containsKey(fieldPath));
    }

    @SuppressWarnings("unchecked")
    public static Object resolve(String fieldPath, BaseClientRepresentation client) {
        if ("auth.method".equals(fieldPath)) {
            if (client instanceof OIDCClientRepresentation oidc && oidc.getAuth() != null) {
                return oidc.getAuth().getMethod();
            }
            return null;
        }
        String protocol = client.getProtocol();
        BaseClientModelSchema schema = protocol != null ? SCHEMAS.get(protocol) : null;
        if (schema != null) {
            return schema.getRepresentationValue(client, fieldPath);
        }
        return null;
    }
}

package org.keycloak.services.client.query;

import org.keycloak.models.mapper.ClientModelMappers;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

public class FieldResolver {

    private static final ClientModelMappers MAPPERS = new ClientModelMappers();

    public static boolean isKnownField(String fieldPath) {
        if ("auth.method".equals(fieldPath)) {
            return true;
        }
        return MAPPERS.isKnownField(fieldPath);
    }

    public static Object resolve(String fieldPath, BaseClientRepresentation client) {
        if ("auth.method".equals(fieldPath)) {
            if (client instanceof OIDCClientRepresentation oidc && oidc.getAuth() != null) {
                return oidc.getAuth().getMethod();
            }
            return null;
        }
        return MAPPERS.resolveFieldValue(fieldPath, client);
    }
}

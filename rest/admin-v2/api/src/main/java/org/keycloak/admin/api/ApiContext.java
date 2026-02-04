package org.keycloak.admin.api;

import jakarta.annotation.Nonnull;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.util.ObjectMapperResolver;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * API context that can be shared across sub-resources to manage authz, validation, eventing, mapping
 */
public record ApiContext(@Nonnull KeycloakSession session,
                         @Nonnull JakartaValidatorProvider validator,
                         @Nonnull AuthContext auth
                         // provide eventing capabilities, etc.
) {
    public record AuthContext(@Nonnull AdminAuth info,
                              @Nonnull AdminPermissionEvaluator permissions,
                              @Nonnull TokenManager tokenManager) {

    }

    private static final ObjectMapper MAPPER = new ObjectMapperResolver().getContext(null);

    public ObjectMapper mapper() {
        return MAPPER;
    }
}

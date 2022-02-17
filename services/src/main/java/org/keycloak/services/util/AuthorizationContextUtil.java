package org.keycloak.services.util;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.rar.AuthorizationRequestParserProvider;
import org.keycloak.protocol.oidc.rar.parsers.ClientScopeAuthorizationRequestParserProviderFactory;
import org.keycloak.rar.AuthorizationRequestContext;

public class AuthorizationContextUtil {

    public static AuthorizationRequestContext getAuthorizationRequestContextFromScopes(KeycloakSession session, String scope) {
        if (!Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            throw new RuntimeException("The Dynamic Scopes feature is not enabled and the AuthorizationRequestContext hasn't been generated");
        }
        AuthorizationRequestParserProvider clientScopeParser = session.getProvider(AuthorizationRequestParserProvider.class,
                ClientScopeAuthorizationRequestParserProviderFactory.CLIENT_SCOPE_PARSER_ID);

        if (clientScopeParser == null) {
            throw new RuntimeException(String.format("No provider found for authorization requests parser %1s",
                    ClientScopeAuthorizationRequestParserProviderFactory.CLIENT_SCOPE_PARSER_ID));
        }

        return clientScopeParser.parseScopes(scope);
    }

}

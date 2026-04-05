package org.keycloak.representations;

import org.keycloak.TokenCategory;

public class AuthorizationResponseToken extends JsonWebToken{

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.AUTHORIZATION_RESPONSE;
    }
}

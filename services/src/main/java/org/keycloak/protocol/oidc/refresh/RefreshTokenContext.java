package org.keycloak.protocol.oidc.refresh;

import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.grants.OAuth2GrantType;
import org.keycloak.protocol.oidc.grants.RefreshTokenGrantType;
import org.keycloak.representations.RefreshToken;

public record RefreshTokenContext(RefreshToken oldRefreshToken, OAuth2GrantType.Context grantContext, RefreshTokenGrantType grant, TokenManager tokenManager,
                                  String scopeParameter, String resourceParameter) {
}

package org.keycloak.protocol.oidc.token;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;

public record TokenPostProcessorContext(OAuth2Code code, RefreshToken requestRefreshToken, RefreshToken refreshToken, AccessToken accessToken, ClientSessionContext clientSessionCtx) {
}

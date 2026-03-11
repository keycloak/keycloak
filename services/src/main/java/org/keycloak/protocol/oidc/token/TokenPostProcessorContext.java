package org.keycloak.protocol.oidc.token;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;

public record TokenPostProcessorContext(RefreshToken refreshToken, AccessToken accessToken, ClientSessionContext clientSessionCtx) {
}

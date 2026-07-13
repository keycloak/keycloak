package org.keycloak.protocol.oidc.refresh;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;

public record InitialRefreshTokenContext(ClientSessionContext clientSessionCtx, TokenManager.AccessTokenResponseBuilder responseBuilder,
                                         EventBuilder event, boolean offlineTokenRequested, AccessToken.Confirmation confirmation) {
}

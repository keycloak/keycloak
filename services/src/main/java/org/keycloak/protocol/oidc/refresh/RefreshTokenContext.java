package org.keycloak.protocol.oidc.refresh;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.RefreshToken;

public record RefreshTokenContext(RefreshToken oldRefreshToken, TokenManager tokenManager, ClientConnection connection, RealmModel realm,
                                  ClientModel authorizedClient, EventBuilder event, HttpHeaders headers,
                                  String scopeParameter, String resourceParameter) {
}

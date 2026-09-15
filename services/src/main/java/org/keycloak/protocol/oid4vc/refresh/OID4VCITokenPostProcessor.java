package org.keycloak.protocol.oid4vc.refresh;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;

public class OID4VCITokenPostProcessor implements TokenPostProcessor {

    private final KeycloakSession session;

    public OID4VCITokenPostProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void process(TokenPostProcessorContext context) {
        AccessToken accessToken = context.accessToken();
        RefreshToken refreshToken = context.refreshToken();

        if (refreshToken == null || !OID4VCIRefreshTokenProviderFactory.PROVIDER_ID.equals(refreshToken.getProvider())) {
            return;
        }

        if (shouldUseTransientSession(accessToken)) {
            // No reference to the sessionId should be within refresh-token or access-token.
            refreshToken.setSessionId(null);
            accessToken.setSessionId(null);
        }

        // Limit the audience to only the credential endpoint URL.
        String credentialEndpoint = OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
        accessToken.audience(credentialEndpoint);
    }


    // This might be possibly updated to always return true? As sessionId is not needed on refresh-token nor access-token even on the initial issuance (during authorization_code grant)
    private boolean shouldUseTransientSession(AccessToken accessToken) {
        TokenContextEncoderProvider encoder = session.getProvider(TokenContextEncoderProvider.class);
        return (encoder.getTokenContextFromTokenId(accessToken.getId()).getSessionType() == AccessTokenContext.SessionType.TRANSIENT);
    }
}

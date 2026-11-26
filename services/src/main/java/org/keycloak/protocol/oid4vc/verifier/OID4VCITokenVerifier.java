package org.keycloak.protocol.oid4vc.verifier;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.utils.CredentialScopeUtils;
import org.keycloak.protocol.oidc.verifier.TokenVerifierProvider;
import org.keycloak.representations.AccessToken;

import org.jboss.logging.Logger;

/**
 * Validates that tokens are only used on endpoints allowed by their grant type.
 * This ensures Pre-Authorized Code tokens are restricted to the credential endpoint,
 * and other grant types only access their intended endpoints.
 */
public class OID4VCITokenVerifier implements TokenVerifier.Predicate<AccessToken> {
    private static final Logger logger = Logger.getLogger(OID4VCITokenVerifier.class);

    private final TokenVerifierProvider.TokenVerifierProviderContext context;

    OID4VCITokenVerifier(TokenVerifierProvider.TokenVerifierProviderContext context) {
        this.context = context;
    }


    @Override
    public boolean test(AccessToken token) throws VerificationException {
        KeycloakSession session = context.session();
        RealmModel realm = context.realm();

        // Check if there are any OID4VCI client scopes inside (if it is OID4VCI request)
        ClientModel client = getClient(session, realm, token);
        if (client == null || !client.isEnabled()) {
            return false;
        }

        List<CredentialScopeModel> credentialScopes = CredentialScopeUtils.getCredentialScopesForScopeParameter(client, token.getScope());
        boolean isOID4VCIAccessToken = !credentialScopes.isEmpty();

        // Check if the request path ends with the credential endpoint path
        URI requestUri = context.uriInfo().getRequestUri();
        String credentialsEndpointUrl = OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
        boolean isCredentialEndpointRequest = URI.create(credentialsEndpointUrl).getPath().equals(requestUri.getPath());

        if (isOID4VCIAccessToken) {
            if (isCredentialEndpointRequest) {
                // Check if token has exactly one audience and it matches the credential endpoint
                // Being strict about audience prevents potential security issues with multi-audience tokens
                String expectedAudience = OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
                List<String> tokenAudiences = token.getAudience() == null ? Collections.emptyList() : List.of(token.getAudience());
                boolean allowed = tokenAudiences.size() == 1 && expectedAudience.equals(tokenAudiences.get(0));
                if (!allowed) {
                    logger.tracef("Not allowed to use OID4VCI access token. Allowed audience: '%s'. Token audiences: '%s'", expectedAudience, tokenAudiences);
                }
                return allowed;
            } else {
                // OID4VCI access tokens useful just at credential endpoint
                logger.tracef("Not allowed to use OID4VCI access token at endpoint: %s", requestUri);
                return false;
            }
        } else {
            if (isCredentialEndpointRequest) {
                // Regular access tokens cannot be used at credential endpoint
                logger.tracef("Not allowed to use regular access token without OID4VCI scopes at endpoint: %s", requestUri);
                return false;
            } else {
                return true;
            }
        }
    }

    private ClientModel getClient(KeycloakSession session, RealmModel realm, AccessToken token) {
        String clientId = token.getIssuedFor();
        ClientModel client = session.getContext().getClient();
        if (client != null && client.getClientId().equals(clientId)) {
            return client;
        } else {
            return session.clients().getClientByClientId(realm, clientId);
        }
    }
}

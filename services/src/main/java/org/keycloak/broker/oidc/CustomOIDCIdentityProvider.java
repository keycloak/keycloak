package org.keycloak.broker.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URLEncoder;

public class CustomOIDCIdentityProvider extends OIDCIdentityProvider {
    protected static final Logger logger = Logger.getLogger(CustomOIDCIdentityProvider.class);
    public CustomOIDCIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);
        config.setDefaultScope(config.getDefaultScope().replace("openid", "").trim());
    }

    @Override
    protected String getDefaultScopes() {
        return "";
    }

    @Override
    protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            logger.info("tokenResponse: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tokenResponse));
        } catch (IOException e) {
            logger.error("Failed to convert tokenResponse to JSON string", e);
            logger.info("tokenResponse: " + tokenResponse.toString());
        }

        String encodedIdToken = tokenResponse.getIdToken();
        JsonNode userInfo = parseToken(encodedIdToken);
        logger.info("userInfo: " + userInfo);

        String id = getJsonProperty(userInfo, "sub");
        BrokeredIdentityContext identity = new BrokeredIdentityContext(id, getConfig());    
        String name = getJsonProperty(userInfo, "name");
        String givenName = getJsonProperty(userInfo, IDToken.GIVEN_NAME);
        String familyName = getJsonProperty(userInfo, IDToken.FAMILY_NAME);
        String preferredUsername = getUsernameFromUserInfo(userInfo);
        String email = getJsonProperty(userInfo, "email");
        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(identity, userInfo, getConfig().getAlias());

        identity.getContextData().put(VALIDATED_ID_TOKEN, idToken);
        identity.setId(id);

        if (givenName != null) {
            identity.setFirstName(givenName);
        }

        if (familyName != null) {
            identity.setLastName(familyName);
        }

        if (givenName == null && familyName == null) {
            identity.setName(name);
        }

        identity.setEmail(email);

        identity.setBrokerUserId(getConfig().getAlias() + "." + id);

        if (preferredUsername == null) {
            preferredUsername = email;
        }

        if (preferredUsername == null) {
            preferredUsername = id;
        }

        identity.setUsername(preferredUsername);
        if (tokenResponse != null && tokenResponse.getSessionState() != null) {
            identity.setBrokerSessionId(getConfig().getAlias() + "." + tokenResponse.getSessionState());
        }
        if (tokenResponse != null) identity.getContextData().put(FEDERATED_ACCESS_TOKEN_RESPONSE, tokenResponse);
        if (tokenResponse != null) processAccessTokenResponse(identity, tokenResponse);

        return identity;
    }

    @Override
    public SimpleHttp authenticateTokenRequest(final SimpleHttp tokenRequest) {
        SimpleHttp authenticatedRequest = super.authenticateTokenRequest(tokenRequest);
        String claimsJson = "{\"id_token\":{\"urn:telekom.com:all\":null}}";
        authenticatedRequest.param("claims", URLEncoder.encode(claimsJson));
        return authenticatedRequest;
    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        final UriBuilder uriBuilder = super.createAuthorizationUrl(request);
        String claimsJson = "{\"id_token\":{\"urn:telekom.com:all\":null}}";
        uriBuilder.queryParam("claims", URLEncoder.encode(claimsJson));
        return uriBuilder;
    }

    private JsonNode parseToken(String encodedIdToken) {
      try {
        String[] parts = encodedIdToken.split("\\.");
        if (parts.length != 3) {
          throw new IdentityBrokerException("Invalid JWT token format");
        }
        
        String payload = parts[1];
        byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
        String decodedPayload = new String(decodedBytes);
        
        return JsonSerialization.readValue(decodedPayload, JsonNode.class);
      } catch (Exception e) {
        throw new IdentityBrokerException("Could not parse JWT token", e);
      }
    }
}

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

    private static final String DEFAULT_CLAIMS_JSON = "{\"id_token\":{\"urn:telekom.com:all\":{\"essential\":true}}}";
    private static final String CLAIMS_ENV_VAR = "KEYCLOAK_OIDC_CLAIMS_JSON";
    private static final boolean useOpenId =  "true".equals(System.getenv("USE_OPENID"));
    
    public CustomOIDCIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);
        if (!useOpenId) {
            config.setDefaultScope(config.getDefaultScope().replace("openid", "").trim());
            logger.info("Default scopes: " + config.getDefaultScope());
        }
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

        super.session.getContext().getAuthenticationSession().setUserSessionNote(FEDERATED_ACCESS_TOKEN, tokenResponse.getToken());
        super.session.getContext().getAuthenticationSession().setUserSessionNote("FEDERATED_CLIENT_ID", getConfig().getClientId());
        super.session.getContext().getAuthenticationSession().setUserSessionNote("FEDERATED_SECRET", getConfig().getClientSecret());
        super.session.getContext().getAuthenticationSession().setUserSessionNote("FEDERATED_TOKEN_URL", getConfig().getTokenUrl());

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
        String claimsJson = getClaimsJson();
        authenticatedRequest.param("claims", URLEncoder.encode(claimsJson));
        try {
            logger.info("Request URL: " + authenticatedRequest.getUrl());
            logger.info("Request headers: " + authenticatedRequest.getHeaders());
            logger.info("Request params: " + authenticatedRequest.getParams());
        } catch (Exception e) {
            logger.error("Failed to convert tokenRequest to JSON string", e);
        }
        return authenticatedRequest;
    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        final UriBuilder uriBuilder = super.createAuthorizationUrl(request);
        String claimsJson = getClaimsJson();
        uriBuilder.queryParam("claims", URLEncoder.encode(claimsJson));
        return uriBuilder;
    }

    private String getClaimsJson() {
        String envValue = System.getenv(CLAIMS_ENV_VAR);
        return (envValue != null && !envValue.trim().isEmpty()) ? envValue : DEFAULT_CLAIMS_JSON;
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

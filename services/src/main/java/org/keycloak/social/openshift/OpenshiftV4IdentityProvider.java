package org.keycloak.social.openshift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * Identity provider for Openshift V4.
 *
 * @author David Festal and Sebastian Łaskawiec
 */
public class OpenshiftV4IdentityProvider extends AbstractOAuth2IdentityProvider<OpenshiftV4IdentityProviderConfig> implements SocialIdentityProvider<OpenshiftV4IdentityProviderConfig> {

    public static final String BASE_URL = "https://api.preview.openshift.com";
    public static final String OPENSHIFT_OAUTH_METADATA_ENDPOINT = "/.well-known/oauth-authorization-server";
    public static final String PROFILE_RESOURCE = "/apis/user.openshift.io/v1/users/~";
    public static final String DEFAULT_SCOPE = "user:info";

    public OpenshiftV4IdentityProvider(KeycloakSession session, OpenshiftV4IdentityProviderConfig config) {
        super(session, config);
        final String baseUrl = Optional.ofNullable(config.getBaseUrl()).orElse(BASE_URL);
        Map<String, Object> oauthDescriptor = getAuthJson(session, config.getBaseUrl());
        logger.debugv("Openshift v4 OAuth descriptor: {0}", oauthDescriptor);
        config.setAuthorizationUrl((String) oauthDescriptor.get("authorization_endpoint"));
        config.setTokenUrl((String) oauthDescriptor.get("token_endpoint"));
        config.setUserInfoUrl(baseUrl + PROFILE_RESOURCE);
    }

    Map<String, Object> getAuthJson(KeycloakSession session, String baseUrl) {
        try {
            InputStream response = getOauthMetadataInputStream(session, baseUrl);
            Map<String, Object> map = mapMetadata(response);
            return map;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not initialize oAuth metadata", e);
        }
    }

    InputStream getOauthMetadataInputStream(KeycloakSession session, String baseUrl) throws IOException {
        HttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        HttpGet getRequest = new HttpGet(baseUrl + OPENSHIFT_OAUTH_METADATA_ENDPOINT);
        getRequest.addHeader("accept", "application/json");

        HttpResponse response = httpClient.execute(getRequest);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
        }
        return response.getEntity().getContent();
    }

    Map mapMetadata(InputStream response) throws IOException {
        return new ObjectMapper().readValue(response, Map.class);
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        try {
            final JsonNode profile = fetchProfile(accessToken);
            final BrokeredIdentityContext user = extractUserContext(profile);
            AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
            return user;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from Openshift.", e);
        }
    }

    private BrokeredIdentityContext extractUserContext(JsonNode profile) {
        JsonNode metadata = profile.get("metadata");
        logger.debugv("extractUserContext: metadata = {0}", metadata);
        final BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(metadata, "uid"));
        user.setUsername(getJsonProperty(metadata, "name"));
        user.setName(getJsonProperty(profile, "fullName"));
        user.setIdpConfig(getConfig());
        user.setIdp(this);
        return user;
    }

    private JsonNode fetchProfile(String accessToken) throws IOException {
        return SimpleHttp.doGet(getConfig().getUserInfoUrl(), this.session)
                .header("Authorization", "Bearer " + accessToken)
                .asJson();
    }

    @Override
    protected boolean supportsExternalExchange() {
        return true;
    }

    @Override
    protected String getProfileEndpointForValidation(EventBuilder event) {
        return getConfig().getUserInfoUrl();
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
        final BrokeredIdentityContext user = extractUserContext(profile);
        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
        return user;
    }

}

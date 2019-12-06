package org.keycloak.social.openshift;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Optional;

/**
 * Identity provider for Openshift V3. Check <a href="https://docs.openshift.com/enterprise/3.0/architecture/additional_concepts/authentication.html">official documentation</a> for more details.
 */
public class OpenshiftV3IdentityProvider extends AbstractOAuth2IdentityProvider<OpenshiftV3IdentityProviderConfig> implements SocialIdentityProvider<OpenshiftV3IdentityProviderConfig> {

    public static final String BASE_URL = "https://api.preview.openshift.com";
    private static final String AUTH_RESOURCE = "/oauth/authorize";
    private static final String TOKEN_RESOURCE = "/oauth/token";
    private static final String PROFILE_RESOURCE = "/oapi/v1/users/~";
    private static final String DEFAULT_SCOPE = "user:info";

    public OpenshiftV3IdentityProvider(KeycloakSession session, OpenshiftV3IdentityProviderConfig config) {
        super(session, config);
        final String baseUrl = Optional.ofNullable(config.getBaseUrl()).orElse(BASE_URL);
        config.setAuthorizationUrl(baseUrl + AUTH_RESOURCE);
        config.setTokenUrl(baseUrl + TOKEN_RESOURCE);
        config.setUserInfoUrl(baseUrl + PROFILE_RESOURCE);
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

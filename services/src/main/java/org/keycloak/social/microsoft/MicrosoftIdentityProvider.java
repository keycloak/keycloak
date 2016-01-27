package org.keycloak.social.microsoft;

import java.net.URLEncoder;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.oidc.util.JsonSimpleHttp;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.logging.KeycloakLogger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * Identity provider for Microsoft account. Uses OAuth 2 protocol of Windows Live Services as documented at <a href="https://msdn.microsoft.com/en-us/library/hh243647.aspx">https://msdn.microsoft.com/en-us/library/hh243647.aspx</a>
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class MicrosoftIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

    private static final KeycloakLogger logger = Logger.getMessageLogger(KeycloakLogger.class, MicrosoftIdentityProvider.class.getName());

    public static final String AUTH_URL = "https://login.live.com/oauth20_authorize.srf";
    public static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    public static final String PROFILE_URL = "https://apis.live.net/v5.0/me";
    public static final String DEFAULT_SCOPE = "wl.basic,wl.emails";

    public MicrosoftIdentityProvider(OAuth2IdentityProviderConfig config) {
        super(config);
        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setUserInfoUrl(PROFILE_URL);
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        try {
            String URL = PROFILE_URL + "?access_token=" + URLEncoder.encode(accessToken, "UTF-8");
            if (logger.isDebugEnabled()) {
                logger.debug("Microsoft Live user profile request to: " + URL);
            }
            JsonNode profile = JsonSimpleHttp.asJson(SimpleHttp.doGet(URL));

            String id = getJsonProperty(profile, "id");

            String email = null;
            if (profile.has("emails")) {
                email = getJsonProperty(profile.get("emails"), "preferred");
            }

            BrokeredIdentityContext user = new BrokeredIdentityContext(id);

            user.setUsername(email != null ? email : id);
            user.setFirstName(getJsonProperty(profile, "first_name"));
            user.setLastName(getJsonProperty(profile, "last_name"));
            if (email != null)
                user.setEmail(email);
            user.setIdpConfig(getConfig());
            user.setIdp(this);

            AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

            return user;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from Microsoft Live ID.", e);
        }
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }
}

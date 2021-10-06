package org.keycloak.social.orcid;

import java.io.IOException;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.events.EventBuilder;

public class OrcidIdentityProvider extends AbstractOAuth2IdentityProvider<OrcidIdentityProviderConfig> implements SocialIdentityProvider<OrcidIdentityProviderConfig> {

    public static final String AUTH_URL = "/authorize";
    public static final String TOKEN_URL = "/token";
    public static final String DEFAULT_SCOPE = "/authenticate";
    public static final String RECORD = "/record";

    public OrcidIdentityProvider(KeycloakSession session, OrcidIdentityProviderConfig config) {
        super(session, config);
        config.setAuthorizationUrl(config.getBaseUrl() + AUTH_URL);
        config.setTokenUrl(config.getBaseUrl() + TOKEN_URL);
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
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode node) {
        JsonNode orcidIdentifier = node.get("orcid-identifier");
        JsonNode person = node.get("person");

        String id = getJsonProperty(orcidIdentifier, "path");

        BrokeredIdentityContext user = new BrokeredIdentityContext(id);
        user.setUsername(id);
        JsonNode name = person.get("name");
        if (name!= null && ! name.isNull()) {
            String firstName = getJsonProperty(name.get("given-names"), "value");
            String lastName = getJsonProperty(name.get("family-name"), "value");
            user.setFirstName(firstName);
            user.setLastName(lastName);
        }
        String uri = getJsonProperty(orcidIdentifier, "uri");
        user.setUserAttribute("orcid",uri);

        JsonNode emails = person.get("emails").get("email");
        String email = null;
        for (JsonNode emailAttr : emails) {
            if (! emailAttr.get("primary").isNull() && emailAttr.get("primary").booleanValue()) {
                email =getJsonProperty(emailAttr, "email");
                break;
            }
        }
        user.setEmail(email);

        user.setIdpConfig(getConfig());
        user.setIdp(this);

        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, node, getConfig().getAlias());

        return user;
    }

    @Override
    public BrokeredIdentityContext getFederatedIdentity(String response) {
        String []  info = extractInfoFromResponse(response, getAccessTokenResponseParameter());
        String accessToken = info[0];
        String orcid = info[1];

        try {
            JsonNode profile = SimpleHttp.doGet(getConfig().getUserInfoUrl()+"/"+orcid+RECORD, session).header("Authorization", "Bearer " + accessToken).asJson();
            BrokeredIdentityContext context =  extractIdentityFromProfile(null, profile);
            context.getContextData().put(FEDERATED_ACCESS_TOKEN, accessToken);
            return context;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from orcid.", e);
        }

    }

    private String [] extractInfoFromResponse(String response, String tokenName){
        String accessToken =null;
        String orcid = null;
        try {
            JsonNode node = mapper.readTree(response);
            if(node.has(tokenName)){
                String s = node.get(tokenName).textValue();
                if(s != null && !s.trim().isEmpty())
                    accessToken = s;
            }
            if(node.has("orcid")){
                String s = node.get("orcid").textValue();
                if(s != null && !s.trim().isEmpty())
                    orcid = s;
            }
            if (accessToken == null) {
                throw new IdentityBrokerException("No access token available in OAuth server response: " + response);
            }
            if (orcid == null) {
                throw new IdentityBrokerException("No orcid id available in ORCID server response: " + response);
            }
        } catch (IOException e) {
            throw new IdentityBrokerException("Could not extract token [" + tokenName + "] from response [" + response + "] due: " + e.getMessage(), e);
        }
        return Stream.of(accessToken,orcid).toArray(String[]::new);
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }
}

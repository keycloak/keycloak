package org.keycloak.protocol.oidc.mappers;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_LABEL;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_RESPONSE;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_RESPONSE_HELP_TEXT;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_RESPONSE_LABEL;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME;

/**
 * {@link UserAttributeMapper} implementation that enables to map the value of a user attribute to the "scope" fields of
 * {@link AccessToken}s and {@link AccessTokenResponse}s. In the fields, the attribute value will be preceded by the
 * configured token claim in the form "${CLAIM_NAME}:${ATTRIBUTE_VALUE}". In case no token claim was configured, the
 * value will not be added to the fields. Also note that the mapper does not check if mappings result in duplicate or
 * ambiguous "scope" entries such as "[...] claimName:attributeValue [...] claimName:attributeValue [...]" or
 * "[...] claimName:attributeValue1 [...] claimName:attributeValue2 [...]". In addition, currently only single-valued
 * user attributes are supported.
 */
public class UserAttributeToScopeMapper extends UserAttributeMapper implements OIDCAccessTokenResponseMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        configProperties.add(new ProviderConfigProperty() {{
            setName(ProtocolMapperUtils.USER_ATTRIBUTE);
            setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
            setHelpText(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_HELP_TEXT);
            setType(ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE);
        }});

        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);

        OIDCAttributeMapperHelper.addJsonTypeConfig(configProperties);

        configProperties.add(new ProviderConfigProperty() {{
            setName(INCLUDE_IN_ACCESS_TOKEN);
            setLabel(INCLUDE_IN_ACCESS_TOKEN_LABEL);
            setType(ProviderConfigProperty.BOOLEAN_TYPE);
            setDefaultValue(Boolean.TRUE.toString());
            setHelpText(INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT);
        }});

        configProperties.add(new ProviderConfigProperty() {{
            setName(INCLUDE_IN_ACCESS_TOKEN_RESPONSE);
            setLabel(INCLUDE_IN_ACCESS_TOKEN_RESPONSE_LABEL);
            setType(ProviderConfigProperty.BOOLEAN_TYPE);
            setDefaultValue(Boolean.TRUE.toString());
            setHelpText(INCLUDE_IN_ACCESS_TOKEN_RESPONSE_HELP_TEXT);
        }});
    }

    private static final Logger logger = Logger.getLogger(UserAttributeToScopeMapper.class);

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    public static final String PROVIDER_ID = "user-attribute-to-scope-mapper";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Attribute To Scope";
    }

    @Override
    public String getHelpText() {
        return "Map a custom user attribute to a token scope. This mapper does not check if mappings result in " +
                "duplicate or ambiguous scope entries.";
    }

    /**
     * Perform the actual mapping for {@link AccessToken}s.
     */
    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        if (!(token instanceof AccessToken accessToken))
            return;

        var claim = buildClaim(mappingModel, userSession);
        if (claim == null)
            return;

        var claimString = claim.getKey() + ":" + claim.getValue();

        var scope = accessToken.getScope();

        if (scope == null || scope.isBlank()) {
            accessToken.setScope(claimString);
            return;
        }


        var existingScopes = new HashSet<>(List.of(scope.trim().split("\\s+")));
        if (existingScopes.contains(claimString)) {
            logger.debugf("Skipping duplicate scope entry: %s", claimString);
            return;
        }

        //append safely (handle trailing space)
        String newScope = scope.endsWith(" ") ? scope + claimString : scope + " " + claimString;
        accessToken.setScope(newScope);

    }

    /**
     * Build the token claim as a {@link Map.Entry} with "${CLAIM_NAME}" on the left and "${ATTRIBUTE_VALUE}" on the right
     * side.
     */
    private Map.Entry<String,Object>  buildClaim(ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        var claimName = mappingModel.getConfig().get(TOKEN_CLAIM_NAME);
        if (claimName == null || claimName.isBlank())
            return null;

        var user = userSession.getUser();
        var attributeName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE);
        var aggregateAttrs = Boolean.parseBoolean(mappingModel.getConfig().get(ProtocolMapperUtils.AGGREGATE_ATTRS));
        var attributeValue = KeycloakModelUtils.resolveAttribute(user, attributeName, aggregateAttrs);
        if (attributeValue == null)
            return null;

        // Check attribute value for existence and that it is single-valued
        var objectAttributeValue = OIDCAttributeMapperHelper.mapAttributeValue(mappingModel, attributeValue);
        if (objectAttributeValue == null)
            return null;
        else if (objectAttributeValue instanceof Collection<?>) {
            logger.errorv("Value of user attribute \"{0}\" is multi-valued which is currently not supported in " +
                    "user attribute value to token scope mapping. Value will not be added to token scope.", attributeName);
            return null;
        }

        // Check for supported attribute value type. As per OIDCAttributeMapperHelper.convertToType(), boolean, integer,
        // string, long, and JsonNode value types are supported. However, we currently don't support JsonNode values
        // because their string representation could potentially violate OAuth's well-formed constraints towards "scope"
        // values.
        var unsupportedValueType = !(objectAttributeValue instanceof Boolean)
                && !(objectAttributeValue instanceof Integer)
                && !(objectAttributeValue instanceof Long)
                && !(objectAttributeValue instanceof String);
        if (unsupportedValueType) {
            logger.errorv("Value of user attribute \"{0}\" is not of primitive or String type but of unsupported " +
                            "type \"{1}\". Value will not be added to token scope.", attributeName,
                    objectAttributeValue.getClass().getName());
            return null;
        }
        return Map.entry(claimName, objectAttributeValue);
    }

    /**
     * Perform the actual mapping for {@link AccessTokenResponse}s.
     */
    @Override
    protected void setClaim(AccessTokenResponse accessTokenResponse,
                            ProtocolMapperModel mappingModel,
                            UserSessionModel userSession,
                            KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx
    ) {
        var claim = buildClaim(mappingModel, userSession);
        if (claim == null)
            return;

        accessTokenResponse.setOtherClaims(claim.getKey(), claim.getValue());
    }
}

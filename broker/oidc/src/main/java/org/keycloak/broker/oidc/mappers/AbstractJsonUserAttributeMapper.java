package org.keycloak.broker.oidc.mappers;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Abstract class for Social Provider mappers which allow mapping of JSON user profile field into Keycloak user attribute.
 * Concrete mapper classes with own ID and provider mapping must be implemented for each social provider who uses {@link JsonNode} user profile.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class AbstractJsonUserAttributeMapper extends AbstractIdentityProviderMapper {

    protected static final Logger logger = Logger.getLogger(AbstractJsonUserAttributeMapper.class);

    protected static final Logger LOGGER_DUMP_USER_PROFILE = Logger.getLogger("org.keycloak.social.user_profile_dump");

    /**
     * Config param where name of mapping source JSON User Profile field is stored.
     */
    public static final String CONF_JSON_FIELD = "jsonField";
    /**
     * Config param where name of mapping target USer attribute is stored.
     */
    public static final String CONF_USER_ATTRIBUTE = "userAttribute";

    /**
     * Key in {@link BrokeredIdentityContext#getContextData()} where {@link JsonNode} with user profile is stored.
     */
    public static final String CONTEXT_JSON_NODE = OIDCIdentityProvider.USER_INFO;

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        property1 = new ProviderConfigProperty();
        property1.setName(CONF_JSON_FIELD);
        property1.setLabel("Social Profile JSON Field Name");
        property1.setHelpText("Name of field in Social provider User Profile JSON data to get value from.");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);
        property = new ProviderConfigProperty();
        property.setName(CONF_USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store information into.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static void storeUserProfileForMapper(BrokeredIdentityContext user, JsonNode profile) {
        user.getContextData().put(AbstractJsonUserAttributeMapper.CONTEXT_JSON_NODE, profile);
        if (LOGGER_DUMP_USER_PROFILE.isDebugEnabled())
            LOGGER_DUMP_USER_PROFILE.debug("User Profile JSON Data: " + profile);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Attribute Importer";
    }

    @Override
    public String getHelpText() {
        return "Import user profile information if it exists in Social provider JSON data into the specified user attribute.";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(CONF_USER_ATTRIBUTE);
        if (attribute == null) {
            logger.debug("Attribute is not configured");
            return;
        }

        String value = getJsonValue(mapperModel, context);
        if (value != null) {
            user.setAttribute(attribute, value);
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // we do not update user profile from social provider
    }

    protected static String getJsonValue(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        String jsonField = mapperModel.getConfig().get(CONF_JSON_FIELD);
        if (jsonField == null) {
            logger.debug("JSON field is not configured");
            return null;
        }

        JsonNode profileJsonNode = (JsonNode) context.getContextData().get(CONTEXT_JSON_NODE);

        if (profileJsonNode != null) {
            JsonNode value = profileJsonNode.get(jsonField);
            if (value != null) {
                String ret = value.asText();
                if (ret != null && !ret.trim().isEmpty())
                    return ret.trim();
                else
                    return null;
            }
        } else {
            logger.debug("User profile JSON node is not available.");
        }

        return null;
    }

}

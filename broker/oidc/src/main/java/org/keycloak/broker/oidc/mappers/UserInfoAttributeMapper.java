package org.keycloak.broker.oidc.mappers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.provider.ProviderConfigProperty;

public class UserInfoAttributeMapper extends UserAttributeMapper
{
    public static final String   GOOGLE_OIDC_IDENTITY_PROVIDER = "google";
    public static final String[] COMPATIBLE_PROVIDERS          = {GOOGLE_OIDC_IDENTITY_PROVIDER};

    public static final List<String> URL_BASED_VALUES = new ArrayList<>();

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String USER_ATTRIBUTE = "user.attribute";

    static
    {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        property1 = new ProviderConfigProperty();
        property1.setName(CLAIM);
        property1.setLabel("Claim");
        property1.setHelpText("Name of claim to search for in token.  You can reference nested claims using a '.', i.e. 'address.locality'.");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store claim.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        URL_BASED_VALUES.add("picture");
    }

    public static final String PROVIDER_ID = "oidc-user-info-attribute-idp-mapper";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "User Info Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "User Info Attribute Importer";
    }

    @Override
    public String getHelpText() {
        return "Import declared claim if it exists in ID token,  access token, or user info into the specified user attribute.";
    }


    @Override
    protected Object getClaimValue(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        Object claimValue = super.getClaimValue(mapperModel, context);
        if (null != claimValue) return removeQueryStringParamsIfUrl(claimValue.toString());

        ObjectNode userAttributes = (ObjectNode)context.getContextData().get(KeycloakOIDCIdentityProvider.USER_INFO);
        JsonNode attributeValue = userAttributes.get(mapperModel.getConfig().get(CLAIM));
        if (null != attributeValue) {
            if (URL_BASED_VALUES.contains(mapperModel.getConfig().get(CLAIM)))
                return removeQueryStringParamsIfUrl(attributeValue.getTextValue());
            else
                return attributeValue.getTextValue();
        }

        return null;
    }

    private String removeQueryStringParamsIfUrl(String value)
    {
        try {
            new URL(value); // see if this is a valid url
            String[] urlParts = value.split("\\?");
            return urlParts[0]; // remove query string params

        } catch (Exception e) {
            return value;
        }
    }

}

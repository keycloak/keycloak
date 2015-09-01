package org.keycloak.broker.provider;

import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HardcodedAttributeMapper extends AbstractIdentityProviderMapper {
    public static final String ATTRIBUTE = "attribute";
    public static final String ATTRIBUTE_VALUE = "attribute.value";
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE);
        property.setLabel("User Attribute");
        property.setHelpText("Name of user attribute you want to hardcode");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_VALUE);
        property.setLabel("User Attribute Value");
        property.setHelpText("Value you want to hardcode");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
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
        return "Hardcoded Attribute";
    }

    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};


    public static final String PROVIDER_ID = "hardcoded-attribute-idp-mapper";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(ATTRIBUTE);
        String attributeValue = mapperModel.getConfig().get(ATTRIBUTE_VALUE);
        user.setSingleAttribute(attribute, attributeValue);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(ATTRIBUTE);
        String attributeValue = mapperModel.getConfig().get(ATTRIBUTE_VALUE);
        user.setSingleAttribute(attribute, attributeValue);
    }

    @Override
    public String getHelpText() {
        return "When user is imported from provider, hardcode a value to a specific user attribute.";
    }
}

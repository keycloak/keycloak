package org.keycloak.protocol.saml.mappers;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;

public class NameIdMapperHelper {

    public static final String NAMEID_MAPPER_CATEGORY = "NameID Mapper";

    // The Class implemented NameIDMapper needs the following attributes.
    public static final String MAPPER_NAMEID_FORMAT = "mapper.nameid.format";
    public static final String MAPPER_NAMEID_FORMAT_LABEL = "name-id-format";
    public static final String MAPPER_NAMEID_FORMAT_HELP_TEXT = "mapper.nameid.format.tooltip";

    public static void setConfigProperties(List<ProviderConfigProperty> configProperties) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(NameIdMapperHelper.MAPPER_NAMEID_FORMAT);
        property.setLabel(NameIdMapperHelper.MAPPER_NAMEID_FORMAT_LABEL);
        property.setHelpText(NameIdMapperHelper.MAPPER_NAMEID_FORMAT_HELP_TEXT);
        List<String> types = new ArrayList<String>();
        types.add(JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get());
        types.add(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());
        types.add(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get());
        types.add(JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get());
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(types);
        configProperties.add(property);
    }
}

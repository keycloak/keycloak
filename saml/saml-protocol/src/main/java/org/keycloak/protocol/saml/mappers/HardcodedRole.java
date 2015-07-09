package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.protocol.saml.SamlProtocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mappings UserModel property (the property name of a getter method) to an AttributeStatement.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HardcodedRole extends AbstractSAMLProtocolMapper {
    public static final String PROVIDER_ID = "saml-hardcode-role-mapper";
    public static final String ATTRIBUTE_VALUE = "attribute.value";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName("role");
        property.setLabel("Role");
        property.setHelpText("Arbitrary role name you want to hardcode.  This role does not have to exist in current realm and can be just any string you need");
        property.setType(ProviderConfigProperty.ROLE_TYPE);
        configProperties.add(property);
    }



    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded role";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode role into SAML Assertion.";
    }

    public static ProtocolMapperModel create(String name,
                                             String role) {
        String mapperId = PROVIDER_ID;
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
       Map<String, String> config = new HashMap<String, String>();
        config.put("role", role);
        mapper.setConfig(config);
        return mapper;

    }

}

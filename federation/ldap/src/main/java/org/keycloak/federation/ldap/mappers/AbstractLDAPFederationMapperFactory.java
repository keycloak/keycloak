package org.keycloak.federation.ldap.mappers;

import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.mappers.MapperConfigValidationException;
import org.keycloak.mappers.UserFederationMapperFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractLDAPFederationMapperFactory implements UserFederationMapperFactory {

    // Used to map attributes from LDAP to UserModel attributes
    public static final String ATTRIBUTE_MAPPER_CATEGORY = "Attribute Mapper";

    // Used to map roles from LDAP to UserModel users
    public static final String ROLE_MAPPER_CATEGORY = "Role Mapper";

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public String getFederationProviderType() {
        return LDAPFederationProviderFactory.PROVIDER_NAME;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        throw new IllegalStateException("Method not supported for this implementation");
    }

    @Override
    public void close() {
    }

    public static ProviderConfigProperty createConfigProperty(String name, String label, String helpText, String type, Object defaultValue) {
        ProviderConfigProperty configProperty = new ProviderConfigProperty();
        configProperty.setName(name);
        configProperty.setLabel(label);
        configProperty.setHelpText(helpText);
        configProperty.setType(type);
        configProperty.setDefaultValue(defaultValue);
        return configProperty;
    }

    protected void checkMandatoryConfigAttribute(String name, String displayName, UserFederationMapperModel mapperModel) throws MapperConfigValidationException {
        String attrConfigValue = mapperModel.getConfig().get(name);
        if (attrConfigValue == null || attrConfigValue.trim().isEmpty()) {
            throw new MapperConfigValidationException("Missing configuration for '" + displayName + "'");
        }
    }


}

package org.keycloak.federation.ldap.mappers;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.mappers.MapperConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FullNameLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID =  "full-name-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty userModelAttribute = createConfigProperty(FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, "LDAP Full Name Attribute",
                "Name of LDAP attribute, which contains fullName of user. In most cases it will be 'cn' ", ProviderConfigProperty.STRING_TYPE, LDAPConstants.CN);
        configProperties.add(userModelAttribute);

        ProviderConfigProperty readOnly = createConfigProperty(UserAttributeLDAPFederationMapper.READ_ONLY, "Read Only",
                "For Read-only is data imported from LDAP to Keycloak DB, but it's not saved back to LDAP when user is updated in Keycloak.", ProviderConfigProperty.BOOLEAN_TYPE, "false");
        configProperties.add(readOnly);
    }

    @Override
    protected String getHelpText() {
        return "Used to map full-name of user from single attribute in LDAP (usually 'cn' attribute) to firstName and lastName attributes of UserModel in Keycloak DB";
    }

    @Override
    public String getDisplayCategory() {
        return ATTRIBUTE_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Full Name";
    }

    @Override
    protected List<ProviderConfigProperty> getBaseConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfig(UserFederationMapperModel mapperModel) throws MapperConfigValidationException {
        checkMandatoryConfigAttribute(FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, "LDAP Full Name Attribute", mapperModel);
    }

    @Override
    public UserFederationMapper create(KeycloakSession session) {
        return new FullNameLDAPFederationMapper(this);
    }
}

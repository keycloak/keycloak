package org.keycloak.federation.ldap.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.mappers.MapperConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FullNameLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID =  "full-name-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty userModelAttribute = createConfigProperty(FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, "LDAP Full Name Attribute",
                "Name of LDAP attribute, which contains fullName of user. In most cases it will be 'cn' ", ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(userModelAttribute);

        ProviderConfigProperty readOnly = createConfigProperty(UserAttributeLDAPFederationMapper.READ_ONLY, "Read Only",
                "For Read-only is data imported from LDAP to Keycloak DB, but it's not saved back to LDAP when user is updated in Keycloak.", ProviderConfigProperty.BOOLEAN_TYPE, null);
        configProperties.add(readOnly);
    }

    @Override
    public String getHelpText() {
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
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        Map<String, String> defaultValues = new HashMap<>();
        LDAPConfig config = new LDAPConfig(providerModel.getConfig());

        defaultValues.put(FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, LDAPConstants.CN);

        String readOnly = config.getEditMode() == UserFederationProvider.EditMode.WRITABLE ? "false" : "true";
        defaultValues.put(UserAttributeLDAPFederationMapper.READ_ONLY, readOnly);

        return defaultValues;
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
    protected AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm) {
        return new FullNameLDAPFederationMapper(mapperModel, federationProvider, realm);
    }
}

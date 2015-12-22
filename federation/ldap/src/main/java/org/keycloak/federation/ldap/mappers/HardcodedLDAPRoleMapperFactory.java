package org.keycloak.federation.ldap.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.mappers.MapperConfigValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HardcodedLDAPRoleMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID = "hardcoded-ldap-role-mapper";
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty roleAttr = createConfigProperty(HardcodedLDAPRoleMapper.ROLE, "Role",
                "Role to grant to user.  Click 'Select Role' button to browse roles, or just type it in the textbox.  To reference an application role the syntax is appname.approle, i.e. myapp.myrole",
                ProviderConfigProperty.ROLE_TYPE, null);
        configProperties.add(roleAttr);
    }

    @Override
    public String getHelpText() {
        return "When user is imported from LDAP, he will be automatically added into this configured role.";
    }

    @Override
    public String getDisplayCategory() {
        return ROLE_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Role";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        return new HashMap<>();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationMapperModel mapperModel) throws MapperConfigValidationException {
        String roleName = mapperModel.getConfig().get(HardcodedLDAPRoleMapper.ROLE);
        if (roleName == null) {
            throw new MapperConfigValidationException("Role can't be null");
        }
        RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);
        if (role == null) {
            throw new MapperConfigValidationException("There is no role corresponding to configured value");
        }
    }

    @Override
    protected AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm) {
        return new HardcodedLDAPRoleMapper(mapperModel, federationProvider, realm);
    }
}

package org.keycloak.federation.ldap.mappers.msad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.mappers.AbstractLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.AbstractLDAPFederationMapperFactory;
import org.keycloak.mappers.MapperConfigValidationException;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MSADUserAccountControlMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID = LDAPConstants.MSAD_USER_ACCOUNT_CONTROL_MAPPER;
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
    }

    @Override
    public String getHelpText() {
        return "Mapper specific to MSAD. It's able to integrate the MSAD user account state into Keycloak account state (account enabled, password is expired etc). It's using userAccountControl and pwdLastSet MSAD attributes for that. " +
                "For example if pwdLastSet is 0, the Keycloak user is required to update password, if userAccountControl is 514 (disabled account) the Keycloak user is disabled as well etc. Mapper is also able to handle exception code from LDAP user authentication.";
    }

    @Override
    public String getDisplayCategory() {
        return ATTRIBUTE_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "MSAD User Account Control";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        return Collections.emptyMap();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationMapperModel mapperModel) throws MapperConfigValidationException {
    }

    @Override
    protected AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm) {
        return new MSADUserAccountControlMapper(mapperModel, federationProvider, realm);
    }
}

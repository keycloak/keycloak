package org.keycloak.federation.ldap.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.keycloak.mappers.MapperConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID = "role-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty rolesDn = createConfigProperty(RoleLDAPFederationMapper.ROLES_DN, "LDAP Roles DN",
                "LDAP DN where are roles of this tree saved. For example 'ou=finance,dc=example,dc=org' ", ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(rolesDn);

        ProviderConfigProperty roleNameLDAPAttribute = createConfigProperty(RoleLDAPFederationMapper.ROLE_NAME_LDAP_ATTRIBUTE, "Role Name LDAP Attribute",
                "Name of LDAP attribute, which is used in role objects for name and RDN of role. Usually it will be 'cn' . In this case typical group/role object may have DN like 'cn=role1,ou=finance,dc=example,dc=org' ",
                ProviderConfigProperty.STRING_TYPE, LDAPConstants.CN);
        configProperties.add(roleNameLDAPAttribute);

        ProviderConfigProperty membershipLDAPAttribute = createConfigProperty(RoleLDAPFederationMapper.MEMBERSHIP_LDAP_ATTRIBUTE, "Membership LDAP Attribute",
                "Name of LDAP attribute on role, which is used for membership mappings. Usually it will be 'member' ",
                ProviderConfigProperty.STRING_TYPE, LDAPConstants.MEMBER);
        configProperties.add(membershipLDAPAttribute);

        ProviderConfigProperty roleObjectClasses = createConfigProperty(RoleLDAPFederationMapper.ROLE_OBJECT_CLASSES, "Role Object Classes",
                "Object classes of the role object divided by comma (if more values needed). In typical LDAP deployment it could be 'groupOfNames' or 'groupOfEntries' ",
                ProviderConfigProperty.STRING_TYPE, LDAPConstants.GROUP_OF_NAMES);
        configProperties.add(roleObjectClasses);

        List<String> modes = new LinkedList<String>();
        for (RoleLDAPFederationMapper.Mode mode : RoleLDAPFederationMapper.Mode.values()) {
            modes.add(mode.toString());
        }
        ProviderConfigProperty mode = createConfigProperty(RoleLDAPFederationMapper.MODE, "Mode",
                "LDAP_ONLY means that all role mappings are retrieved from LDAP and saved into LDAP. READ_ONLY is Read-only LDAP mode where role mappings are " +
                        "retrieved from both LDAP and DB and merged together. New role grants are not saved to LDAP but to DB. IMPORT is Read-only LDAP mode where role mappings are retrieved from LDAP just at the time when user is imported from LDAP and then " +
                        "they are saved to local keycloak DB.",
                ProviderConfigProperty.LIST_TYPE, modes);
        configProperties.add(mode);

        ProviderConfigProperty useRealmRolesMappings = createConfigProperty(RoleLDAPFederationMapper.USE_REALM_ROLES_MAPPING, "Use Realm Roles Mapping",
                "If true, then LDAP role mappings will be mapped to realm role mappings in Keycloak. Otherwise it will be mapped to client role mappings", ProviderConfigProperty.BOOLEAN_TYPE, "true");
        configProperties.add(useRealmRolesMappings);

        ProviderConfigProperty clientIdProperty = createConfigProperty(RoleLDAPFederationMapper.CLIENT_ID, "Client ID",
                "Client ID of client to which LDAP role mappings will be mapped. Applicable just if 'Use Realm Roles Mapping' is false",
                ProviderConfigProperty.CLIENT_LIST_TYPE, null);
        configProperties.add(clientIdProperty);
    }

    @Override
    public String getHelpText() {
        return "Used to map role mappings of roles from some LDAP DN to Keycloak role mappings of either realm roles or client roles of particular client";
    }

    @Override
    public String getDisplayCategory() {
        return ROLE_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Role mappings";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfig(UserFederationMapperModel mapperModel) throws MapperConfigValidationException {
        checkMandatoryConfigAttribute(RoleLDAPFederationMapper.ROLES_DN, "LDAP Roles DN", mapperModel);

        String realmMappings = mapperModel.getConfig().get(RoleLDAPFederationMapper.USE_REALM_ROLES_MAPPING);
        boolean useRealmMappings = Boolean.parseBoolean(realmMappings);
        if (!useRealmMappings) {
            String clientId = mapperModel.getConfig().get(RoleLDAPFederationMapper.CLIENT_ID);
            if (clientId == null || clientId.trim().isEmpty()) {
                throw new MapperConfigValidationException("Client ID needs to be provided in config when Realm Roles Mapping is not used");
            }
        }
    }

    @Override
    public UserFederationMapper create(KeycloakSession session) {
        return new RoleLDAPFederationMapper();
    }
}

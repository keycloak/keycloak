package org.keycloak.federation.ldap.mappers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.mappers.MapperConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    private static final Logger logger = Logger.getLogger(RoleLDAPFederationMapperFactory.class);

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
                "Object class (or classes) of the role object. It's divided by comma if more classes needed. In typical LDAP deployment it could be 'groupOfNames' . In Active Directory it's usually 'group' ",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(roleObjectClasses);
        
        ProviderConfigProperty ldapFilter = createConfigProperty(RoleLDAPFederationMapper.ROLES_LDAP_FILTER,
                "LDAP Filter",
                "LDAP Filter adds additional custom filter to the whole query.",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(ldapFilter);

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

    // Sync roles from LDAP to Keycloak DB during creation or update of mapperModel
    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof RealmModel.UserFederationMapperEvent) {
                    RealmModel.UserFederationMapperEvent mapperEvent = (RealmModel.UserFederationMapperEvent)event;
                    UserFederationMapperModel mapperModel = mapperEvent.getFederationMapper();
                    RealmModel realm = mapperEvent.getRealm();
                    KeycloakSession session = mapperEvent.getSession();

                    if (mapperModel.getFederationMapperType().equals(PROVIDER_ID)) {
                        try {
                            String federationProviderId = mapperModel.getFederationProviderId();
                            UserFederationProviderModel providerModel = KeycloakModelUtils.findUserFederationProviderById(federationProviderId, realm);
                            if (providerModel == null) {
                                throw new IllegalStateException("Can't find federation provider with ID [" + federationProviderId + "] in realm " + realm.getName());
                            }

                            UserFederationProviderFactory ldapFactory = (UserFederationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, providerModel.getProviderName());
                            LDAPFederationProvider ldapProvider = (LDAPFederationProvider) ldapFactory.getInstance(session, providerModel);

                            // Sync roles
                            new RoleLDAPFederationMapper().syncRolesFromLDAP(mapperModel, ldapProvider, realm);
                        } catch (Exception e) {
                            logger.warn("Exception during initial sync of roles from LDAP.", e);
                        }
                    }
                }
            }

        });
    }

    @Override
    public void validateConfig(UserFederationMapperModel mapperModel) throws MapperConfigValidationException {
        checkMandatoryConfigAttribute(RoleLDAPFederationMapper.ROLES_DN, "LDAP Roles DN", mapperModel);
        checkMandatoryConfigAttribute(RoleLDAPFederationMapper.MODE, "Mode", mapperModel);

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

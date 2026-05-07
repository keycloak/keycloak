package org.keycloak.storage.ldap.mappers;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.utils.KeycloakSessionUtil;

import java.util.ArrayList;
import java.util.List;

public class RequiredActionLDAPGroupStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = "required-action-ldap-group-mapper";
    public static final String REQUIRED_ACTION = "requiredAction";
    public static final String GROUP = "requiredGroup";
    public static final String GROUPS_DN = "groupsDn";
    public static final String MEMBERSHIP_ATTR_NAME = "membershipAttrName";


    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new RequiredActionLDAPGroupStorageMapper(mapperModel, federationProvider);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "When a user is imported from LDAP and is in the specified LDAP group, then the chosen required actions will be added to the user. This will only happen if the user did not already have or had the required actions assigned through this mapper. If the configured required actions changes, the new required actions will be added to all users regardless of prior assignments.";
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        if (config.getConfig().getFirst(GROUP) == null) {
            throw new ComponentValidationException("Group can't be null");
        }

        if (config.getConfig().getFirst(GROUPS_DN) == null) {
            throw new ComponentValidationException("Group DN can't be null");
        }

        if (config.getConfig().getList(REQUIRED_ACTION) == null || config.getConfig().getList(REQUIRED_ACTION).isEmpty()) {
            throw new ComponentValidationException("Required Action can't be null");
        }

        if (config.getConfig().getFirst(MEMBERSHIP_ATTR_NAME) == null) {
            throw new ComponentValidationException("Membership Attribute Name can't be null");
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        final List<ProviderConfigProperty> configProperties = new ArrayList<>();

        var availableRequiredActionIdList = KeycloakSessionUtil
                .getKeycloakSession()
                .getContext()
                .getRealm()
                .getRequiredActionProvidersStream()
                .map(RequiredActionProviderModel::getProviderId)
                .toList();

        configProperties.add(createConfigProperty(
                REQUIRED_ACTION,
                "Required Action",
                "Required Action to add to the user.",
                ProviderConfigProperty.MULTIVALUED_LIST_TYPE,
                availableRequiredActionIdList,
                true)
        );
        configProperties.add(createConfigProperty(
                GROUP,
                "Ldap group",
                "cn name of the group to check if the user is in. For example 'mygroup'.",
                ProviderConfigProperty.STRING_TYPE,
                null,
                true)
        );
        configProperties.add(createConfigProperty(
                GROUPS_DN,
                "Groups DN",
                "DN of the LDAP tree where groups are located. For example 'ou=Groups,dc=example,dc=com'",
                ProviderConfigProperty.STRING_TYPE,
                null,
                true)
        );
        configProperties.add(createConfigProperty(
                MEMBERSHIP_ATTR_NAME,
                "Membership Attribute Name",
                "Name of the attribute in the LDAP group that contains the members. Default is 'member'. Some LDAP schemas use 'uniqueMember' or 'memberUid' instead.",
                ProviderConfigProperty.STRING_TYPE,
                null,
                true)
        );

        return configProperties;
    }
}

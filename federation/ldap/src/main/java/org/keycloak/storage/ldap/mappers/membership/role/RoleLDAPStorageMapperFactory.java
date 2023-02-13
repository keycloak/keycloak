/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.storage.ldap.mappers.membership.role;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.UserRolesRetrieveStrategy;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleLDAPStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = "role-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties;
    protected static final Map<String, UserRolesRetrieveStrategy> userRolesStrategies = new LinkedHashMap<>();
    protected static final List<String> MEMBERSHIP_TYPES = new LinkedList<>();
    protected static final List<String> MODES = new LinkedList<>();
    protected static final List<String> NO_IMPORT_MODES = new LinkedList<>();

    static {
        userRolesStrategies.put(RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE, new UserRolesRetrieveStrategy.LoadRolesByMember());
        userRolesStrategies.put(RoleMapperConfig.GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE, new UserRolesRetrieveStrategy.GetRolesFromUserMemberOfAttribute());
        userRolesStrategies.put(RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY, new UserRolesRetrieveStrategy.LoadRolesByMemberRecursively());

        for (MembershipType membershipType : MembershipType.values()) {
            MEMBERSHIP_TYPES.add(membershipType.toString());
        }

        for (LDAPGroupMapperMode mode : LDAPGroupMapperMode.values()) {
            MODES.add(mode.toString());
        }
        NO_IMPORT_MODES.add(LDAPGroupMapperMode.LDAP_ONLY.toString());
        NO_IMPORT_MODES.add(LDAPGroupMapperMode.READ_ONLY.toString());

        List<ProviderConfigProperty> config = getProps(null);
        configProperties = config;
    }

    private static List<ProviderConfigProperty> getProps(ComponentModel parent) {
        String roleObjectClasses = LDAPConstants.GROUP_OF_NAMES;
        String mode = LDAPGroupMapperMode.LDAP_ONLY.toString();
        String membershipUserAttribute = LDAPConstants.UID;
        boolean importEnabled = true;
        boolean isActiveDirectory = false;
        if (parent != null) {
            LDAPConfig config = new LDAPConfig(parent.getConfig());
            roleObjectClasses = config.isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
            mode = config.getEditMode() == UserStorageProvider.EditMode.WRITABLE ? LDAPGroupMapperMode.LDAP_ONLY.toString() : LDAPGroupMapperMode.READ_ONLY.toString();
            membershipUserAttribute = config.getUsernameLdapAttribute();
            importEnabled = new UserStorageProviderModel(parent).isImportEnabled();
            isActiveDirectory = config.isActiveDirectory();
        }

        ProviderConfigurationBuilder config = ProviderConfigurationBuilder.create()
                .property().name(RoleMapperConfig.ROLES_DN)
                .label("LDAP Roles DN")
                .helpText("LDAP DN where are roles of this tree saved. For example 'ou=finance,dc=example,dc=org' ")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property().name(RoleMapperConfig.ROLE_NAME_LDAP_ATTRIBUTE)
                .label("Role Name LDAP Attribute")
                .helpText("Name of LDAP attribute, which is used in role objects for name and RDN of role. Usually it will be 'cn' . In this case typical group/role object may have DN like 'cn=role1,ou=finance,dc=example,dc=org' ")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(LDAPConstants.CN)
                .add()
                .property().name(RoleMapperConfig.ROLE_OBJECT_CLASSES)
                .label("Role Object Classes")
                .helpText("Object class (or classes) of the role object. It's divided by comma if more classes needed. In typical LDAP deployment it could be 'groupOfNames' . In Active Directory it's usually 'group' ")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(roleObjectClasses)
                .add()
                .property().name(RoleMapperConfig.MEMBERSHIP_LDAP_ATTRIBUTE)
                .label("Membership LDAP Attribute")
                .helpText("Name of LDAP attribute on role, which is used for membership mappings. Usually it will be 'member' ." +
                        "However when 'Membership Attribute Type' is 'UID' then 'Membership LDAP Attribute' could be typically 'memberUid' .")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(LDAPConstants.MEMBER)
                .add()
                .property().name(RoleMapperConfig.MEMBERSHIP_ATTRIBUTE_TYPE)
                .label("Membership Attribute Type")
                .helpText("DN means that LDAP role has it's members declared in form of their full DN. For example 'member: uid=john,ou=users,dc=example,dc=com' . " +
                        "UID means that LDAP role has it's members declared in form of pure user uids. For example 'memberUid: john' .")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(MEMBERSHIP_TYPES)
                .defaultValue(MembershipType.DN.toString())
                .add()
                .property().name(RoleMapperConfig.MEMBERSHIP_USER_LDAP_ATTRIBUTE)
                .label("Membership User LDAP Attribute")
                .helpText("Used just if Membership Attribute Type is UID. It is name of LDAP attribute on user, which is used for membership mappings. Usually it will be 'uid' . For example if value of " +
                        "'Membership User LDAP Attribute' is 'uid' and " +
                        " LDAP group has  'memberUid: john', then it is expected that particular LDAP user will have attribute 'uid: john' .")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(membershipUserAttribute)
                .add()
                .property().name(RoleMapperConfig.ROLES_LDAP_FILTER)
                .label("LDAP Filter")
                .helpText("LDAP Filter adds additional custom filter to the whole query for retrieve LDAP roles. Leave this empty if no additional filtering is needed and you want to retrieve all roles from LDAP. Otherwise make sure that filter starts with '(' and ends with ')'")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add();

        if (importEnabled) {
            config.property().name(RoleMapperConfig.MODE)
                    .label("Mode")
                    .helpText("LDAP_ONLY means that all role mappings are retrieved from LDAP and saved into LDAP. READ_ONLY is Read-only LDAP mode where role mappings are " +
                            "retrieved from both LDAP and DB and merged together. New role grants are not saved to LDAP but to DB. IMPORT is Read-only LDAP mode where role mappings are retrieved from LDAP just at the time when user is imported from LDAP and then " +
                            "they are saved to local keycloak DB.")
                    .type(ProviderConfigProperty.LIST_TYPE)
                    .options(MODES)
                    .defaultValue(mode)
                    .add();
        } else {
            config.property().name(RoleMapperConfig.MODE)
                    .label("Mode")
                    .helpText("LDAP_ONLY means that specified role mappings are writable to LDAP. READ_ONLY means LDAP is readonly.")
                    .type(ProviderConfigProperty.LIST_TYPE)
                    .options(NO_IMPORT_MODES)
                    .defaultValue(mode)
                    .add();

        }

        List<String> roleRetrievers = new LinkedList<>(userRolesStrategies.keySet());
        String roleRetrieveHelpText = "Specify how to retrieve roles of user. LOAD_ROLES_BY_MEMBER_ATTRIBUTE means that roles of user will be retrieved by sending LDAP query to retrieve all roles where 'member' is our user. " +
                "GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE means that roles of user will be retrieved from 'memberOf' attribute of our user. Or from the other attribute specified by 'Member-Of LDAP Attribute' . ";
        if (isActiveDirectory) {
            roleRetrieveHelpText = roleRetrieveHelpText + "LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY is applicable just in Active Directory and it means that roles of user will be retrieved recursively with usage of LDAP_MATCHING_RULE_IN_CHAIN Ldap extension.";
        } else {
            // Option should be available just for the Active Directory
            roleRetrievers.remove(RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY);
        }

        config.property().name(RoleMapperConfig.USER_ROLES_RETRIEVE_STRATEGY)
                .label("User Roles Retrieve Strategy")
                .helpText(roleRetrieveHelpText)
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(roleRetrievers)
                .defaultValue(RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE)
                .add()
                .property().name(GroupMapperConfig.MEMBEROF_LDAP_ATTRIBUTE)
                .label("Member-Of LDAP Attribute")
                .helpText("Used just when 'User Roles Retrieve Strategy' is GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE . " +
                        "It specifies the name of the LDAP attribute on the LDAP user, which contains the roles (LDAP Groups), which the user is member of. " +
                        "Usually it will be 'memberOf' and that's also the default value.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(LDAPConstants.MEMBER_OF)
                .add()
                .property().name(RoleMapperConfig.USE_REALM_ROLES_MAPPING)
                .label("Use Realm Roles Mapping")
                .helpText("If true, then LDAP role mappings will be mapped to realm role mappings in Keycloak. Otherwise it will be mapped to client role mappings")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue("true")
                .add()
                .property().name(RoleMapperConfig.CLIENT_ID)
                .label("Client ID")
                .helpText("Client ID of client to which LDAP role mappings will be mapped. Applicable just if 'Use Realm Roles Mapping' is false")
                .type(ProviderConfigProperty.CLIENT_LIST_TYPE)
                .add();
        return config.build();
    }

    @Override
    public void onParentUpdate(RealmModel realm, UserStorageProviderModel oldParent, UserStorageProviderModel newParent, ComponentModel mapperModel) {
        if (!newParent.isImportEnabled()) {
            if (new RoleMapperConfig(mapperModel).getMode() == LDAPGroupMapperMode.IMPORT) {
                 mapperModel.getConfig().putSingle(RoleMapperConfig.MODE, LDAPGroupMapperMode.READ_ONLY.toString());
                 realm.updateComponent(mapperModel);

            }
        }
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        ComponentModel parentModel = realm.getComponent(model.getParentId());
        UserStorageProviderModel parent = new UserStorageProviderModel(parentModel);
        onParentUpdate(realm, parent, parent, model);

    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        ComponentModel parentModel = realm.getComponent(newModel.getParentId());
        UserStorageProviderModel parent = new UserStorageProviderModel(parentModel);
        onParentUpdate(realm, parent, parent, newModel);
    }

    @Override
    public String getHelpText() {
        return "Used to map role mappings of roles from some LDAP DN to Keycloak role mappings of either realm roles or client roles of particular client";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(RealmModel realm, ComponentModel parent) {
        return getProps(parent);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Map<String, Object> getTypeMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fedToKeycloakSyncSupported", true);
        metadata.put("fedToKeycloakSyncMessage", "sync-ldap-roles-to-keycloak");
        metadata.put("keycloakToFedSyncSupported", true);
        metadata.put("keycloakToFedSyncMessage", "sync-keycloak-roles-to-ldap");

        return metadata;
    }


    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        checkMandatoryConfigAttribute(RoleMapperConfig.ROLES_DN, "LDAP Roles DN", config);
        checkMandatoryConfigAttribute(RoleMapperConfig.MODE, "Mode", config);

        String realmMappings = config.getConfig().getFirst(RoleMapperConfig.USE_REALM_ROLES_MAPPING);
        boolean useRealmMappings = Boolean.parseBoolean(realmMappings);
        if (!useRealmMappings) {
            String clientId = config.getConfig().getFirst(RoleMapperConfig.CLIENT_ID);
            if (clientId == null || clientId.trim().isEmpty()) {
                throw new ComponentValidationException("ldapErrorMissingClientId");
            }
        }

        LDAPUtils.validateCustomLdapFilter(config.getConfig().getFirst(RoleMapperConfig.ROLES_LDAP_FILTER));
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new RoleLDAPStorageMapper(mapperModel, federationProvider, this);
    }

    protected UserRolesRetrieveStrategy getUserRolesRetrieveStrategy(String strategyKey) {
        return userRolesStrategies.get(strategyKey);
    }
}

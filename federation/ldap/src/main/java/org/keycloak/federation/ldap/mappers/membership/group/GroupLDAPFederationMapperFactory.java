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

package org.keycloak.federation.ldap.mappers.membership.group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.mappers.AbstractLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.AbstractLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.membership.CommonLDAPGroupMapperConfig;
import org.keycloak.federation.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.federation.ldap.mappers.membership.MembershipType;
import org.keycloak.federation.ldap.mappers.membership.UserRolesRetrieveStrategy;
import org.keycloak.federation.ldap.mappers.membership.role.RoleMapperConfig;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.UserFederationMapperSyncConfigRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID = "group-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    protected static final Map<String, UserRolesRetrieveStrategy> userGroupsStrategies = new LinkedHashMap<>();

    // TODO: Merge with RoleLDAPFederationMapperFactory as there are lot of similar properties
    static {
        userGroupsStrategies.put(GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE, new UserRolesRetrieveStrategy.LoadRolesByMember());
        userGroupsStrategies.put(GroupMapperConfig.GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE, new UserRolesRetrieveStrategy.GetRolesFromUserMemberOfAttribute());
        userGroupsStrategies.put(GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY, new UserRolesRetrieveStrategy.LoadRolesByMemberRecursively());

        ProviderConfigProperty groupsDn = createConfigProperty(GroupMapperConfig.GROUPS_DN, "LDAP Groups DN",
                "LDAP DN where are groups of this tree saved. For example 'ou=groups,dc=example,dc=org' ", ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(groupsDn);

        ProviderConfigProperty groupNameLDAPAttribute = createConfigProperty(GroupMapperConfig.GROUP_NAME_LDAP_ATTRIBUTE, "Group Name LDAP Attribute",
                "Name of LDAP attribute, which is used in group objects for name and RDN of group. Usually it will be 'cn' . In this case typical group/role object may have DN like 'cn=Group1,ou=groups,dc=example,dc=org' ",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(groupNameLDAPAttribute);

        ProviderConfigProperty groupObjectClasses = createConfigProperty(GroupMapperConfig.GROUP_OBJECT_CLASSES, "Group Object Classes",
                "Object class (or classes) of the group object. It's divided by comma if more classes needed. In typical LDAP deployment it could be 'groupOfNames' . In Active Directory it's usually 'group' ",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(groupObjectClasses);

        ProviderConfigProperty preserveGroupInheritance = createConfigProperty(GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "Preserve Group Inheritance",
                "Flag whether group inheritance from LDAP should be propagated to Keycloak. If false, then all LDAP groups will be mapped as flat top-level groups in Keycloak. Otherwise group inheritance is " +
                        "preserved into Keycloak, but the group sync might fail if LDAP structure contains recursions or multiple parent groups per child groups",
                ProviderConfigProperty.BOOLEAN_TYPE, null);
        configProperties.add(preserveGroupInheritance);

        ProviderConfigProperty membershipLDAPAttribute = createConfigProperty(GroupMapperConfig.MEMBERSHIP_LDAP_ATTRIBUTE, "Membership LDAP Attribute",
                "Name of LDAP attribute on group, which is used for membership mappings. Usually it will be 'member' ",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(membershipLDAPAttribute);

        List<String> membershipTypes = new LinkedList<>();
        for (MembershipType membershipType : MembershipType.values()) {
            membershipTypes.add(membershipType.toString());
        }
        ProviderConfigProperty membershipType = createConfigProperty(GroupMapperConfig.MEMBERSHIP_ATTRIBUTE_TYPE, "Membership Attribute Type",
                "DN means that LDAP group has it's members declared in form of their full DN. For example 'member: uid=john,ou=users,dc=example,dc=com' . " +
                        "UID means that LDAP group has it's members declared in form of pure user uids. For example 'memberUid: john' .",
                ProviderConfigProperty.LIST_TYPE, membershipTypes);
        configProperties.add(membershipType);

        ProviderConfigProperty ldapFilter = createConfigProperty(GroupMapperConfig.GROUPS_LDAP_FILTER,
                "LDAP Filter",
                "LDAP Filter adds additional custom filter to the whole query for retrieve LDAP groups. Leave this empty if no additional filtering is needed and you want to retrieve all groups from LDAP. Otherwise make sure that filter starts with '(' and ends with ')'",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(ldapFilter);

        List<String> modes = new LinkedList<>();
        for (LDAPGroupMapperMode mode : LDAPGroupMapperMode.values()) {
            modes.add(mode.toString());
        }
        ProviderConfigProperty mode = createConfigProperty(GroupMapperConfig.MODE, "Mode",
                "LDAP_ONLY means that all group mappings of users are retrieved from LDAP and saved into LDAP. READ_ONLY is Read-only LDAP mode where group mappings are " +
                        "retrieved from both LDAP and DB and merged together. New group joins are not saved to LDAP but to DB. IMPORT is Read-only LDAP mode where group mappings are " +
                        "retrieved from LDAP just at the time when user is imported from LDAP and then " +
                        "they are saved to local keycloak DB.",
                ProviderConfigProperty.LIST_TYPE, modes);
        configProperties.add(mode);

        List<String> roleRetrievers = new LinkedList<>(userGroupsStrategies.keySet());
        ProviderConfigProperty retriever = createConfigProperty(GroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY, "User Groups Retrieve Strategy",
                "Specify how to retrieve groups of user. LOAD_GROUPS_BY_MEMBER_ATTRIBUTE means that roles of user will be retrieved by sending LDAP query to retrieve all groups where 'member' is our user. " +
                        "GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE means that groups of user will be retrieved from 'memberOf' attribute of our user. " +
                        "LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY is applicable just in Active Directory and it means that groups of user will be retrieved recursively with usage of LDAP_MATCHING_RULE_IN_CHAIN Ldap extension."
                ,
                ProviderConfigProperty.LIST_TYPE, roleRetrievers);
        configProperties.add(retriever);

        ProviderConfigProperty mappedGroupAttributes = createConfigProperty(GroupMapperConfig.MAPPED_GROUP_ATTRIBUTES, "Mapped Group Attributes",
                "List of names of attributes divided by comma. This points to the list of attributes on LDAP group, which will be mapped as attributes of Group in Keycloak. " +
                "Leave this empty if no additional group attributes are required to be mapped in Keycloak. ",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(mappedGroupAttributes);

        ProviderConfigProperty dropNonExistingGroupsDuringSync = createConfigProperty(GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC, "Drop non-existing groups during sync",
                "If this flag is true, then during sync of groups from LDAP to Keycloak, we will keep just those Keycloak groups, which still exists in LDAP. Rest will be deleted",
                ProviderConfigProperty.BOOLEAN_TYPE, null);
        configProperties.add(dropNonExistingGroupsDuringSync);
    }

    @Override
    public String getHelpText() {
        return "Used to map group mappings of groups from some LDAP DN to Keycloak group mappings";
    }

    @Override
    public String getDisplayCategory() {
        return GROUP_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Group mappings";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        Map<String, String> defaultValues = new HashMap<>();
        LDAPConfig config = new LDAPConfig(providerModel.getConfig());

        defaultValues.put(GroupMapperConfig.GROUP_NAME_LDAP_ATTRIBUTE, LDAPConstants.CN);

        String roleObjectClasses = config.isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
        defaultValues.put(GroupMapperConfig.GROUP_OBJECT_CLASSES, roleObjectClasses);

        defaultValues.put(GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, "true");
        defaultValues.put(GroupMapperConfig.MEMBERSHIP_LDAP_ATTRIBUTE, LDAPConstants.MEMBER);
        defaultValues.put(GroupMapperConfig.MEMBERSHIP_ATTRIBUTE_TYPE, MembershipType.DN.toString());

        String mode = config.getEditMode() == UserFederationProvider.EditMode.WRITABLE ? LDAPGroupMapperMode.LDAP_ONLY.toString() : LDAPGroupMapperMode.READ_ONLY.toString();
        defaultValues.put(GroupMapperConfig.MODE, mode);
        defaultValues.put(RoleMapperConfig.USER_ROLES_RETRIEVE_STRATEGY, GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE);

        defaultValues.put(GroupMapperConfig.DROP_NON_EXISTING_GROUPS_DURING_SYNC, "false");

        return defaultValues;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public UserFederationMapperSyncConfigRepresentation getSyncConfig() {
        return new UserFederationMapperSyncConfigRepresentation(true, "sync-ldap-groups-to-keycloak", true, "sync-keycloak-groups-to-ldap");
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationProviderModel fedProviderModel, UserFederationMapperModel mapperModel) throws FederationConfigValidationException {
        checkMandatoryConfigAttribute(GroupMapperConfig.GROUPS_DN, "LDAP Groups DN", mapperModel);
        checkMandatoryConfigAttribute(GroupMapperConfig.MODE, "Mode", mapperModel);

        String mt = mapperModel.getConfig().get(CommonLDAPGroupMapperConfig.MEMBERSHIP_ATTRIBUTE_TYPE);
        MembershipType membershipType = mt==null ? MembershipType.DN : Enum.valueOf(MembershipType.class, mt);
        boolean preserveGroupInheritance = Boolean.parseBoolean(mapperModel.getConfig().get(GroupMapperConfig.PRESERVE_GROUP_INHERITANCE));
        if (preserveGroupInheritance && membershipType != MembershipType.DN) {
            throw new FederationConfigValidationException("ldapErrorCantPreserveGroupInheritanceWithUIDMembershipType");
        }

        LDAPUtils.validateCustomLdapFilter(mapperModel.getConfig().get(GroupMapperConfig.GROUPS_LDAP_FILTER));
    }

    @Override
    protected AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm) {
        return new GroupLDAPFederationMapper(mapperModel, federationProvider, realm, this);
    }

    protected UserRolesRetrieveStrategy getUserGroupsRetrieveStrategy(String strategyKey) {
        return userGroupsStrategies.get(strategyKey);
    }
}

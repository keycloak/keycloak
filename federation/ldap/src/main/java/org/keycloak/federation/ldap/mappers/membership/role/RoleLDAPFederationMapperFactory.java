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

package org.keycloak.federation.ldap.mappers.membership.role;

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
import org.keycloak.federation.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.federation.ldap.mappers.membership.MembershipType;
import org.keycloak.federation.ldap.mappers.membership.UserRolesRetrieveStrategy;
import org.keycloak.federation.ldap.mappers.membership.group.GroupMapperConfig;
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
public class RoleLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    public static final String PROVIDER_ID = "role-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    protected static final Map<String, UserRolesRetrieveStrategy> userRolesStrategies = new LinkedHashMap<>();


    static {
        userRolesStrategies.put(RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE, new UserRolesRetrieveStrategy.LoadRolesByMember());
        userRolesStrategies.put(RoleMapperConfig.GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE, new UserRolesRetrieveStrategy.GetRolesFromUserMemberOfAttribute());
        userRolesStrategies.put(RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY, new UserRolesRetrieveStrategy.LoadRolesByMemberRecursively());

        ProviderConfigProperty rolesDn = createConfigProperty(RoleMapperConfig.ROLES_DN, "LDAP Roles DN",
                "LDAP DN where are roles of this tree saved. For example 'ou=finance,dc=example,dc=org' ", ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(rolesDn);

        ProviderConfigProperty roleNameLDAPAttribute = createConfigProperty(RoleMapperConfig.ROLE_NAME_LDAP_ATTRIBUTE, "Role Name LDAP Attribute",
                "Name of LDAP attribute, which is used in role objects for name and RDN of role. Usually it will be 'cn' . In this case typical group/role object may have DN like 'cn=role1,ou=finance,dc=example,dc=org' ",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(roleNameLDAPAttribute);

        ProviderConfigProperty roleObjectClasses = createConfigProperty(RoleMapperConfig.ROLE_OBJECT_CLASSES, "Role Object Classes",
                "Object class (or classes) of the role object. It's divided by comma if more classes needed. In typical LDAP deployment it could be 'groupOfNames' . In Active Directory it's usually 'group' ",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(roleObjectClasses);

        ProviderConfigProperty membershipLDAPAttribute = createConfigProperty(RoleMapperConfig.MEMBERSHIP_LDAP_ATTRIBUTE, "Membership LDAP Attribute",
                "Name of LDAP attribute on role, which is used for membership mappings. Usually it will be 'member' ",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(membershipLDAPAttribute);


        List<String> membershipTypes = new LinkedList<>();
        for (MembershipType membershipType : MembershipType.values()) {
            membershipTypes.add(membershipType.toString());
        }
        ProviderConfigProperty membershipType = createConfigProperty(RoleMapperConfig.MEMBERSHIP_ATTRIBUTE_TYPE, "Membership Attribute Type",
                "DN means that LDAP role has it's members declared in form of their full DN. For example 'member: uid=john,ou=users,dc=example,dc=com' . " +
                        "UID means that LDAP role has it's members declared in form of pure user uids. For example 'memberUid: john' .",
                ProviderConfigProperty.LIST_TYPE, membershipTypes);
        configProperties.add(membershipType);


        ProviderConfigProperty ldapFilter = createConfigProperty(RoleMapperConfig.ROLES_LDAP_FILTER,
                "LDAP Filter",
                "LDAP Filter adds additional custom filter to the whole query for retrieve LDAP roles. Leave this empty if no additional filtering is needed and you want to retrieve all roles from LDAP. Otherwise make sure that filter starts with '(' and ends with ')'",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(ldapFilter);


        List<String> modes = new LinkedList<>();
        for (LDAPGroupMapperMode mode : LDAPGroupMapperMode.values()) {
            modes.add(mode.toString());
        }
        ProviderConfigProperty mode = createConfigProperty(RoleMapperConfig.MODE, "Mode",
                "LDAP_ONLY means that all role mappings are retrieved from LDAP and saved into LDAP. READ_ONLY is Read-only LDAP mode where role mappings are " +
                        "retrieved from both LDAP and DB and merged together. New role grants are not saved to LDAP but to DB. IMPORT is Read-only LDAP mode where role mappings are retrieved from LDAP just at the time when user is imported from LDAP and then " +
                        "they are saved to local keycloak DB.",
                ProviderConfigProperty.LIST_TYPE, modes);
        configProperties.add(mode);


        List<String> roleRetrievers = new LinkedList<>(userRolesStrategies.keySet());
        ProviderConfigProperty retriever = createConfigProperty(RoleMapperConfig.USER_ROLES_RETRIEVE_STRATEGY, "User Roles Retrieve Strategy",
                "Specify how to retrieve roles of user. LOAD_ROLES_BY_MEMBER_ATTRIBUTE means that roles of user will be retrieved by sending LDAP query to retrieve all roles where 'member' is our user. " +
                        "GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE means that roles of user will be retrieved from 'memberOf' attribute of our user. " +
                        "LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY is applicable just in Active Directory and it means that roles of user will be retrieved recursively with usage of LDAP_MATCHING_RULE_IN_CHAIN Ldap extension."
                ,
                ProviderConfigProperty.LIST_TYPE, roleRetrievers);
        configProperties.add(retriever);


        ProviderConfigProperty useRealmRolesMappings = createConfigProperty(RoleMapperConfig.USE_REALM_ROLES_MAPPING, "Use Realm Roles Mapping",
                "If true, then LDAP role mappings will be mapped to realm role mappings in Keycloak. Otherwise it will be mapped to client role mappings", ProviderConfigProperty.BOOLEAN_TYPE, null);
        configProperties.add(useRealmRolesMappings);

        ProviderConfigProperty clientIdProperty = createConfigProperty(RoleMapperConfig.CLIENT_ID, "Client ID",
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
    public Map<String, String> getDefaultConfig(UserFederationProviderModel providerModel) {
        Map<String, String> defaultValues = new HashMap<>();
        LDAPConfig config = new LDAPConfig(providerModel.getConfig());

        defaultValues.put(RoleMapperConfig.ROLE_NAME_LDAP_ATTRIBUTE, LDAPConstants.CN);

        String roleObjectClasses = config.isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
        defaultValues.put(RoleMapperConfig.ROLE_OBJECT_CLASSES, roleObjectClasses);

        defaultValues.put(RoleMapperConfig.MEMBERSHIP_LDAP_ATTRIBUTE, LDAPConstants.MEMBER);
        defaultValues.put(RoleMapperConfig.MEMBERSHIP_ATTRIBUTE_TYPE, MembershipType.DN.toString());

        String mode = config.getEditMode() == UserFederationProvider.EditMode.WRITABLE ? LDAPGroupMapperMode.LDAP_ONLY.toString() : LDAPGroupMapperMode.READ_ONLY.toString();
        defaultValues.put(RoleMapperConfig.MODE, mode);

        defaultValues.put(RoleMapperConfig.USER_ROLES_RETRIEVE_STRATEGY, RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE);
        defaultValues.put(RoleMapperConfig.USE_REALM_ROLES_MAPPING, "true");
        return defaultValues;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public UserFederationMapperSyncConfigRepresentation getSyncConfig() {
        return new UserFederationMapperSyncConfigRepresentation(true, "sync-ldap-roles-to-keycloak", true, "sync-keycloak-roles-to-ldap");
    }

    @Override
    public void validateConfig(RealmModel realm, UserFederationProviderModel fedProviderModel, UserFederationMapperModel mapperModel) throws FederationConfigValidationException {
        checkMandatoryConfigAttribute(RoleMapperConfig.ROLES_DN, "LDAP Roles DN", mapperModel);
        checkMandatoryConfigAttribute(RoleMapperConfig.MODE, "Mode", mapperModel);

        String realmMappings = mapperModel.getConfig().get(RoleMapperConfig.USE_REALM_ROLES_MAPPING);
        boolean useRealmMappings = Boolean.parseBoolean(realmMappings);
        if (!useRealmMappings) {
            String clientId = mapperModel.getConfig().get(RoleMapperConfig.CLIENT_ID);
            if (clientId == null || clientId.trim().isEmpty()) {
                throw new FederationConfigValidationException("ldapErrorMissingClientId");
            }
        }

        LDAPUtils.validateCustomLdapFilter(mapperModel.getConfig().get(RoleMapperConfig.ROLES_LDAP_FILTER));
    }

    @Override
    protected AbstractLDAPFederationMapper createMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider federationProvider, RealmModel realm) {
        return new RoleLDAPFederationMapper(mapperModel, federationProvider, realm, this);
    }

    protected UserRolesRetrieveStrategy getUserRolesRetrieveStrategy(String strategyKey) {
        return userRolesStrategies.get(strategyKey);
    }
}

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

package org.keycloak.storage.ldap.mappers.membership.group;

import java.util.Collection;
import java.util.Collections;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapperConfig;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupMapperConfig extends CommonLDAPGroupMapperConfig {

    // LDAP DN where groups of this tree are saved.
    public static final String GROUPS_DN = "groups.dn";
    public static final String GROUPS_RELATIVE_CREATE_DN = "groups.relative.create.dn";

    // Name of LDAP attribute, which is used in group objects for name and RDN of group. Usually it will be "cn"
    public static final String GROUP_NAME_LDAP_ATTRIBUTE = "group.name.ldap.attribute";

    // Object classes of the group object.
    public static final String GROUP_OBJECT_CLASSES = "group.object.classes";

    // Flag whether group inheritance from LDAP should be propagated to Keycloak group inheritance.
    public static final String PRESERVE_GROUP_INHERITANCE = "preserve.group.inheritance";

    // Flag whether missing groups should be ignored.
    public static final String IGNORE_MISSING_GROUPS = "ignore.missing.groups";

    // Customized LDAP filter which is added to the whole LDAP query
    public static final String GROUPS_LDAP_FILTER = "groups.ldap.filter";

    // Name of attributes of the LDAP group object, which will be mapped as attributes of Group in Keycloak
    public static final String MAPPED_GROUP_ATTRIBUTES = "mapped.group.attributes";

    // During sync of groups from LDAP to Keycloak, we will keep just those Keycloak groups, which still exists in LDAP. Rest will be deleted
    public static final String DROP_NON_EXISTING_GROUPS_DURING_SYNC = "drop.non.existing.groups.during.sync";

    // See UserRolesRetrieveStrategy
    public static final String LOAD_GROUPS_BY_MEMBER_ATTRIBUTE = "LOAD_GROUPS_BY_MEMBER_ATTRIBUTE";
    public static final String GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE = "GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE";
    public static final String LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY = "LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY";

    // Keycloak group path the LDAP groups are added to (default: top level "/")
    public static final String LDAP_GROUPS_PATH = "groups.path";
    public static final String DEFAULT_LDAP_GROUPS_PATH = "/";

    public GroupMapperConfig(ComponentModel mapperModel) {
        super(mapperModel);
    }


    public String getGroupsDn() {
        String groupsDn = mapperModel.getConfig().getFirst(GROUPS_DN);
        if (groupsDn == null) {
            throw new ModelException("Groups DN is null! Check your configuration");
        }
        return groupsDn;
    }

    public String getRelativeCreateDn() {
        String relativeCreateDn = mapperModel.getConfig().getFirst(GROUPS_RELATIVE_CREATE_DN);
        if(relativeCreateDn != null) {
            relativeCreateDn = relativeCreateDn.trim();
            return relativeCreateDn.endsWith(",") ? relativeCreateDn : relativeCreateDn + ",";
        }
        return "";
    }

    @Override
    public String getLDAPGroupsDn() {
        return getGroupsDn();
    }

    public String getGroupNameLdapAttribute() {
        String rolesRdnAttr = mapperModel.getConfig().getFirst(GROUP_NAME_LDAP_ATTRIBUTE);
        return rolesRdnAttr!=null ? rolesRdnAttr : LDAPConstants.CN;
    }

    @Override
    public String getLDAPGroupNameLdapAttribute() {
        return getGroupNameLdapAttribute();
    }

    public boolean isPreserveGroupsInheritance() {
        return AbstractLDAPStorageMapper.parseBooleanParameter(mapperModel, PRESERVE_GROUP_INHERITANCE);
    }

    public boolean isIgnoreMissingGroups() {
        return AbstractLDAPStorageMapper.parseBooleanParameter(mapperModel, IGNORE_MISSING_GROUPS);
    }

    public Collection<String> getGroupObjectClasses(LDAPStorageProvider ldapProvider) {
        String objectClasses = mapperModel.getConfig().getFirst(GROUP_OBJECT_CLASSES);
        if (objectClasses == null) {
            // For Active directory, the default is 'group' . For other servers 'groupOfNames'
            objectClasses = ldapProvider.getLdapIdentityStore().getConfig().isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
        }

        return getConfigValues(objectClasses);
    }

    public Collection<String> getGroupAttributes() {
        String groupAttrs = mapperModel.getConfig().getFirst(MAPPED_GROUP_ATTRIBUTES);
        return (groupAttrs == null) ? Collections.<String>emptySet() : getConfigValues(groupAttrs);
    }

    public String getCustomLdapFilter() {
        return mapperModel.getConfig().getFirst(GROUPS_LDAP_FILTER);
    }

    public boolean isDropNonExistingGroupsDuringSync() {
        return AbstractLDAPStorageMapper.parseBooleanParameter(mapperModel, DROP_NON_EXISTING_GROUPS_DURING_SYNC);
    }

    public String getUserGroupsRetrieveStrategy() {
        String strategyString = mapperModel.getConfig().getFirst(USER_ROLES_RETRIEVE_STRATEGY);
        return strategyString!=null ? strategyString : LOAD_GROUPS_BY_MEMBER_ATTRIBUTE;
    }

    public String getGroupsPath() {
        String groupsPath = mapperModel.getConfig().getFirst(LDAP_GROUPS_PATH);
        return ObjectUtil.isBlank(groupsPath) ? DEFAULT_LDAP_GROUPS_PATH : groupsPath.trim();
    }

    public String getGroupsPathWithTrailingSlash() {
        String path = getGroupsPath();
        while (!path.endsWith("/")) {
            path = getGroupsPath() + "/";
        }
        return path;
    }

    public boolean isTopLevelGroupsPath() {
        return "/".equals(getGroupsPath());
    }
}

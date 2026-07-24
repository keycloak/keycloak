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

import java.util.Collection;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapperConfig;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleMapperConfig extends CommonLDAPGroupMapperConfig {

    // LDAP DN where are roles of this tree saved.
    public static final String ROLES_DN = "roles.dn";
    public static final String ROLES_RELATIVE_CREATE_DN = "roles.relative.create.dn";

    // Name of LDAP attribute, which is used in role objects for name and RDN of role. Usually it will be "cn"
    public static final String ROLE_NAME_LDAP_ATTRIBUTE = "role.name.ldap.attribute";

    // Object classes of the role object.
    public static final String ROLE_OBJECT_CLASSES = "role.object.classes";

    // Boolean option. If true, we will map LDAP roles to realm roles. If false, we will map to client roles (client specified by option CLIENT_ID)
    public static final String USE_REALM_ROLES_MAPPING = "use.realm.roles.mapping";

    // ClientId, which we want to map roles. Applicable just if "USE_REALM_ROLES_MAPPING" is false
    public static final String CLIENT_ID = "client.id";

    // Customized LDAP filter which is added to the whole LDAP query
    public static final String ROLES_LDAP_FILTER = "roles.ldap.filter";

    // See UserRolesRetrieveStrategy
    public static final String LOAD_ROLES_BY_MEMBER_ATTRIBUTE = "LOAD_ROLES_BY_MEMBER_ATTRIBUTE";
    public static final String GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE = "GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE";
    public static final String LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY = "LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY";


    public RoleMapperConfig(ComponentModel mapperModel) {
        super(mapperModel);
    }

    public String getRolesDn() {
        String rolesDn = mapperModel.getConfig().getFirst(ROLES_DN);
        if (rolesDn == null) {
            throw new ModelException("Roles DN is null! Check your configuration");
        }
        return rolesDn;
    }

    public String getRelativeCreateDn() {
        String relativeCreateDn = mapperModel.getConfig().getFirst(ROLES_RELATIVE_CREATE_DN);
        if(relativeCreateDn != null) {
            relativeCreateDn = relativeCreateDn.trim();
            return relativeCreateDn.endsWith(",") ? relativeCreateDn : relativeCreateDn + ",";
        }
        return "";
    }

    @Override
    public String getLDAPGroupsDn() {
        return getRolesDn();
    }

    public String getRoleNameLdapAttribute() {
        String rolesRdnAttr = mapperModel.getConfig().getFirst(ROLE_NAME_LDAP_ATTRIBUTE);
        return rolesRdnAttr!=null ? rolesRdnAttr : LDAPConstants.CN;
    }

    @Override
    public String getLDAPGroupNameLdapAttribute() {
        return getRoleNameLdapAttribute();
    }

    public Collection<String> getRoleObjectClasses(LDAPStorageProvider ldapProvider) {
        String objectClasses = mapperModel.getConfig().getFirst(ROLE_OBJECT_CLASSES);
        if (objectClasses == null) {
            // For Active directory, the default is 'group' . For other servers 'groupOfNames'
            objectClasses = ldapProvider.getLdapIdentityStore().getConfig().isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
        }

        return getConfigValues(objectClasses);
    }

    public String getCustomLdapFilter() {
        return mapperModel.getConfig().getFirst(ROLES_LDAP_FILTER);
    }

    public boolean isRealmRolesMapping() {
        String realmRolesMapping = mapperModel.getConfig().getFirst(USE_REALM_ROLES_MAPPING);
        return realmRolesMapping==null || Boolean.parseBoolean(realmRolesMapping);
    }

    public String getClientId() {
        return mapperModel.getConfig().getFirst(CLIENT_ID);
    }


    public String getUserRolesRetrieveStrategy() {
        String strategyString = mapperModel.getConfig().getFirst(USER_ROLES_RETRIEVE_STRATEGY);
        return strategyString!=null ? strategyString : LOAD_ROLES_BY_MEMBER_ATTRIBUTE;
    }

}

/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.ldap.role.config;

import org.keycloak.Config;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.map.storage.ldap.config.LdapMapCommonGroupMapperConfig;
import org.keycloak.models.map.storage.ldap.config.LdapMapConfig;
import org.keycloak.models.map.storage.ldap.model.LdapMapDn;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

public class LdapMapRoleMapperConfig extends LdapMapCommonGroupMapperConfig {

    private final Config.Scope config;
    private final LdapMapConfig ldapMapConfig;

    public LdapMapRoleMapperConfig(Config.Scope config) {
        super(new ComponentModel() {
            @Override
            public MultivaluedHashMap<String, String> getConfig() {
                return new MultivaluedHashMap<String, String>() {
                    @Override
                    public String getFirst(String key) {
                        return config.get(key);
                    }
                };
            }
        });
        this.config = config;
        this.ldapMapConfig = new LdapMapConfig(config);
    }

    public String getRealmRolesDn() {
        String rolesDn = config.get(REALM_ROLES_DN);
        if (rolesDn == null) {
            throw new ModelException("Roles DN is null! Check your configuration");
        }
        return rolesDn;
    }

    public String getCommonRolesDn() {
        String rolesDn = config.get(COMMON_ROLES_DN);
        if (rolesDn == null) {
            throw new ModelException("Roles DN is null! Check your configuration");
        }
        return rolesDn;
    }

    public String getClientRolesDn() {
        String rolesDn = config.get(CLIENT_ROLES_DN);
        if (rolesDn == null) {
            throw new ModelException("Roles DN is null! Check your configuration");
        }
        return rolesDn;
    }

    public String getRolesDn(Boolean isClientRole, String clientId) {
        String rolesDn;
        if (isClientRole == null && clientId == null) {
            rolesDn = mapperModel.getConfig().getFirst(COMMON_ROLES_DN);
        } else {
            if (isClientRole != null && !isClientRole) {
                rolesDn = config.get(REALM_ROLES_DN);
            } else {
                rolesDn = config.get(CLIENT_ROLES_DN);
                if (rolesDn != null) {
                    LdapMapDn dn = LdapMapDn.fromString(rolesDn);
                    LdapMapDn.RDN firstRdn = dn.getFirstRdn();
                    for (String key : firstRdn.getAllKeys()) {
                        firstRdn.setAttrValue(key, firstRdn.getAttrValue(key).replaceAll("\\{0}", Matcher.quoteReplacement(clientId)));
                    }
                    rolesDn = dn.toString();
                }
            }
        }
        if (rolesDn == null) {
            throw new ModelException("Roles DN is null! Check your configuration");
        }
        return rolesDn;
    }

    public Set<String> getRoleAttributes() {
        String roleAttributes = mapperModel.getConfig().getFirst("role.attributes");
        if (roleAttributes == null) {
            roleAttributes = "";
        }
        return new HashSet<>(Arrays.asList(roleAttributes.trim().split("\\s+")));
    }

    // LDAP DN where are realm roles of this tree saved.
    public static final String REALM_ROLES_DN = "roles.realm.dn";

    // LDAP DN where are client roles of this tree saved.
    public static final String CLIENT_ROLES_DN = "roles.client.dn";

    // LDAP DN to find both client and realm roles.
    public static final String COMMON_ROLES_DN = "roles.common.dn";

    // Name of LDAP attribute, which is used in role objects for name and RDN of role. Usually it will be "cn"
    public static final String ROLE_NAME_LDAP_ATTRIBUTE = "role.name.ldap.attribute";

    // Object classes of the role object.
    public static final String ROLE_OBJECT_CLASSES = "role.object.classes";

    // Customized LDAP filter which is added to the whole LDAP query
    public static final String ROLES_LDAP_FILTER = "roles.ldap.filter";

    // See UserRolesRetrieveStrategy
    public static final String LOAD_ROLES_BY_MEMBER_ATTRIBUTE = "LOAD_ROLES_BY_MEMBER_ATTRIBUTE";
    public static final String GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE = "GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE";
    public static final String LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY = "LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY";

    public String getRoleNameLdapAttribute() {
        String rolesRdnAttr = mapperModel.getConfig().getFirst(ROLE_NAME_LDAP_ATTRIBUTE);
        return rolesRdnAttr!=null ? rolesRdnAttr : LDAPConstants.CN;
    }

    @Override
    public String getLDAPGroupNameLdapAttribute() {
        return getRoleNameLdapAttribute();
    }

    public String getCustomLdapFilter() {
        return mapperModel.getConfig().getFirst(ROLES_LDAP_FILTER);
    }

    public String getUserRolesRetrieveStrategy() {
        String strategyString = mapperModel.getConfig().getFirst(USER_ROLES_RETRIEVE_STRATEGY);
        return strategyString!=null ? strategyString : LOAD_ROLES_BY_MEMBER_ATTRIBUTE;
    }

    public LdapMapConfig getLdapMapConfig() {
        return ldapMapConfig;
    }
}

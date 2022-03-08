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

package org.keycloak.models.map.storage.ldap.user.config;

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

public class LdapMapUserMapperConfig extends LdapMapCommonGroupMapperConfig {

    private final Config.Scope config;
    private final LdapMapConfig ldapMapConfig;

    public LdapMapUserMapperConfig(Config.Scope config) {
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

    public String getRealmUsersDn() {
        String usersDn = config.get(REALM_USERS_DN);
        if (usersDn == null) {
            throw new ModelException("Users DN is null! Check your configuration");
        }
        return usersDn;
    }

    public String getCommonUsersDn() {
        String usersDn = config.get(COMMON_USERS_DN);
        if (usersDn == null) {
            throw new ModelException("Users DN is null! Check your configuration");
        }
        return usersDn;
    }

    public String getClientUsersDn() {
        String usersDn = config.get(CLIENT_USERS_DN);
        if (usersDn == null) {
            throw new ModelException("Users DN is null! Check your configuration");
        }
        return usersDn;
    }

    public Set<String> getUserAttributes() {
        String userAttributes = mapperModel.getConfig().getFirst("user.attributes");
        if (userAttributes == null) {
            userAttributes = "";
        }
        return new HashSet<>(Arrays.asList(userAttributes.trim().split("\\s+")));
    }

    // LDAP DN where are realm users of this tree saved.
    public static final String REALM_USERS_DN = "users.realm.dn";

    // LDAP DN where are client users of this tree saved.
    public static final String CLIENT_USERS_DN = "users.client.dn";

    // LDAP DN to find both client and realm users.
    public static final String COMMON_USERS_DN = "users.common.dn";

    // Customized LDAP filter which is added to the whole LDAP query
    public static final String USERS_LDAP_FILTER = "users.ldap.filter";

    // See UserUsersRetrieveStrategy
    public static final String LOAD_USERS_BY_MEMBER_ATTRIBUTE = "LOAD_USERS_BY_MEMBER_ATTRIBUTE";
    public static final String GET_USERS_FROM_USER_MEMBEROF_ATTRIBUTE = "GET_USERS_FROM_USER_MEMBEROF_ATTRIBUTE";
    public static final String LOAD_USERS_BY_MEMBER_ATTRIBUTE_RECURSIVELY = "LOAD_USERS_BY_MEMBER_ATTRIBUTE_RECURSIVELY";

    public String getUserNameLdapAttribute() {
        return getLdapMapConfig().getUsernameLdapAttribute();
    }

    @Override
    public String getLDAPGroupNameLdapAttribute() {
        return getUserNameLdapAttribute();
    }

    public String getCustomLdapFilter() {
        return mapperModel.getConfig().getFirst(USERS_LDAP_FILTER);
    }

    public LdapMapConfig getLdapMapConfig() {
        return ldapMapConfig;
    }
}

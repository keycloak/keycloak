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

package org.keycloak.models.map.storage.ldap.config;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class LdapMapCommonGroupMapperConfig {

    // Name of LDAP attribute on role, which is used for membership mappings. Usually it will be "member"
    public static final String MEMBERSHIP_LDAP_ATTRIBUTE = "membership.ldap.attribute";

    // See docs for MembershipType enum
    public static final String MEMBERSHIP_ATTRIBUTE_TYPE = "membership.attribute.type";

    // Used just for membershipType=UID. Name of LDAP attribute on user, which is used for membership mappings. Usually it will be "uid"
    public static final String MEMBERSHIP_USER_LDAP_ATTRIBUTE = "membership.user.ldap.attribute";

    // See docs for Mode enum
    public static final String MODE = "mode";

    // See docs for UserRolesRetrieveStrategy enum
    public static final String USER_ROLES_RETRIEVE_STRATEGY = "user.roles.retrieve.strategy";

    // Used just for UserRolesRetrieveStrategy.GetRolesFromUserMemberOfAttribute. It's the name of the attribute on LDAP user, which is used to track the groups which user is member.
    // Usually it will "memberof"
    public static final String MEMBEROF_LDAP_ATTRIBUTE = "memberof.ldap.attribute";


    protected final ComponentModel mapperModel;

    public LdapMapCommonGroupMapperConfig(ComponentModel mapperModel) {
        this.mapperModel = mapperModel;
    }

    public String getMembershipLdapAttribute() {
        String membershipAttrName = mapperModel.getConfig().getFirst(MEMBERSHIP_LDAP_ATTRIBUTE);
        return membershipAttrName!=null ? membershipAttrName : LDAPConstants.MEMBER;
    }

    public String getMembershipUserLdapAttribute(LdapMapConfig ldapMapConfig) {
        String membershipUserAttrName = mapperModel.getConfig().getFirst(MEMBERSHIP_USER_LDAP_ATTRIBUTE);
        return membershipUserAttrName!=null ? membershipUserAttrName : ldapMapConfig.getUsernameLdapAttribute();
    }

    public String getMemberOfLdapAttribute() {
        String memberOfLdapAttrName = mapperModel.getConfig().getFirst(MEMBEROF_LDAP_ATTRIBUTE);
        return memberOfLdapAttrName!=null ? memberOfLdapAttrName : LDAPConstants.MEMBER_OF;
    }

    protected Set<String> getConfigValues(String str) {
        String[] objClasses = str.split(",");
        Set<String> trimmed = new HashSet<>();
        for (String objectClass : objClasses) {
            objectClass = objectClass.trim();
            if (objectClass.length() > 0) {
                trimmed.add(objectClass);
            }
        }
        return trimmed;
    }

    public abstract String getLDAPGroupNameLdapAttribute();


}

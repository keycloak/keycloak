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

package org.keycloak.storage.ldap.mappers.membership;


import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQueryConditionsBuilder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Strategy for how to retrieve LDAP roles of user
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserRolesRetrieveStrategy {


    List<LDAPObject> getLDAPRoleMappings(CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapUser, LDAPConfig ldapConfig);

    void beforeUserLDAPQuery(CommonLDAPGroupMapper roleOrGroupMapper, LDAPQuery query);


    // Impl subclasses

    /**
     * Roles of user will be retrieved by sending LDAP query to retrieve all roles where "member" is our user
     */
    class LoadRolesByMember implements UserRolesRetrieveStrategy {

        @Override
        public List<LDAPObject> getLDAPRoleMappings(CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapUser, LDAPConfig ldapConfig) {
            try (LDAPQuery ldapQuery = roleOrGroupMapper.createLDAPGroupQuery()) {
                String membershipAttr = roleOrGroupMapper.getConfig().getMembershipLdapAttribute();

                String membershipUserAttrName = roleOrGroupMapper.getConfig().getMembershipUserLdapAttribute(ldapConfig);
                String userMembership = LDAPUtils.getMemberValueOfChildObject(ldapUser, roleOrGroupMapper.getConfig().getMembershipTypeLdapAttribute(), membershipUserAttrName);

                Condition membershipCondition = getMembershipCondition(membershipAttr, userMembership);
                ldapQuery.addWhereCondition(membershipCondition);
                return ldapQuery.getResultList();
            }
        }

        @Override
        public void beforeUserLDAPQuery(CommonLDAPGroupMapper roleOrGroupMapper, LDAPQuery query) {
        }

        protected Condition getMembershipCondition(String membershipAttr, String userMembership) {
            return new LDAPQueryConditionsBuilder().equal(membershipAttr, userMembership);
        }

    };

    /**
     * Roles of user will be retrieved from "memberOf" attribute of our user
     */
    class GetRolesFromUserMemberOfAttribute implements UserRolesRetrieveStrategy {

        @Override
        public List<LDAPObject> getLDAPRoleMappings(CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapUser, LDAPConfig ldapConfig) {
            String memberOfLdapAttrName = roleOrGroupMapper.getConfig().getMemberOfLdapAttribute();

            Set<String> memberOfValues = ldapUser.getAttributeAsSet(memberOfLdapAttrName);
            if (memberOfValues == null) {
                return Collections.emptyList();
            }

            List<LDAPObject> roles = new LinkedList<>();
            LDAPDn parentDn = LDAPDn.fromString(roleOrGroupMapper.getConfig().getLDAPGroupsDn());

            for (String roleDn : memberOfValues) {
                LDAPDn roleDN = LDAPDn.fromString(roleDn);
                if (roleDN.isDescendantOf(parentDn)) {
                    LDAPObject role = new LDAPObject();
                    role.setDn(roleDN);

                    LDAPDn.RDN firstRDN = roleDN.getFirstRdn();
                    String attrKey = roleOrGroupMapper.getConfig().getLDAPGroupNameLdapAttribute();
                    String attrVal = firstRDN.getAttrValue(attrKey);
                    if (attrVal != null) {
                        role.setRdnAttributeName(attrKey);
                        role.setSingleAttribute(attrKey, attrVal);
                        roles.add(role);
                    }
                }
            }
            return roles;
        }

        @Override
        public void beforeUserLDAPQuery(CommonLDAPGroupMapper roleOrGroupMapper, LDAPQuery query) {
            String memberOfLdapAttrName = roleOrGroupMapper.getConfig().getMemberOfLdapAttribute();

            query.addReturningLdapAttribute(memberOfLdapAttrName);
            query.addReturningReadOnlyLdapAttribute(memberOfLdapAttrName);
        }

    };

    /**
     * Extension specific to Active Directory. Roles of user will be retrieved by sending LDAP query to retrieve all roles where "member" is our user.
     * The query will be able to retrieve memberships recursively with usage of AD specific extension LDAP_MATCHING_RULE_IN_CHAIN, so likely doesn't work on other LDAP servers
     */
    class LoadRolesByMemberRecursively extends LoadRolesByMember {

        protected Condition getMembershipCondition(String membershipAttr, String userMembership) {
            return new LDAPQueryConditionsBuilder().equal(membershipAttr + LDAPConstants.LDAP_MATCHING_RULE_IN_CHAIN, userMembership);
        }

    };

}

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


import java.util.List;
import java.util.Set;

import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.utils.StreamsUtil;

/**
 * Strategy for how to retrieve LDAP roles of user
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserRolesRetrieveStrategy {


    List<LDAPObject> getLDAPRoleMappings(CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapUser, LDAPConfig ldapConfig);

    List<UserModel> getLDAPRoleMembers(RealmModel realm, CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapRoleOrGroup, int firstResult, int maxResults);

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

                return LDAPUtils.loadAllLDAPObjects(ldapQuery, ldapConfig);
            }
        }

        @Override
        public List<UserModel> getLDAPRoleMembers(RealmModel realm, CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapRoleOrGroup, int firstResult, int maxResults) {
            MembershipType membershipType = roleOrGroupMapper.getConfig().getMembershipTypeLdapAttribute();
            return membershipType.getGroupMembers(realm, roleOrGroupMapper, ldapRoleOrGroup, firstResult, maxResults);
        }

        @Override
        public void beforeUserLDAPQuery(CommonLDAPGroupMapper roleOrGroupMapper, LDAPQuery query) {
        }

        protected Condition getMembershipCondition(String membershipAttr, String userMembership) {
            return new LDAPQueryConditionsBuilder().equal(membershipAttr, userMembership);
        }

    };

    /**
     * Roles of user will be loaded from LDAP based on "memberOf" attribute of our user
     */
    class GetRolesFromUserMemberOfAttribute implements UserRolesRetrieveStrategy {

        @Override
        public List<LDAPObject> getLDAPRoleMappings(CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapUser, LDAPConfig ldapConfig) {
            try (LDAPQuery ldapQuery = roleOrGroupMapper.createLDAPGroupQuery()) {
                CommonLDAPGroupMapperConfig config = roleOrGroupMapper.getConfig();
                String rdnAttr = config.getLDAPGroupNameLdapAttribute();
                LDAPQueryConditionsBuilder conditionBuilder = new LDAPQueryConditionsBuilder();
                Set<String> memberOfValues = ldapUser.getAttributeAsSetOrDefault(config.getMemberOfLdapAttribute(), Set.of());
                // load only those groups/roles the user is memberOf
                // we do this by query to apply defined custom filters
                // and make sure the values of memberOf have the role/group base DN as its parent
                Condition[] conditions = memberOfValues.stream()
                        .map(LDAPDn::fromString)
                        .filter(roleDN -> roleDN.isDescendantOf(LDAPDn.fromString(config.getLDAPGroupsDn())))
                        .map(roleDN -> conditionBuilder.equal(rdnAttr, roleDN.getFirstRdn().getAttrValue(rdnAttr)))
                        .toArray(Condition[]::new);

                if (conditions.length == 0) {
                    // no roles/groups to fetch based on the pre-filters applied to the memberOf values
                    return List.of();
                }

                ldapQuery.addWhereCondition(conditionBuilder.orCondition(conditions));

                return LDAPUtils.loadAllLDAPObjects(ldapQuery, ldapConfig);
            }
        }

        @Override
        public List<UserModel> getLDAPRoleMembers(RealmModel realm, CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapRoleOrGroup, int firstResult, int maxResults) {
            String memberOfLdapAttrName = roleOrGroupMapper.getConfig().getMemberOfLdapAttribute();
            String roleOrGroupDn = ldapRoleOrGroup.getDn().toString();
            return StreamsUtil.paginatedStream(
                    roleOrGroupMapper.getLdapProvider().searchForUserByUserAttributeStream(realm, memberOfLdapAttrName, roleOrGroupDn), firstResult, maxResults)
                    .toList();
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

        @Override
        protected Condition getMembershipCondition(String membershipAttr, String userMembership) {
            return new LDAPQueryConditionsBuilder().equal(membershipAttr + LDAPConstants.LDAP_MATCHING_RULE_IN_CHAIN, userMembership);
        }

    };

}

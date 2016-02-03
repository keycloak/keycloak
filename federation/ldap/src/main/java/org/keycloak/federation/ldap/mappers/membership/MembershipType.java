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

package org.keycloak.federation.ldap.mappers.membership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.federation.ldap.mappers.membership.group.GroupLDAPFederationMapper;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum MembershipType {

    /**
     * Used if LDAP role has it's members declared in form of their full DN. For example ( "member: uid=john,ou=users,dc=example,dc=com" )
     */
    DN {

        @Override
        public Set<LDAPDn> getLDAPSubgroups(GroupLDAPFederationMapper groupMapper, LDAPObject ldapGroup) {
            CommonLDAPGroupMapperConfig config = groupMapper.getConfig();
            return getLDAPMembersWithParent(ldapGroup, config.getMembershipLdapAttribute(), LDAPDn.fromString(config.getLDAPGroupsDn()));
        }

        // Get just those members of specified group, which are descendants of "requiredParentDn"
        protected Set<LDAPDn> getLDAPMembersWithParent(LDAPObject ldapGroup, String membershipLdapAttribute, LDAPDn requiredParentDn) {
            Set<String> allMemberships = LDAPUtils.getExistingMemberships(membershipLdapAttribute, ldapGroup);

            // Filter and keep just groups
            Set<LDAPDn> result = new HashSet<>();
            for (String membership : allMemberships) {
                LDAPDn childDn = LDAPDn.fromString(membership);
                if (childDn.isDescendantOf(requiredParentDn)) {
                    result.add(childDn);
                }
            }
            return result;
        }

        @Override
        public List<UserModel> getGroupMembers(GroupLDAPFederationMapper groupMapper, LDAPObject ldapGroup, int firstResult, int maxResults) {
            RealmModel realm = groupMapper.getRealm();
            LDAPFederationProvider ldapProvider = groupMapper.getLdapProvider();
            CommonLDAPGroupMapperConfig config = groupMapper.getConfig();

            LDAPDn usersDn = LDAPDn.fromString(ldapProvider.getLdapIdentityStore().getConfig().getUsersDn());
            Set<LDAPDn> userDns = getLDAPMembersWithParent(ldapGroup, config.getMembershipLdapAttribute(), usersDn);

            if (userDns == null) {
                return Collections.emptyList();
            }

            if (userDns.size() <= firstResult) {
                return Collections.emptyList();
            }

            List<LDAPDn> dns = new ArrayList<>(userDns);
            int max = Math.min(dns.size(), firstResult + maxResults);
            dns = dns.subList(firstResult, max);

            // If usernameAttrName is same like DN, we can just retrieve usernames from DNs
            List<String> usernames = new LinkedList<>();
            LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();
            if (ldapConfig.getUsernameLdapAttribute().equals(ldapConfig.getRdnLdapAttribute())) {
                for (LDAPDn userDn : dns) {
                    String username = userDn.getFirstRdnAttrValue();
                    usernames.add(username);
                }
            } else {
                LDAPQuery query = LDAPUtils.createQueryForUserSearch(ldapProvider, realm);
                LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
                Condition[] orSubconditions = new Condition[dns.size()];
                int index = 0;
                for (LDAPDn userDn : dns) {
                    Condition condition = conditionsBuilder.equal(userDn.getFirstRdnAttrName(), userDn.getFirstRdnAttrValue());
                    orSubconditions[index] = condition;
                    index++;
                }
                Condition orCondition = conditionsBuilder.orCondition(orSubconditions);
                query.addWhereCondition(orCondition);
                List<LDAPObject> ldapUsers = query.getResultList();
                for (LDAPObject ldapUser : ldapUsers) {
                    String username = LDAPUtils.getUsername(ldapUser, ldapConfig);
                    usernames.add(username);
                }
            }

            // We have dns of users, who are members of our group. Load them now
            return ldapProvider.loadUsersByUsernames(usernames, realm);
        }

    },


    /**
     * Used if LDAP role has it's members declared in form of pure user uids. For example ( "memberUid: john" )
     */
    UID {

        // Group inheritance not supported for this config
        @Override
        public Set<LDAPDn> getLDAPSubgroups(GroupLDAPFederationMapper groupMapper, LDAPObject ldapGroup) {
            return Collections.emptySet();
        }

        @Override
        public List<UserModel> getGroupMembers(GroupLDAPFederationMapper groupMapper, LDAPObject ldapGroup, int firstResult, int maxResults) {
            String memberAttrName = groupMapper.getConfig().getMembershipLdapAttribute();
            Set<String> memberUids = LDAPUtils.getExistingMemberships(memberAttrName, ldapGroup);

            if (memberUids == null || memberUids.size() <= firstResult) {
                return Collections.emptyList();
            }

            List<String> uids = new ArrayList<>(memberUids);
            int max = Math.min(memberUids.size(), firstResult + maxResults);
            uids = uids.subList(firstResult, max);

            return groupMapper.getLdapProvider().loadUsersByUsernames(uids, groupMapper.getRealm());
        }

    };

    public abstract Set<LDAPDn> getLDAPSubgroups(GroupLDAPFederationMapper groupMapper, LDAPObject ldapGroup);

    public abstract List<UserModel> getGroupMembers(GroupLDAPFederationMapper groupMapper, LDAPObject ldapGroup, int firstResult, int maxResults);
}

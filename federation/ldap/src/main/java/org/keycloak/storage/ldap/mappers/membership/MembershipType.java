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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum MembershipType {

    /**
     * Used if LDAP role has it's members declared in form of their full DN. For example ( "member: uid=john,ou=users,dc=example,dc=com" )
     */
    DN {

        @Override
        public Set<LDAPDn> getLDAPSubgroups(CommonLDAPGroupMapper groupMapper, LDAPObject ldapGroup) {
            CommonLDAPGroupMapperConfig config = groupMapper.getConfig();
            return getLDAPMembersWithParent(groupMapper.getLdapProvider(), ldapGroup, config.getMembershipLdapAttribute(),
                    LDAPDn.fromString(config.getLDAPGroupsDn()), config.getLDAPGroupNameLdapAttribute());
        }

        // Get just those members of specified group, which are descendants of "requiredParentDn"
        protected Set<LDAPDn> getLDAPMembersWithParent(LDAPStorageProvider ldapProvider, LDAPObject ldapGroup,
                String membershipLdapAttribute, LDAPDn requiredParentDn, String rdnAttr) {
            Set<String> allMemberships = LDAPUtils.getExistingMemberships(ldapProvider, membershipLdapAttribute, ldapGroup);

            // Filter and keep just descendants of requiredParentDn and with the correct RDN
            Set<LDAPDn> result = new LinkedHashSet<>();
            for (String membership : allMemberships) {
                LDAPDn childDn = LDAPDn.fromString(membership);
                if (childDn.isDescendantOf(requiredParentDn) && childDn.getFirstRdn().getAttrValue(rdnAttr) != null) {
                    result.add(childDn);
                }
            }
            return result;
        }

        @Override
        public List<UserModel> getGroupMembers(RealmModel realm, CommonLDAPGroupMapper groupMapper, LDAPObject ldapGroup, int firstResult, int maxResults) {
            LDAPStorageProvider ldapProvider = groupMapper.getLdapProvider();
            CommonLDAPGroupMapperConfig config = groupMapper.getConfig();

            LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();
            LDAPDn usersDn = LDAPDn.fromString(ldapProvider.getLdapIdentityStore().getConfig().getUsersDn());
            Set<LDAPDn> userDns = getLDAPMembersWithParent(ldapProvider, ldapGroup, config.getMembershipLdapAttribute(), usersDn, ldapConfig.getRdnLdapAttribute());

            if (userDns == null || userDns.size() <= firstResult) {
                return Collections.emptyList();
            }

            return ldapProvider.loadUsersByDNs(realm, userDns, firstResult, maxResults)
                    .collect(Collectors.toList());
        }
    },

    /**
     * Used if LDAP role has it's members declared in form of pure user uids. For example ( "memberUid: john" )
     */
    UID {

        // Group inheritance not supported for this config
        @Override
        public Set<LDAPDn> getLDAPSubgroups(CommonLDAPGroupMapper groupMapper, LDAPObject ldapGroup) {
            return Collections.emptySet();
        }

        @Override
        public List<UserModel> getGroupMembers(RealmModel realm, CommonLDAPGroupMapper groupMapper, LDAPObject ldapGroup, int firstResult, int maxResults) {
            LDAPStorageProvider ldapProvider = groupMapper.getLdapProvider();
            LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();

            String memberAttrName = groupMapper.getConfig().getMembershipLdapAttribute();
            Set<String> memberUids = LDAPUtils.getExistingMemberships(ldapProvider, memberAttrName, ldapGroup);

            if (memberUids == null || memberUids.size() <= firstResult) {
                return Collections.emptyList();
            }

            String membershipUserAttrName = groupMapper.getConfig().getMembershipUserLdapAttribute(ldapConfig);

            return ldapProvider.loadUsersByUniqueAttribute(realm, membershipUserAttrName, memberUids, firstResult, maxResults)
                    .collect(Collectors.toList());
        }
    };

    public abstract Set<LDAPDn> getLDAPSubgroups(CommonLDAPGroupMapper groupMapper, LDAPObject ldapGroup);

    public abstract List<UserModel> getGroupMembers(RealmModel realm, CommonLDAPGroupMapper groupMapper, LDAPObject ldapGroup, int firstResult, int maxResults);
}

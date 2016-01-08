package org.keycloak.federation.ldap.mappers.membership;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.models.LDAPConstants;

/**
 * Strategy for how to retrieve LDAP roles of user
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserRolesRetrieveStrategy {


    List<LDAPObject> getLDAPRoleMappings(CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapUser);

    void beforeUserLDAPQuery(LDAPQuery query);


    // Impl subclasses

    /**
     * Roles of user will be retrieved by sending LDAP query to retrieve all roles where "member" is our user
     */
    class LoadRolesByMember implements UserRolesRetrieveStrategy {

        @Override
        public List<LDAPObject> getLDAPRoleMappings(CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapUser) {
            LDAPQuery ldapQuery = roleOrGroupMapper.createLDAPGroupQuery();
            String membershipAttr = roleOrGroupMapper.getConfig().getMembershipLdapAttribute();

            String userMembership = LDAPUtils.getMemberValueOfChildObject(ldapUser, roleOrGroupMapper.getConfig().getMembershipTypeLdapAttribute());

            Condition membershipCondition = getMembershipCondition(membershipAttr, userMembership);
            ldapQuery.addWhereCondition(membershipCondition);
            return ldapQuery.getResultList();
        }

        @Override
        public void beforeUserLDAPQuery(LDAPQuery query) {
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
        public List<LDAPObject> getLDAPRoleMappings(CommonLDAPGroupMapper roleOrGroupMapper, LDAPObject ldapUser) {
            Set<String> memberOfValues = ldapUser.getAttributeAsSet(LDAPConstants.MEMBER_OF);
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

                    String firstDN = roleDN.getFirstRdnAttrName();
                    if (firstDN.equalsIgnoreCase(roleOrGroupMapper.getConfig().getLDAPGroupNameLdapAttribute())) {
                        role.setRdnAttributeName(firstDN);
                        role.setSingleAttribute(firstDN, roleDN.getFirstRdnAttrValue());
                        roles.add(role);
                    }
                }
            }
            return roles;
        }

        @Override
        public void beforeUserLDAPQuery(LDAPQuery query) {
            query.addReturningLdapAttribute(LDAPConstants.MEMBER_OF);
            query.addReturningReadOnlyLdapAttribute(LDAPConstants.MEMBER_OF);
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

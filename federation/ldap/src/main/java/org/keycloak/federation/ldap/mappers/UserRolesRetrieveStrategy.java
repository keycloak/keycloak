package org.keycloak.federation.ldap.mappers;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationMapperModel;

/**
 * Strategy for how to retrieve LDAP roles of user
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum UserRolesRetrieveStrategy {


    /**
     * Roles of user will be retrieved by sending LDAP query to retrieve all roles where "member" is our user
     */
    LOAD_ROLES_BY_MEMBER_ATTRIBUTE {

        @Override
        public List<LDAPObject> getLDAPRoleMappings(RoleLDAPFederationMapper roleMapper, UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser) {
            LDAPQuery ldapQuery = roleMapper.createRoleQuery(mapperModel, ldapProvider);
            String membershipAttr = roleMapper.getMembershipLdapAttribute(mapperModel);

            String userMembership = roleMapper.getMembershipFromUser(ldapUser, roleMapper.getMembershipTypeLdapAttribute(mapperModel));

            Condition membershipCondition = new LDAPQueryConditionsBuilder().equal(membershipAttr, userMembership);
            ldapQuery.addWhereCondition(membershipCondition);
            return ldapQuery.getResultList();
        }

        @Override
        public void beforeUserLDAPQuery(UserFederationMapperModel mapperModel, LDAPQuery query) {
        }

    },


    /**
     * Roles of user will be retrieved from "memberOf" attribute of our user
     */
    GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE {

        @Override
        public List<LDAPObject> getLDAPRoleMappings(RoleLDAPFederationMapper roleMapper, UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser) {
            Set<String> memberOfValues = ldapUser.getAttributeAsSet(LDAPConstants.MEMBER_OF);
            if (memberOfValues == null) {
                return Collections.emptyList();
            }

            List<LDAPObject> roles = new LinkedList<>();
            LDAPDn parentDn = LDAPDn.fromString(roleMapper.getRolesDn(mapperModel));

            for (String roleDn : memberOfValues) {
                LDAPDn roleDN = LDAPDn.fromString(roleDn);
                if (roleDN.isDescendantOf(parentDn)) {
                    LDAPObject role = new LDAPObject();
                    role.setDn(roleDN);

                    String firstDN = roleDN.getFirstRdnAttrName();
                    if (firstDN.equalsIgnoreCase(roleMapper.getRoleNameLdapAttribute(mapperModel))) {
                        role.setRdnAttributeName(firstDN);
                        role.setSingleAttribute(firstDN, roleDN.getFirstRdnAttrValue());
                        roles.add(role);
                    }
                }
            }
            return roles;
        }

        @Override
        public void beforeUserLDAPQuery(UserFederationMapperModel mapperModel, LDAPQuery query) {
            query.addReturningLdapAttribute(LDAPConstants.MEMBER_OF);
            query.addReturningReadOnlyLdapAttribute(LDAPConstants.MEMBER_OF);
        }

    },


    /**
     * Extension specific to Active Directory. Roles of user will be retrieved by sending LDAP query to retrieve all roles where "member" is our user.
     * The query will be able to retrieve memberships recursively
     * (Assume "role1" has member "role2" and role2 has member "johnuser". Then searching for roles of "johnuser" will return both "role1" and "role2" )
     *
     * This is using AD specific extension LDAP_MATCHING_RULE_IN_CHAIN, so likely doesn't work on other LDAP servers
     */
    LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY {

        @Override
        public List<LDAPObject> getLDAPRoleMappings(RoleLDAPFederationMapper roleMapper, UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser) {
            LDAPQuery ldapQuery = roleMapper.createRoleQuery(mapperModel, ldapProvider);
            String membershipAttr = roleMapper.getMembershipLdapAttribute(mapperModel);
            membershipAttr = membershipAttr + LDAPConstants.LDAP_MATCHING_RULE_IN_CHAIN;
            String userMembership = roleMapper.getMembershipFromUser(ldapUser, roleMapper.getMembershipTypeLdapAttribute(mapperModel));

            Condition membershipCondition = new LDAPQueryConditionsBuilder().equal(membershipAttr, userMembership);
            ldapQuery.addWhereCondition(membershipCondition);
            return ldapQuery.getResultList();
        }

        @Override
        public void beforeUserLDAPQuery(UserFederationMapperModel mapperModel, LDAPQuery query) {
        }

    };



    public abstract List<LDAPObject> getLDAPRoleMappings(RoleLDAPFederationMapper roleMapper, UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser);

    public abstract void beforeUserLDAPQuery(UserFederationMapperModel mapperModel, LDAPQuery query);

}

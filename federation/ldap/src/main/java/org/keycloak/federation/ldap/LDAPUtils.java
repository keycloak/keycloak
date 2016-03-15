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

package org.keycloak.federation.ldap;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.mappers.LDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.membership.MembershipType;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserModel;

/**
 * Allow to directly call some operations against LDAPIdentityStore.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPUtils {

    /**
     * @param ldapProvider
     * @param realm
     * @param user
     * @return newly created LDAPObject with all the attributes, uuid and DN properly set
     */
    public static LDAPObject addUserToLDAP(LDAPFederationProvider ldapProvider, RealmModel realm, UserModel user) {
        LDAPObject ldapUser = new LDAPObject();

        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        LDAPConfig ldapConfig = ldapStore.getConfig();
        ldapUser.setRdnAttributeName(ldapConfig.getRdnLdapAttribute());
        ldapUser.setObjectClasses(ldapConfig.getUserObjectClasses());

        Set<UserFederationMapperModel> federationMappers = realm.getUserFederationMappersByFederationProvider(ldapProvider.getModel().getId());
        List<UserFederationMapperModel> sortedMappers = ldapProvider.sortMappersAsc(federationMappers);
        for (UserFederationMapperModel mapperModel : sortedMappers) {
            LDAPFederationMapper ldapMapper = ldapProvider.getMapper(mapperModel);
            ldapMapper.onRegisterUserToLDAP(mapperModel, ldapProvider, ldapUser, user, realm);
        }

        LDAPUtils.computeAndSetDn(ldapConfig, ldapUser);
        ldapStore.add(ldapUser);
        return ldapUser;
    }

    public static LDAPQuery createQueryForUserSearch(LDAPFederationProvider ldapProvider, RealmModel realm) {
        LDAPQuery ldapQuery = new LDAPQuery(ldapProvider);
        LDAPConfig config = ldapProvider.getLdapIdentityStore().getConfig();
        ldapQuery.setSearchScope(config.getSearchScope());
        ldapQuery.setSearchDn(config.getUsersDn());
        ldapQuery.addObjectClasses(config.getUserObjectClasses());

        String customFilter = config.getCustomUserSearchFilter();
        if (customFilter != null) {
            Condition customFilterCondition = new LDAPQueryConditionsBuilder().addCustomLDAPFilter(customFilter);
            ldapQuery.addWhereCondition(customFilterCondition);
        }

        Set<UserFederationMapperModel> mapperModels = realm.getUserFederationMappersByFederationProvider(ldapProvider.getModel().getId());
        ldapQuery.addMappers(mapperModels);

        return ldapQuery;
    }

    // ldapUser has filled attributes, but doesn't have filled dn.
    private static void computeAndSetDn(LDAPConfig config, LDAPObject ldapUser) {
        String rdnLdapAttrName = config.getRdnLdapAttribute();
        String rdnLdapAttrValue = ldapUser.getAttributeAsString(rdnLdapAttrName);
        if (rdnLdapAttrValue == null) {
            throw new ModelException("RDN Attribute [" + rdnLdapAttrName + "] is not filled. Filled attributes: " + ldapUser.getAttributes());
        }

        LDAPDn dn = LDAPDn.fromString(config.getUsersDn());
        dn.addFirst(rdnLdapAttrName, rdnLdapAttrValue);
        ldapUser.setDn(dn);
    }

    public static String getUsername(LDAPObject ldapUser, LDAPConfig config) {
        String usernameAttr = config.getUsernameLdapAttribute();
        String ldapUsername = ldapUser.getAttributeAsString(usernameAttr);

        if (ldapUsername == null) {
            throw new ModelException("User returned from LDAP has null username! Check configuration of your LDAP mappings. Mapped username LDAP attribute: " +
                    config.getUsernameLdapAttribute() + ", user DN: " + ldapUser.getDn() + ", attributes from LDAP: " + ldapUser.getAttributes());
        }

        return ldapUsername;
    }

    public static void checkUuid(LDAPObject ldapUser, LDAPConfig config) {
        if (ldapUser.getUuid() == null) {
            throw new ModelException("User returned from LDAP has null uuid! Check configuration of your LDAP settings. UUID Attribute must be unique among your LDAP records and available on all the LDAP user records. " +
                    "If your LDAP server really doesn't support the notion of UUID, you can use any other attribute, which is supposed to be unique among LDAP users in tree. For example 'uid' or 'entryDN' . " +
                    "Mapped UUID LDAP attribute: " + config.getUuidLDAPAttributeName() + ", user DN: " + ldapUser.getDn());
        }
    }


    // roles & groups

    public static LDAPObject createLDAPGroup(LDAPFederationProvider ldapProvider, String groupName, String groupNameAttribute, Collection<String> objectClasses,
                                             String parentDn, Map<String, Set<String>> additionalAttributes) {
        LDAPObject ldapObject = new LDAPObject();

        ldapObject.setRdnAttributeName(groupNameAttribute);
        ldapObject.setObjectClasses(objectClasses);
        ldapObject.setSingleAttribute(groupNameAttribute, groupName);

        LDAPDn roleDn = LDAPDn.fromString(parentDn);
        roleDn.addFirst(groupNameAttribute, groupName);
        ldapObject.setDn(roleDn);

        for (Map.Entry<String, Set<String>> attrEntry : additionalAttributes.entrySet()) {
            ldapObject.setAttribute(attrEntry.getKey(), attrEntry.getValue());
        }

        ldapProvider.getLdapIdentityStore().add(ldapObject);
        return ldapObject;
    }

    /**
     * Add ldapChild as member of ldapParent and save ldapParent to LDAP.
     *
     * @param ldapProvider
     * @param membershipType how is 'member' attribute saved (full DN or just uid)
     * @param memberAttrName usually 'member'
     * @param ldapParent role or group
     * @param ldapChild usually user (or child group or child role)
     * @param sendLDAPUpdateRequest if true, the method will send LDAP update request too. Otherwise it will skip it
     */
    public static void addMember(LDAPFederationProvider ldapProvider, MembershipType membershipType, String memberAttrName, LDAPObject ldapParent, LDAPObject ldapChild, boolean sendLDAPUpdateRequest) {

        Set<String> memberships = getExistingMemberships(memberAttrName, ldapParent);

        // Remove membership placeholder if present
        if (membershipType == MembershipType.DN) {
            for (String membership : memberships) {
                if (LDAPConstants.EMPTY_MEMBER_ATTRIBUTE_VALUE.equals(membership)) {
                    memberships.remove(membership);
                    break;
                }
            }
        }

        String membership = getMemberValueOfChildObject(ldapChild, membershipType);

        memberships.add(membership);
        ldapParent.setAttribute(memberAttrName, memberships);

        if (sendLDAPUpdateRequest) {
            ldapProvider.getLdapIdentityStore().update(ldapParent);
        }
    }

    /**
     * Remove ldapChild as member of ldapParent and save ldapParent to LDAP.
     *
     * @param ldapProvider
     * @param membershipType how is 'member' attribute saved (full DN or just uid)
     * @param memberAttrName usually 'member'
     * @param ldapParent role or group
     * @param ldapChild usually user (or child group or child role)
     * @param sendLDAPUpdateRequest if true, the method will send LDAP update request too. Otherwise it will skip it
     */
    public static void deleteMember(LDAPFederationProvider ldapProvider, MembershipType membershipType, String memberAttrName, LDAPObject ldapParent, LDAPObject ldapChild, boolean sendLDAPUpdateRequest) {
        Set<String> memberships = getExistingMemberships(memberAttrName, ldapParent);

        String userMembership = getMemberValueOfChildObject(ldapChild, membershipType);

        memberships.remove(userMembership);

        // Some membership placeholder needs to be always here as "member" is mandatory attribute on some LDAP servers. But not on active directory! (Placeholder, which not matches any real object is not allowed here)
        if (memberships.size() == 0 && membershipType== MembershipType.DN && !ldapProvider.getLdapIdentityStore().getConfig().isActiveDirectory()) {
            memberships.add(LDAPConstants.EMPTY_MEMBER_ATTRIBUTE_VALUE);
        }

        ldapParent.setAttribute(memberAttrName, memberships);
        ldapProvider.getLdapIdentityStore().update(ldapParent);
    }

    /**
     * Return all existing memberships (values of attribute 'member' ) from the given ldapRole or ldapGroup
     *
     * @param memberAttrName usually 'member'
     * @param ldapRole
     * @return
     */
    public static Set<String> getExistingMemberships(String memberAttrName, LDAPObject ldapRole) {
        Set<String> memberships = ldapRole.getAttributeAsSet(memberAttrName);
        if (memberships == null) {
            memberships = new HashSet<>();
        }
        return memberships;
    }

    /**
     * Get value to be used as attribute 'member' in some parent ldapObject
     */
    public static String getMemberValueOfChildObject(LDAPObject ldapUser, MembershipType membershipType) {
        return membershipType == MembershipType.DN ? ldapUser.getDn().toString() : ldapUser.getAttributeAsString(ldapUser.getRdnAttributeName());
    }


    /**
     * Load all LDAP objects corresponding to given query. We will load them paginated, so we allow to bypass the limitation of 1000
     * maximum loaded objects in single query in MSAD
     *
     * @param ldapQuery
     * @param ldapProvider
     * @return
     */
    public static List<LDAPObject> loadAllLDAPObjects(LDAPQuery ldapQuery, LDAPFederationProvider ldapProvider) {
        LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();
        boolean pagination = ldapConfig.isPagination();
        if (pagination) {
            // For now reuse globally configured batch size in LDAP provider page
            int pageSize = ldapConfig.getBatchSizeForSync();

            List<LDAPObject> result = new LinkedList<>();
            boolean nextPage = true;

            while (nextPage) {
                ldapQuery.setLimit(pageSize);
                final List<LDAPObject> currentPageGroups = ldapQuery.getResultList();
                result.addAll(currentPageGroups);
                nextPage = ldapQuery.getPaginationContext() != null;
            }

            return result;
        } else {
            // LDAP pagination not available. Do everything in single transaction
            return ldapQuery.getResultList();
        }
    }


    /**
     * Validate configured customFilter matches the requested format
     *
     * @param customFilter
     * @throws FederationConfigValidationException
     */
    public static void validateCustomLdapFilter(String customFilter) throws FederationConfigValidationException {
        if (customFilter != null) {

            customFilter = customFilter.trim();
            if (customFilter.isEmpty()) {
                return;
            }

            if (!customFilter.startsWith("(") || !customFilter.endsWith(")")) {
                throw new FederationConfigValidationException("ldapErrorInvalidCustomFilter");
            }
        }
    }
}

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

package org.keycloak.storage.ldap;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.naming.directory.SearchControls;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.reflection.Property;
import org.keycloak.models.utils.reflection.PropertyCriteria;
import org.keycloak.models.utils.reflection.PropertyQueries;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.storage.ldap.mappers.LDAPMappersComparator;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;

import org.jboss.logging.Logger;

/**
 * Allow to directly call some operations against LDAPIdentityStore.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPUtils {

    private static final Logger log = Logger.getLogger(LDAPUtils.class);

    /**
     * Method to create a user in the LDAP. The user will be created when all
     * mandatory attributes specified by the mappers are set. The method
     * onRegisterUserToLDAP is first called in each mapper to set any default or
     * initial value.
     *
     * @param ldapProvider The ldap provider
     * @param realm The realm of the user
     * @param user The user model
     * @return The LDAPObject created or to be created when mandatory attributes are filled
     */
    public static LDAPObject addUserToLDAP(LDAPStorageProvider ldapProvider, RealmModel realm, UserModel user) {
        return addUserToLDAP(ldapProvider, realm, user, null);
    }

    /**
     * Method that creates a user in the LDAP when all the attributes marked as
     * mandatory by the mappers are set. The method onRegisterUserToLDAP is
     * first called in each mapper to set any default or initial value. When
     * the user is finally created the passed consumerOnCreated parameter is
     * executed (can be null).
     *
     * @param ldapProvider The ldap provider
     * @param realm The realm of the user
     * @param user The user model
     * @param consumerOnCreated The consumer to execute when the user is created
     * @return The LDAPObject created or to be created when mandatory attributes are filled
     */
    public static LDAPObject addUserToLDAP(LDAPStorageProvider ldapProvider, RealmModel realm, UserModel user, Consumer<LDAPObject> consumerOnCreated) {
        LDAPObject ldapUser = new LDAPObject();

        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        LDAPConfig ldapConfig = ldapStore.getConfig();
        ldapUser.setRdnAttributeName(ldapConfig.getRdnLdapAttribute());
        ldapUser.setObjectClasses(ldapConfig.getUserObjectClasses());

        LDAPMappersComparator ldapMappersComparator = new LDAPMappersComparator(ldapConfig);
        Set<String> mandatoryAttrs = realm.getComponentsStream(ldapProvider.getModel().getId(), LDAPStorageMapper.class.getName())
                .sorted(ldapMappersComparator.sortAsc())
                .map(mapperModel -> {
                    LDAPStorageMapper ldapMapper = ldapProvider.getMapperManager().getMapper(mapperModel);
                    ldapMapper.onRegisterUserToLDAP(ldapUser, user, realm);
                    return ldapMapper.mandatoryAttributeNames();
                })
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        mandatoryAttrs.add(ldapConfig.getRdnLdapAttribute());

        String passwordModifiedTimeAttributeName = ldapStore.getPasswordModificationTimeAttributeName();
        String passwordModifiedTime = user.getFirstAttribute(passwordModifiedTimeAttributeName);
        if (passwordModifiedTime != null) {
            ldapUser.setSingleAttribute(passwordModifiedTimeAttributeName, passwordModifiedTime);
        }

        ldapUser.executeOnMandatoryAttributesComplete(mandatoryAttrs, ldapObject -> {
            LDAPUtils.computeAndSetDn(ldapConfig, ldapObject);
            ldapStore.add(ldapObject);
            if (consumerOnCreated != null) {
                consumerOnCreated.accept(ldapObject);
            }
        });
        return ldapUser;
    }

    public static LDAPQuery createQueryForUserSearch(LDAPStorageProvider ldapProvider, RealmModel realm) {
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

        List<ComponentModel> mapperModels = realm
                .getComponentsStream(ldapProvider.getModel().getId(), LDAPStorageMapper.class.getName())
                .collect(Collectors.toList());
        ldapQuery.addMappers(mapperModels);

        String kerberosPrincipalAttr = ldapProvider.getKerberosConfig().getKerberosPrincipalAttribute();
        if (kerberosPrincipalAttr != null) {
            ldapQuery.addReturningLdapAttribute(kerberosPrincipalAttr);
            ldapQuery.addReturningReadOnlyLdapAttribute(kerberosPrincipalAttr);
        }

        if (config.isActiveDirectory()) {
            ldapQuery.addReturningLdapAttribute(LDAPConstants.PWD_LAST_SET);
        } else {
            // https://datatracker.ietf.org/doc/html/draft-behera-ldap-password-policy
            ldapQuery.addReturningLdapAttribute(LDAPConstants.PWD_CHANGED_TIME);
        }

        return ldapQuery;
    }

    // ldapUser has filled attributes, but doesn't have filled dn.
    public static void computeAndSetDn(LDAPConfig config, LDAPObject ldapUser) {
        String rdnLdapAttrName = config.getRdnLdapAttribute();
        String rdnLdapAttrValue = ldapUser.getAttributeAsString(rdnLdapAttrName);
        if (rdnLdapAttrValue == null) {
            throw new ModelException("RDN Attribute [" + rdnLdapAttrName + "] is not filled. Filled attributes: " + ldapUser.getAttributes());
        }

        LDAPDn dn = LDAPDn.fromString(config.getRelativeCreateDn() + config.getUsersDn());
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

        if (config.isImportEnabled()) {
            return Optional.of(ldapUsername).map(String::toLowerCase).orElse(null);
        }

        return  ldapUsername;
    }

    public static void checkUuid(LDAPObject ldapUser, LDAPConfig config) {
        if (ldapUser.getUuid() == null) {
            throw new ModelException("User returned from LDAP has null uuid! Check configuration of your LDAP settings. UUID Attribute must be unique among your LDAP records and available on all the LDAP user records. " +
                    "If your LDAP server really doesn't support the notion of UUID, you can use any other attribute, which is supposed to be unique among LDAP users in tree. For example 'uid' or 'entryDN' . " +
                    "Mapped UUID LDAP attribute: " + config.getUuidLDAPAttributeName() + ", user DN: " + ldapUser.getDn());
        }
    }


    // roles & groups

    public static LDAPObject createLDAPGroup(LDAPStorageProvider ldapProvider, String groupName, String groupNameAttribute, Collection<String> objectClasses,
                                             String parentDn, Map<String, Set<String>> additionalAttributes, String membershipLdapAttribute) {
        LDAPObject ldapObject = new LDAPObject();

        ldapObject.setRdnAttributeName(groupNameAttribute);
        ldapObject.setObjectClasses(objectClasses);
        ldapObject.setSingleAttribute(groupNameAttribute, groupName);

        for (String objectClassValue : objectClasses) {
            // On MSAD with object class "group", empty member must not be added. Specified object classes typically
            // require empty member attribute if no members have joined yet
            if ((objectClassValue.equalsIgnoreCase(LDAPConstants.GROUP_OF_NAMES)
                    || objectClassValue.equalsIgnoreCase(LDAPConstants.GROUP_OF_ENTRIES)
                    || objectClassValue.equalsIgnoreCase(LDAPConstants.GROUP_OF_UNIQUE_NAMES)) &&
                    additionalAttributes.get(membershipLdapAttribute) == null) {
                ldapObject.setSingleAttribute(membershipLdapAttribute, LDAPConstants.EMPTY_MEMBER_ATTRIBUTE_VALUE);
            }
        }

        LDAPDn roleDn = LDAPDn.fromString(parentDn);
        roleDn.addFirst(groupNameAttribute, groupName);
        ldapObject.setDn(roleDn);

        for (Map.Entry<String, Set<String>> attrEntry : additionalAttributes.entrySet()) {
            ldapObject.setAttribute(attrEntry.getKey(), attrEntry.getValue());
        }

        ldapProvider.getLdapIdentityStore().add(ldapObject);
        return ldapObject;
    }

    public static LDAPObject updateLDAPGroup(LDAPStorageProvider ldapProvider, LDAPObject ldapObject) {
        ldapProvider.getLdapIdentityStore().update(ldapObject);
        return ldapObject;
    }

    /**
     * Add ldapChild as member of ldapParent and save ldapParent to LDAP.
     *
     * @param ldapProvider
     * @param membershipType how is 'member' attribute saved (full DN or just uid)
     * @param memberAttrName usually 'member'
     * @param memberChildAttrName used just if membershipType is UID. Usually 'uid'
     * @param ldapParent role or group
     * @param ldapChild usually user (or child group or child role)
     */
    public static void addMember(LDAPStorageProvider ldapProvider, MembershipType membershipType, String memberAttrName, String memberChildAttrName, LDAPObject ldapParent, LDAPObject ldapChild) {
        String membership = getMemberValueOfChildObject(ldapChild, membershipType, memberChildAttrName);
        ldapProvider.getLdapIdentityStore().addMemberToGroup(ldapParent.getDn().getLdapName(), memberAttrName, membership);
    }

    /**
     * Remove ldapChild as member of ldapParent and save ldapParent to LDAP.
     *
     * @param ldapProvider
     * @param membershipType how is 'member' attribute saved (full DN or just uid)
     * @param memberAttrName usually 'member'
     * @param memberChildAttrName used just if membershipType is UID. Usually 'uid'
     * @param ldapParent role or group
     * @param ldapChild usually user (or child group or child role)
     */
    public static void deleteMember(LDAPStorageProvider ldapProvider, MembershipType membershipType, String memberAttrName, String memberChildAttrName, LDAPObject ldapParent, LDAPObject ldapChild) {
        String userMembership = getMemberValueOfChildObject(ldapChild, membershipType, memberChildAttrName);
        ldapProvider.getLdapIdentityStore().removeMemberFromGroup(ldapParent.getDn().getLdapName(), memberAttrName, userMembership);
    }

    /**
     * Return all existing memberships (values of attribute 'member' ) from the given ldapRole or ldapGroup
     *
     * @param ldapProvider The ldap provider
     * @param memberAttrName usually 'member'
     * @param ldapRole
     * @return
     */
    public static Set<String> getExistingMemberships(LDAPStorageProvider ldapProvider, String memberAttrName, LDAPObject ldapRole) {
        LDAPUtils.fillRangedAttribute(ldapProvider, ldapRole, memberAttrName);
        Set<String> memberships = ldapRole.getAttributeAsSet(memberAttrName);
        if (memberships == null) {
            memberships = new HashSet<>();
        }
        return memberships;
    }

    /**
     * Get value to be used as attribute 'member' or 'memberUid' in some parent ldapObject
     */
    public static String getMemberValueOfChildObject(LDAPObject ldapUser, MembershipType membershipType, String memberChildAttrName) {
        if (membershipType == MembershipType.DN) {
            return ldapUser.getDn().toString();
        } else {
            return ldapUser.getAttributeAsString(memberChildAttrName);
        }
    }


    /**
     * Load all LDAP objects corresponding to given query. We will load them paginated, so we allow to bypass the limitation of 1000
     * maximum loaded objects in single query in MSAD
     *
     * @param ldapQuery LDAP query to be used. The caller should close it after calling this method
     * @param ldapProvider
     * @return
     */
    public static List<LDAPObject> loadAllLDAPObjects(LDAPQuery ldapQuery, LDAPStorageProvider ldapProvider) {
        LDAPConfig ldapConfig = ldapProvider.getLdapIdentityStore().getConfig();
        return loadAllLDAPObjects(ldapQuery, ldapConfig);
    }

    /**
     * Load all LDAP objects corresponding to given query. We will load them paginated, so we allow to bypass the limitation of 1000
     * maximum loaded objects in single query in MSAD
     *
     * @param ldapQuery LDAP query to be used. The caller should close it after calling this method
     * @param ldapConfig
     * @return
     */
    public static List<LDAPObject> loadAllLDAPObjects(LDAPQuery ldapQuery, LDAPConfig ldapConfig) {

        if (ldapConfig.isPagination() && ldapConfig.getBatchSizeForSync() > 0) {
            // For now reuse globally configured batch size in LDAP provider page
            int pageSize = ldapConfig.getBatchSizeForSync();

            List<LDAPObject> result = new LinkedList<>();
            boolean nextPage = true;

            while (nextPage) {
                ldapQuery.setLimit(pageSize);
                final List<LDAPObject> currentPageGroups = ldapQuery.getResultList();
                result.addAll(currentPageGroups);
                nextPage = ldapQuery.getPaginationContext().hasNextPage();
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
     * @throws ComponentValidationException
     */
    public static void validateCustomLdapFilter(String customFilter) throws ComponentValidationException {
        if (customFilter != null) {

            customFilter = customFilter.trim();
            if (customFilter.isEmpty()) {
                return;
            }

            if (!customFilter.startsWith("(") || !customFilter.endsWith(")")) {
                throw new ComponentValidationException("ldapErrorInvalidCustomFilter");
            }
        }
    }

    private static LDAPQuery createLdapQueryForRangeAttribute(LDAPStorageProvider ldapProvider, LDAPObject ldapObject, String name) {
        LDAPQuery q = new LDAPQuery(ldapProvider);
        q.setSearchDn(ldapObject.getDn().getLdapName());
        q.setSearchScope(SearchControls.OBJECT_SCOPE);
        q.addReturningLdapAttribute(name + ";range=" + (ldapObject.getCurrentRange(name) + 1) + "-*");
        return q;
    }

    /**
     * Performs iterative searches over an LDAPObject to return an attribute that is ranged.
     * @param ldapProvider The provider to use
     * @param ldapObject The current object with the ranged attribute not complete
     * @param name The attribute name
     */
    public static void fillRangedAttribute(LDAPStorageProvider ldapProvider, LDAPObject ldapObject, String name) {
        LDAPObject newObject = ldapObject;
        while (!newObject.isRangeComplete(name)) {
            try (LDAPQuery q = createLdapQueryForRangeAttribute(ldapProvider, ldapObject, name)) {
                newObject = q.getFirstResult();
                ldapObject.populateRangedAttribute(newObject, name);
            }
        }
    }

    /**
     * Return a map of the user model properties from the getter methods
     * Map key are the attributes names in lower case
     */
    public static Map<String, Property<Object>> getUserModelProperties(){

        Map<String, Property<Object>> userModelProps = PropertyQueries.createQuery(UserModel.class)
                .addCriteria(new PropertyCriteria() {

                    @Override
                    public boolean methodMatches(Method m) {
                        if ((m.getName().startsWith("get") || m.getName().startsWith("is"))
                                && m.getParameterCount() > 0) {
                            return false;
                        }

                        return true;
                    }

                }).getResultList();

        // Convert to be keyed by lower-cased attribute names
        Map<String, Property<Object>> userModelProperties = new HashMap<>();
        for (Map.Entry<String, Property<Object>> entry : userModelProps.entrySet()) {
            userModelProperties.put(entry.getKey().toLowerCase(), entry.getValue());
        }

        return userModelProperties;
    }

    public static String getDefaultKerberosUserPrincipalAttribute(String vendor) {
        if (vendor != null) {
            switch (vendor) {
                case LDAPConstants.VENDOR_RHDS:
                    return KerberosConstants.KERBEROS_PRINCIPAL_LDAP_ATTRIBUTE_KRB_PRINCIPAL_NAME;
                case LDAPConstants.VENDOR_ACTIVE_DIRECTORY:
                    return KerberosConstants.KERBEROS_PRINCIPAL_LDAP_ATTRIBUTE_USER_PRINCIPAL_NAME;
            }
        }

        return KerberosConstants.KERBEROS_PRINCIPAL_LDAP_ATTRIBUTE_KRB5_PRINCIPAL_NAME;
    }

    /**
     * Convert Generalized Time as defined in RFC4517 to the Date
     */
    static Date generalizedTimeToDate(String generalized) {

        String[] parts = generalized.split("[Z+-]");
        String[] timeFraction = parts[0].split("[.,]");
        String time = timeFraction[0];

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.setLenient(false);

        calendar.set(Calendar.YEAR, Integer.parseInt(time.substring(0, 4)));
        calendar.set(Calendar.MONTH, Integer.parseInt(time.substring(4, 6)) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(time.substring(6, 8)));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(8, 10)));
        if (time.length() >= 12) calendar.set(Calendar.MINUTE, Integer.parseInt(time.substring(10, 12)));
        if (time.length() >= 14) calendar.set(Calendar.SECOND, Integer.parseInt(time.substring(12, 14)));

        // fraction
        if (timeFraction.length >= 2) {
            double fraction = Double.parseDouble("0." + timeFraction[1]);
            if (time.length() >= 14) { // fraction of second
                calendar.set(Calendar.MILLISECOND, (int) Math.round(fraction * 1000));
            } else if (time.length() >= 12) { // fraction of minute
                calendar.set(Calendar.SECOND, (int) Math.round(fraction * 60));
            } else { // fraction of hour
                calendar.set(Calendar.MINUTE, (int) Math.round(fraction * 60));
            }
        }

        // timezone
        if (generalized.length() > parts[0].length()) {
            char delimiter = generalized.charAt(parts[0].length());
            if (delimiter == 'Z') {
                calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
            } else {
                calendar.setTimeZone(TimeZone.getTimeZone("GMT" + delimiter + parts[1]));
            }
        }

        return calendar.getTime();
    }
}
